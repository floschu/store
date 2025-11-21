package at.florianschuster.store

import kotlinx.coroutines.CancellationException
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
    private val reducer: Reducer<Environment, Action, State>,
    private val events: StoreEvents?,
) : Store<Environment, Action, State> {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<State> = _state.asStateFlow()

    private val effectHandler = EffectHandler(
        state = state,
        dispatch = ::dispatch,
        effectScope = effectScope,
        events = events,
        environment = environment
    )


    init {
        events?.emit(
            StoreEvent.Initialization(
                initialState = initialState,
                environment = environment,
                hasInitialEffect = reducer.initialEffect != null
            )
        )
        reducer.initialEffect?.let { effectHandler.handle(listOf(it)) }
    }

    override fun dispatch(action: Action) {
        events?.emit(StoreEvent.Dispatch(action))
        val effects = mutableListOf<Effect<Environment, Action, State>>()
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

    private fun <Environment, Action, State> reducerContext(
        environment: Environment,
        addEffect: (Effect<Environment, Action, State>) -> Unit,
    ) = object : Reducer.Context<Environment, Action, State> {
        override val environment: Environment = environment
        override fun add(effect: Effect<Environment, Action, State>) = addEffect(effect)
    }
}

internal class DelegatingStoreImplementation<Environment, Action, State>(
    initialState: State,
    private val environment: Environment,
    effectScope: CoroutineScope,
    private val delegates: List<DelegateStore<Environment, Action, State, *, *, *>>,
    private val events: StoreEvents?,
) : Store<Environment, Action, State> {

    private val stateMutex = Mutex()
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<State> = _state.asStateFlow()

    private val effectHandler = EffectHandler(
        state = state,
        dispatch = ::dispatch,
        effectScope = effectScope,
        events = events,
        environment = environment
    )


    init {
        events?.emit(StoreEvent.Initialization(initialState, environment, false))
        for (delegate in delegates) {
            effectScope.launch {
                delegate.state.collect { delegateState ->
                    stateMutex.withLock {
                        val effects = mutableListOf<Effect<Environment, Action, State>>()
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
    }

    override fun dispatch(action: Action) {
        events?.emit(StoreEvent.Dispatch(action))
        for (delegate in delegates) {
            delegate.dispatch(action)
        }
    }

    private fun <Environment, Action, State> expandStateContext(
        environment: Environment,
        addEffect: (Effect<Environment, Action, State>) -> Unit,
    ) = object : DelegateStore.ExpandStateContext<Environment, Action, State> {
        override val environment: Environment = environment
        override fun add(effect: Effect<Environment, Action, State>) = addEffect(effect)
    }
}

internal class EffectHandler<Environment, Action, State>(
    state: StateFlow<State>,
    dispatch: (Action) -> Unit,
    private val effectScope: CoroutineScope,
    private val events: StoreEvents?,
    environment: Environment,
) {
    private val executionContext = effectExecutionContext(environment, state, dispatch)
    private val executionJobList = ExecutionJobList(effectScope)

    fun handle(effects: List<Effect<Environment, Action, State>>) {
        effectScope.launch {
            for (effect in effects) {
                when (effect) {
                    is EffectCancellation<Environment, Action, State> -> {
                        executionJobList.cancel(effect.ids)
                    }

                    is EffectExecution<Environment, Action, State> -> {
                        val effectId = effect.id
                        // only launch effect if it is not already launched
                        if (effectId != null && executionJobList.isActive(effectId)) {
                            continue
                        }
                        // launch new effect
                        val newJob = launch { effect.block(executionContext) }
                        events?.emit(StoreEvent.Effect.Launch(effectId))
                        newJob.invokeOnCompletion { cause ->
                            if (cause is CancellationException && effectId != null) {
                                events?.emit(StoreEvent.Effect.Cancel(effectId))
                            } else {
                                events?.emit(StoreEvent.Effect.Complete(effectId))
                            }
                        }
                        // only track job if it has id and has not already completed
                        if (effectId != null && !newJob.isCompleted) {
                            val item = ExecutionJobList.JobItem(effectId = effectId, job = newJob)
                            executionJobList.add(item)
                        }
                    }
                }
            }
        }
    }

    internal class ExecutionJobList(
        private val effectScope: CoroutineScope
    ) {

        internal class JobItem(
            val effectId: Any?,
            val job: Job,
        )

        private val mutex = Mutex()
        internal val items = mutableListOf<JobItem>()

        suspend fun isActive(id: Any): Boolean = mutex.withLock {
            val item = items.firstOrNull { it.effectId == id }
            item != null && item.job.isActive
        }

        suspend fun add(jobItem: JobItem) {
            suspend fun remove() = mutex.withLock { items.remove(jobItem) }
            mutex.withLock {
                items.add(jobItem)
                jobItem.job.invokeOnCompletion { effectScope.launch { remove() } }
            }
        }

        suspend fun cancel(ids: List<Any>) {
            if (ids.isEmpty()) return
            mutex.withLock {
                if (items.isEmpty()) return@withLock
                for (id in ids) {
                    val jobItem = items
                        .firstOrNull { it.effectId == id }
                        ?: continue
                    jobItem.job.cancel()
                    items.remove(jobItem)
                }
            }
        }
    }

    private fun <Environment, Action, State> effectExecutionContext(
        environment: Environment,
        state: StateFlow<State>,
        dispatch: (Action) -> Unit,
    ) = object : EffectExecution.Context<Environment, Action, State> {
        override val environment: Environment = environment
        override val state: StateFlow<State> = state
        override fun dispatch(action: Action) = dispatch(action)
    }
}
