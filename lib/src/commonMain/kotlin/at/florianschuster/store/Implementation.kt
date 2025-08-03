package at.florianschuster.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class StoreImplementation<Environment, Action, State>(
    initialState: State,
    private val environment: Environment,
    effectScope: CoroutineScope,
    initialEffect: Effect<Environment, Action>?,
    private val reducer: Reducer<Environment, Action, State>,
    private val events: StoreEvents?,
) : Store<Environment, Action, State> {

    private val effectHandler = EffectHandler(::dispatch, effectScope, events, environment)
    private val _state = MutableStateFlow(initialState)

    override val state: StateFlow<State> = _state.asStateFlow()

    init {
        events?.emit(StoreEvent.Initialization(initialState, environment, initialEffect != null))
        initialEffect?.let { effectHandler.handle(listOf(it)) }
    }

    override fun dispatch(action: Action) {
        events?.emit(StoreEvent.Dispatch(action))
        val effects = mutableListOf<Effect<Environment, Action>>()
        val reducerContext = reducerContext(
            environment = environment,
            addEffect = { effects += it },
        )
        _state.update { currentState ->
            val newState = with(reducerContext) {
                with(reducer) {
                    reduce(currentState, action)
                }
            }
            events?.emit(StoreEvent.Reduce(currentState, action, newState))
            newState
        }
        effectHandler.handle(effects)
    }

    private fun <Environment, Action> reducerContext(
        environment: Environment,
        addEffect: (Effect<Environment, Action>) -> Unit,
    ) = object : Reducer.Context<Environment, Action> {
        override val environment: Environment = environment
        override fun add(effect: Effect<Environment, Action>) = addEffect(effect)
    }
}

internal class DelegatingStoreImplementation<Environment, Action, State>(
    initialState: State,
    private val environment: Environment,
    effectScope: CoroutineScope,
    initialEffect: Effect<Environment, Action>?,
    private val delegates: List<DelegateStore<Environment, Action, State, *, *, *>>,
    private val events: StoreEvents?,
) : Store<Environment, Action, State> {

    private val stateMutex = Mutex()
    private val effectHandler = EffectHandler(::dispatch, effectScope, events, environment)
    private val _state = MutableStateFlow(initialState)

    override val state: StateFlow<State> = _state.asStateFlow()

    init {
        events?.emit(StoreEvent.Initialization(initialState, environment, initialEffect != null))
        for (delegate in delegates) {
            effectScope.launch {
                delegate.state.collect { delegateState ->
                    stateMutex.withLock {
                        val effects = mutableListOf<Effect<Environment, Action>>()
                        val expandStateContext = expandStateContext(
                            environment = environment,
                            addEffect = { effects += it },
                        )
                        _state.update { currentState ->
                            val newState = with(expandStateContext) {
                                @Suppress("UNCHECKED_CAST")
                                with(delegate as DelegateStore<Environment, Action, State, *, *, Any?>) {
                                    expandState(currentState, delegateState)
                                }
                            }
                            events?.emit(StoreEvent.ExpandState(currentState, delegateState, newState))
                            newState
                        }
                        effectHandler.handle(effects)
                    }
                }
            }
        }
        initialEffect?.let { effectHandler.handle(listOf(it)) }
    }

    override fun dispatch(action: Action) {
        events?.emit(StoreEvent.Dispatch(action))
        for (delegate in delegates) {
            delegate.dispatch(action)
        }
    }

    private fun <Environment, Action> expandStateContext(
        environment: Environment,
        addEffect: (Effect<Environment, Action>) -> Unit,
    ) = object : DelegateStore.ExpandStateContext<Environment, Action> {
        override val environment: Environment = environment
        override fun add(effect: Effect<Environment, Action>) = addEffect(effect)
    }
}

class EffectHandler<Environment, Action>(
    dispatch: (Action) -> Unit,
    private val effectScope: CoroutineScope,
    private val events: StoreEvents?,
    environment: Environment,
) {
    private val executionContext = effectExecutionContext(environment, dispatch)
    private val executionJobList = ExecutionJobList(events)

    fun handle(effects: List<Effect<Environment, Action>>) {
        effectScope.launch {
            for (effect in effects) {
                when (effect) {
                    is EffectCancellation<Environment, Action> -> {
                        executionJobList.cancel(effect.ids)
                    }

                    is EffectExecution<Environment, Action> -> {
                        // only start if not already started with same id
                        val effectId = effect.id
                        if (effectId != null && effectId in executionJobList) continue
                        // launch new effect
                        val newJob = launch { effect.block(executionContext) }
                        events?.emit(StoreEvent.Effect.Launch(effect.id))
                        if (effect.id != null) {
                            executionJobList.add(
                                ExecutionJobList.Item(effectId = effect.id, job = newJob),
                            )
                        }
                    }
                }
            }
        }
    }

    internal class ExecutionJobList(
        private val events: StoreEvents?,
    ) {

        internal class Item(
            val effectId: Any?,
            val job: Job,
        )

        private val mutex = Mutex()
        internal val items = mutableListOf<Item>()

        suspend operator fun contains(id: Any): Boolean =
            mutex.withLock { items.any { it.effectId == id } }

        suspend fun add(job: Item) {
            mutex.withLock { items.add(job) }
        }

        suspend fun cancel(ids: List<Any>) {
            if (ids.isEmpty()) return
            mutex.withLock {
                if (items.isEmpty()) return@withLock
                for (id in ids) {
                    val job = items.firstOrNull { it.effectId == id } ?: continue
                    job.job.cancel()
                    events?.emit(StoreEvent.Effect.Cancel(job.effectId))
                    items.remove(job)
                }
            }
        }
    }

    private fun <Environment, Action> effectExecutionContext(
        environment: Environment,
        dispatch: (Action) -> Unit,
    ) = object : EffectExecution.Context<Environment, Action> {
        override val environment: Environment = environment
        override fun dispatch(action: Action) = dispatch(action)
    }
}
