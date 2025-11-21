package at.florianschuster.store

import at.florianschuster.store.EffectExecution.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * An [Effect] represents a side effect that can be executed in the context of a [Reducer].
 * The [Effect] is executed right after the [Reducer] has processed an action.
 *
 * More information on [Effect]s:
 *  * [EffectExecution] via [effect]
 *  * [EffectCancellation] via [cancelEffect]/[cancelEffects]
 */
sealed interface Effect<Environment, Action, State>

/**
 * An [Effect] that executes a block of code.
 *
 * Use [effect] to create an [EffectExecution]
 *
 * @param id An optional identifier for the [Effect]. If an [Effect] has an [id], it can not be executed again
 * until [block] has completed or the [Effect] was cancelled via [EffectCancellation].
 * @param block The suspending block of code to be executed. It has access to [Context] which provides the
 * [Environment], [State] and a way to dispatch [Action]s.
 */
interface EffectExecution<Environment, Action, State> : Effect<Environment, Action, State> {
    val id: Any?
    val block: suspend Context<Environment, Action, State>.() -> Unit

    /**
     * The context in which [EffectExecution.block] is executed.
     */
    interface Context<Environment, Action, State> {
        val environment: Environment
        val state: StateFlow<State>
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
interface EffectCancellation<Environment, Action, State> : Effect<Environment, Action, State> {
    val ids: List<Any>
}

/**
 * Creates an [EffectExecution].
 *
 * @param id An optional identifier for the [Effect]. If an [Effect] has an [id], it can not be executed again
 * until [block] has completed or the [Effect] was cancelled via [EffectCancellation].
 * @param block The suspending block of code to be executed. It has access to [Context] which provides the
 * [Environment] and a way to dispatch actions.
 */
fun <Environment, Action, State> effect(
    id: Any? = null,
    block: suspend Context<Environment, Action, State>.() -> Unit,
): EffectExecution<Environment, Action, State> = object : EffectExecution<Environment, Action, State> {
    override val id: Any? = id
    override val block: suspend Context<Environment, Action, State>.() -> Unit = block
}

/**
 * Creates and adds an [EffectExecution] in the context of a [Reducer.reduce].
 *
 * @param id An optional identifier for the [Effect]. If an [Effect] has an [id], it can not be executed again
 * until [block] has completed or the [Effect] was cancelled via [EffectCancellation].
 * @param block The suspending block of code to be executed. It has access to [Context] which provides the
 * [Environment] and a way to dispatch actions.
 */
fun <Environment, Action, State> Reducer.Context<Environment, Action, State>.effect(
    id: Any? = null,
    block: suspend Context<Environment, Action, State>.() -> Unit,
) {
    val effect = object : EffectExecution<Environment, Action, State> {
        override val id: Any? = id
        override val block: suspend Context<Environment, Action, State>.() -> Unit = block
    }
    add(effect)
}

/**
 * Creates and adds an [EffectExecution] in the context of a [DelegateStore.expandState].
 *
 * @param id An optional identifier for the [Effect]. If an [Effect] has an [id], it can not be executed again
 * until [block] has completed or the [Effect] was cancelled via [EffectCancellation].
 * @param block The suspending block of code to be executed. It has access to [Context] which provides the
 * [Environment] and a way to dispatch actions.
 */
fun <Environment, Action, State> DelegateStore.ExpandStateContext<Environment, Action, State>.effect(
    id: Any? = null,
    block: suspend Context<Environment, Action, State>.() -> Unit,
) {
    val effect = object : EffectExecution<Environment, Action, State> {
        override val id: Any? = id
        override val block: suspend Context<Environment, Action, State>.() -> Unit = block
    }
    add(effect)
}

/**
 * Creates and adds an [EffectCancellation] in the context of a [Reducer.reduce].
 *
 * @param id An ID of the [Effect]s to be cancelled.
 */
fun <Environment, Action, State> Reducer.Context<Environment, Action, State>.cancelEffect(
    id: Any,
) {
    val effect = object : EffectCancellation<Environment, Action, State> {
        override val ids: List<Any> = listOf(id)
    }
    add(effect)
}

/**
 * Creates and adds an [EffectCancellation] in the context of a [DelegateStore.expandState].
 *
 * @param id An ID of the [Effect]s to be cancelled.
 */
fun <Environment, Action, State> DelegateStore.ExpandStateContext<Environment, Action, State>.cancelEffect(
    id: Any,
) {
    val effect = object : EffectCancellation<Environment, Action, State> {
        override val ids: List<Any> = listOf(id)
    }
    add(effect)
}

/**
 * Creates and adds an [EffectCancellation] in the context of a [Reducer.reduce].
 *
 * @param ids A list of IDs of the [Effect]s to be cancelled.
 */
fun <Environment, Action, State> Reducer.Context<Environment, Action, State>.cancelEffects(
    ids: List<Any>,
) {
    val effect = object : EffectCancellation<Environment, Action, State> {
        override val ids: List<Any> = ids
    }
    add(effect)
}

/**
 * Creates and adds an [EffectCancellation] in the context of a [DelegateStore.expandState].
 *
 * @param ids A list of IDs of the [Effect]s to be cancelled.
 */
fun <Environment, Action, State> DelegateStore.ExpandStateContext<Environment, Action, State>.cancelEffects(
    ids: List<Any>,
) {
    val effect = object : EffectCancellation<Environment, Action, State> {
        override val ids: List<Any> = ids
    }
    add(effect)
}
