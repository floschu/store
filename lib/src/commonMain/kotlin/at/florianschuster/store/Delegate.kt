package at.florianschuster.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * A [DelegateStore] is input for a delegating [Store].
 */
interface DelegateStore<DelegatingEnvironment, DelegatingAction, DelegatingState, DelegateEnvironment, DelegateAction, DelegateState> {

    /**
     * The context in which [expandState] is executed.
     */
    interface ExpandStateContext<Environment, Action> {
        val environment: Environment
        fun add(effect: Effect<Environment, Action>)
    }

    val state: StateFlow<DelegateState>
    fun dispatch(action: DelegatingAction)

    /**
     * Reduces the [DelegatingState] based on the [DelegateState].
     */
    fun ExpandStateContext<DelegatingEnvironment, DelegatingAction>.expandState(
        state: DelegatingState,
        delegateState: DelegateState
    ): DelegatingState
}

/**
 * Creates a [DelegateStore] that is able to handle delegated [DelegatingAction]s by scoping them to [DelegateAction].
 * It also expands the [DelegatingState] based on the [DelegateState].
 *
 * @param scopeAction A function that scopes a [DelegatingAction] to a [DelegateAction] or null if the delegate
 * cannot handle the [DelegatingAction].
 * @param expandState A function that expands the [DelegatingState] based on the [DelegateState].
 */
fun <DelegatingEnvironment, DelegatingAction, DelegatingState, DelegateEnvironment, DelegateAction, DelegateState>
        Store<DelegateEnvironment, DelegateAction, DelegateState>.delegate(
    scopeAction: (DelegatingAction) -> DelegateAction?,
    expandState: DelegateStore.ExpandStateContext<DelegatingEnvironment, DelegatingAction>.(
        state: DelegatingState,
        delegateState: DelegateState
    ) -> DelegatingState,
): DelegateStore<DelegatingEnvironment, DelegatingAction, DelegatingState,
        DelegateEnvironment, DelegateAction, DelegateState> =
    DelegateStoreImplementation(
        delegate = this,
        localScopeAction = scopeAction,
        localExpandState = expandState,
    )

/**
 * Creates a [DelegateStore] that is able to handle delegated [DelegatingAction]s by scoping them to
 * [DelegateAction]. It also expands the [DelegatingState] based on the [DelegateState].
 *
 * @param initialState The initial [DelegateState] of the created [DelegateStore].
 * @param environment The [DelegateEnvironment] that is available within the contexts of [Reducer]s and [Effect]s.
 * @param effectScope The [CoroutineScope] in which [Effect]s are executed.
 * @param scopeAction A function that scopes a [DelegatingAction] to a [DelegateAction] or null if the delegate
 * cannot handle the [DelegatingAction].
 * @param expandState A function that expands the [DelegatingState] based on the [DelegateState].
 */
fun <DelegatingEnvironment, DelegatingAction, DelegatingState, DelegateEnvironment, DelegateAction, DelegateState>
        Reducer<DelegateEnvironment, DelegateAction, DelegateState>.delegate(
    initialState: DelegateState,
    environment: DelegateEnvironment,
    effectScope: CoroutineScope,
    scopeAction: (DelegatingAction) -> DelegateAction?,
    expandState: DelegateStore.ExpandStateContext<DelegatingEnvironment, DelegatingAction>.(
        state: DelegatingState,
        delegateState: DelegateState
    ) -> DelegatingState,
    events: StoreEvents? = null,
): DelegateStore<DelegatingEnvironment, DelegatingAction, DelegatingState,
        DelegateEnvironment, DelegateAction, DelegateState> = DelegateStoreImplementation(
    delegate = Store(
        initialState = initialState,
        environment = environment,
        effectScope = effectScope,
        reducer = this,
        events = events,
    ),
    localScopeAction = scopeAction,
    localExpandState = expandState,
)

/**
 * Scopes a [DelegateActionWrapper] to a [DelegateAction].
 * This is useful in scenarios when scoping actions in [delegate]:
 *
 * ```kotlin
 * sealed interface DelegatingAction
 * data class Wrapper(val action: DelegateAction) : DelegatingAction
 *
 * /* Store/Reducer */.delegate(
 *     scopeAction = scopeAction(Wrapper::action),
 *     expandState = { ... },
 * )
 * ```
 */
inline fun <reified DelegateActionWrapper, DelegateAction> scopeAction(
    crossinline scope: (DelegateActionWrapper) -> DelegateAction,
): (Any) -> DelegateAction? = { action ->
    (action as? DelegateActionWrapper)?.let(scope)
}

private class DelegateStoreImplementation<DelegatingEnvironment, DelegatingAction, DelegatingState,
        DelegateEnvironment, DelegateAction, DelegateState>(
    private val delegate: Store<DelegateEnvironment, DelegateAction, DelegateState>,
    private val localScopeAction: (DelegatingAction) -> DelegateAction?,
    private val localExpandState: DelegateStore.ExpandStateContext<DelegatingEnvironment, DelegatingAction>.(
        state: DelegatingState,
        delegateState: DelegateState
    ) -> DelegatingState,
) : DelegateStore<DelegatingEnvironment, DelegatingAction, DelegatingState,
        DelegateEnvironment, DelegateAction, DelegateState> {

    override val state: StateFlow<DelegateState> = delegate.state

    override fun dispatch(action: DelegatingAction) {
        val scopedAction = localScopeAction(action) ?: return
        delegate.dispatch(scopedAction)
    }

    override fun DelegateStore.ExpandStateContext<DelegatingEnvironment, DelegatingAction>.expandState(
        state: DelegatingState,
        delegateState: DelegateState,
    ): DelegatingState = localExpandState(state, delegateState)
}
