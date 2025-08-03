package at.florianschuster.store

/**
 * An [Effect] represents a side effect that can be executed in the context of a [Reducer].
 * The [Effect] is executed right after the [Reducer] has processed an action.
 *
 * More information on [Effect]s:
 *  * [EffectExecution] via [effect]
 *  * [EffectCancellation] via [cancelEffect]/[cancelEffects]
 */
sealed interface Effect<Environment, Action>

/**
 * An [Effect] that executes a block of code.
 *
 * Use [effect] to create an [EffectExecution]
 *
 * @param id An optional identifier for the [Effect]. If an [Effect] has an [id], it can not be executed again
 * until [block] has completed or the [Effect] was cancelled via [EffectCancellation].
 * @param block The suspending block of code to be executed. It has access to [Context] which provides the
 * [Environment] and a way to dispatch actions.
 */
interface EffectExecution<Environment, Action> : Effect<Environment, Action> {
    val id: Any?
    val block: (suspend Context<Environment, Action>.() -> Unit)

    /**
     * The context in which [EffectExecution.block] is executed.
     */
    interface Context<Environment, Action> {
        val environment: Environment
        fun dispatch(action: Action)
    }
}

/**
 * An [Effect] that cancels one or more effects.
 *
 * Use [cancelEffect] to create an [EffectCancellation].
 *
 * @param ids A list of IDs of the [Effect]s to be cancelled.
 */
interface EffectCancellation<Environment, Action> : Effect<Environment, Action> {
    val ids: List<Any>
}

/**
 * Creates an [EffectExecution].
 */
fun <Environment, Action> effect(
    id: Any? = null,
    block: suspend EffectExecution.Context<Environment, Action>.() -> Unit,
) = object : EffectExecution<Environment, Action> {
    override val id: Any? = id
    override val block: suspend EffectExecution.Context<Environment, Action>.() -> Unit = block
}

/**
 * Creates and adds an [EffectExecution] in the context of a [Reducer.reduce].
 */
fun <Environment, Action> Reducer.Context<Environment, Action>.effect(
    id: Any? = null,
    block: suspend EffectExecution.Context<Environment, Action>.() -> Unit,
) {
    val effect = object : EffectExecution<Environment, Action> {
        override val id: Any? = id
        override val block: suspend EffectExecution.Context<Environment, Action>.() -> Unit = block
    }
    add(effect)
}

/**
 * Creates and adds an [EffectExecution] in the context of a [DelegateStore.expandState].
 */
fun <Environment, Action> DelegateStore.ExpandStateContext<Environment, Action>.effect(
    id: Any? = null,
    block: suspend EffectExecution.Context<Environment, Action>.() -> Unit,
) {
    val effect = object : EffectExecution<Environment, Action> {
        override val id: Any? = id
        override val block: suspend EffectExecution.Context<Environment, Action>.() -> Unit = block
    }
    add(effect)
}

/**
 * Creates and adds an [EffectCancellation] in the context of a [Reducer.reduce].
 */
fun <Environment, Action> Reducer.Context<Environment, Action>.cancelEffect(
    id: Any,
) {
    val effect = object : EffectCancellation<Environment, Action> {
        override val ids: List<Any> = listOf(id)
    }
    add(effect)
}

/**
 * Creates and adds an [EffectCancellation] in the context of a [DelegateStore.expandState].
 */
fun <Environment, Action> DelegateStore.ExpandStateContext<Environment, Action>.cancelEffect(
    id: Any,
) {
    val effect = object : EffectCancellation<Environment, Action> {
        override val ids: List<Any> = listOf(id)
    }
    add(effect)
}

/**
 * Creates and adds an [EffectCancellation] in the context of a [Reducer.reduce].
 */
fun <Environment, Action> Reducer.Context<Environment, Action>.cancelEffects(
    ids: List<Any>,
) {
    val effect = object : EffectCancellation<Environment, Action> {
        override val ids: List<Any> = ids
    }
    add(effect)
}

/**
 * Creates and adds an [EffectCancellation] in the context of a [DelegateStore.expandState].
 */
fun <Environment, Action> DelegateStore.ExpandStateContext<Environment, Action>.cancelEffects(
    ids: List<Any>,
) {
    val effect = object : EffectCancellation<Environment, Action> {
        override val ids: List<Any> = ids
    }
    add(effect)
}
