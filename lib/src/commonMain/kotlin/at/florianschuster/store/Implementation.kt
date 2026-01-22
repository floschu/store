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
                        // If effect has an ID, use atomic check-and-add to prevent race conditions
                        if (effectId != null) {
                            val newJob = executionJobList.launchIfNotActive(effectId) {
                                launch { effect.block(executionContext) }
                            }
                            if (newJob != null) {
                                events?.emit(StoreEvent.Effect.Launch(effectId))
                                newJob.invokeOnCompletion { cause ->
                                    if (cause is CancellationException) {
                                        events?.emit(StoreEvent.Effect.Cancel(effectId))
                                    } else {
                                        events?.emit(StoreEvent.Effect.Complete(effectId))
                                    }
                                }
                            }
                            // If newJob is null, effect was already active - skip
                        } else {
                            // No effect ID - just launch without tracking
                            val newJob = launch { effect.block(executionContext) }
                            events?.emit(StoreEvent.Effect.Launch(effectId))
                            newJob.invokeOnCompletion { cause ->
                                if (cause is CancellationException) {
                                    events?.emit(StoreEvent.Effect.Cancel(effectId))
                                } else {
                                    events?.emit(StoreEvent.Effect.Complete(effectId))
                                }
                            }
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

        /**
         * Atomically checks if an effect with the given ID is already active.
         * If not active, launches the job and adds it to tracking.
         * Returns the launched Job if successful, or null if the effect was already active.
         * 
         * This method prevents race conditions by combining the check and add
         * operations within a single mutex lock.
         */
        suspend fun launchIfNotActive(id: Any, createJob: () -> Job): Job? = mutex.withLock {
            // Check if already active while holding lock
            val existingItem = items.firstOrNull { it.effectId == id }
            if (existingItem != null && existingItem.job.isActive) {
                return@withLock null  // Already active, don't create new job
            }
            // Create and add the job while still holding lock
            val job = createJob()
            if (!job.isCompleted) {
                val item = JobItem(effectId = id, job = job)
                items.add(item)
                job.invokeOnCompletion { 
                    effectScope.launch { 
                        mutex.withLock { items.remove(item) } 
                    } 
                }
            }
            job
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
