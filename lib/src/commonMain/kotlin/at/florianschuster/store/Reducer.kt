package at.florianschuster.store

/**
 * A [Reducer] is a function that takes the current [State] and an [Action] and returns a new [State].
 */
interface Reducer<Environment, Action, State> {

    /**
     * The context in which [Reducer.reduce] is executed.
     */
    interface Context<Environment, Action, State> {
        val environment: Environment
        fun add(effect: Effect<Environment, Action, State>)
    }

    /**
     * An optional [Effect] that is executed when the [Reducer] is initialized.
     */
    val initialEffect: Effect<Environment, Action, State>?

    /**
     * Reduces the [previousState] based on the [action] and returns a new [State].
     *
     * This function is called whenever an [Action] is dispatched to the [Store].
     *
     * It is executed in the context of [Context] which has access to [Environment] and
     * allows adding [Effect]s. Added [Effect]s will be executed after the [Reducer] has
     * processed the [Action].
     *
     * @param previousState The previous [State] before [action] was dispatched.
     * @param action The [Action] that was dispatched.
     * @return The new [State] after processing the [action].
     */
    fun Context<Environment, Action, State>.reduce(
        previousState: State,
        action: Action,
    ): State
}

/**
 * Creates a [Reducer].
 */
fun <Environment, Action, State> Reducer(
    initialEffect: Effect<Environment, Action, State>? = null,
    reduce: Reducer.Context<Environment, Action, State>.(
        previousState: State,
        action: Action,
    ) -> State,
): Reducer<Environment, Action, State> = StoreReducer(
    initialEffect = initialEffect,
    reduce = reduce
)

/**
 * Creates an empty [Reducer] that does not change the state.
 */
@Suppress("FunctionName")
fun <Environment, Action, State> EmptyReducer(): Reducer<Environment, Action, State> =
    object : Reducer<Environment, Action, State> {
        override val initialEffect: Effect<Environment, Action, State>? = null
        override fun Reducer.Context<Environment, Action, State>.reduce(
            previousState: State,
            action: Action,
        ): State = previousState
    }

internal class StoreReducer<Environment, Action, State>(
    override val initialEffect: Effect<Environment, Action, State>?,
    private val reduce: Reducer.Context<Environment, Action, State>.(
        previousState: State,
        action: Action,
    ) -> State,
) : Reducer<Environment, Action, State> {
    override fun Reducer.Context<Environment, Action, State>.reduce(
        previousState: State,
        action: Action,
    ): State = this@StoreReducer.reduce(this, previousState, action)
}
