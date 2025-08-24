package at.florianschuster.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * A store that holds [State] and allows dispatching [Action]s.
 */
interface Store<Environment, Action, State> {
    val state: StateFlow<State>
    fun dispatch(action: Action)
}

/**
 * Returns [Store.state].
 */
operator fun <State> Store<*, *, State>.component1(): StateFlow<State> = state

/**
 * Returns [Store.dispatch].
 */
operator fun <Action> Store<*, Action, *>.component2(): (Action) -> Unit = ::dispatch

/**
 * Creates a [Store].
 *
 * [Action]s are dispatched to the [Reducer] which **synchronously** updates the [State] and
 * may produce [Effect]s.
 *
 * @param initialState The initial [State] of the [Store].
 * @param environment The [Environment] that is available within the contexts of [Reducer]s and [Effect]s.
 * @param effectScope The [CoroutineScope] in which [Effect]s are executed.
 * @param reducer The [Reducer] that handles updating [State] based on dispatched [Action]s.
 * @param events An optional [StoreEvents] that emits [StoreEvent]s. Can be used for logging or testing.
 */
fun <Environment, Action, State> Store(
    initialState: State,
    environment: Environment,
    effectScope: CoroutineScope,
    reducer: Reducer<Environment, Action, State>,
    events: StoreEvents? = null,
): Store<Environment, Action, State> = StoreImplementation(
    effectScope = effectScope,
    environment = environment,
    initialState = initialState,
    reducer = reducer,
    events = events,
)

/**
 * Creates a [Store] that delegates [Action]s and [State] to [delegates].
 *
 * [Action]s are dispatched to all [delegates] which **asynchronously** update the [State] and
 * may produce [Effect]s.
 *
 * @param initialState The initial [State] of the created parent [Store].
 * @param environment The [Environment] that is available within the contexts of [Reducer]s and [Effect]s.
 * @param effectScope The [CoroutineScope] in which [Effect]s are executed.
 * @param delegates A list of [DelegateStore]s that this [Store] delegates [Action]s and [State] to.
 * The order of the list of [delegates] is important, as it determines the order in which they receive
 * dispatched [Action]s.
 * @param events An optional [StoreEvents] that emits [StoreEvent]s. Can be used for logging or testing.
 */
fun <Environment, Action, State> Store(
    initialState: State,
    environment: Environment,
    effectScope: CoroutineScope,
    delegates: List<DelegateStore<Environment, Action, State, *, *, *>>,
    events: StoreEvents? = null,
): Store<Environment, Action, State> = DelegatingStoreImplementation(
    initialState = initialState,
    environment = environment,
    effectScope = effectScope,
    delegates = delegates,
    events = events,
)
