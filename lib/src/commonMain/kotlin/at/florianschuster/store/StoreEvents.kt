package at.florianschuster.store

/**
 * Emits [StoreEvent]s.
 */
fun interface StoreEvents {

    fun emit(event: StoreEvent)

    companion object {

        /**
         * Logs [Store] events via [println].
         */
        fun Println(tag: String = "store"): StoreEvents = StoreEvents { event -> println("$tag > $event") }
    }
}

/**
 * All events emitted by a [Store].
 */
sealed interface StoreEvent {

    class Initialization<State, Environment> internal constructor(
        val initialState: State,
        val environment: Environment,
        val hasInitialEffect: Boolean,
    ) : StoreEvent {
        override fun toString(): String =
            "init > (initialState=\"$initialState\", " +
                    "environment=\"$environment\", " +
                    "initialEffect=\"$hasInitialEffect\")"
    }

    class Dispatch<Action> internal constructor(
        val action: Action,
    ) : StoreEvent {
        override fun toString(): String =
            "dispatch > \"$action\""
    }

    class Reduce<Action, State> internal constructor(
        val previousState: State,
        val action: Action,
        val newState: State,
    ) : StoreEvent {
        override fun toString(): String =
            "reduce > (\"$previousState\", \"$action\") -> \"$newState\""
    }

    class ExpandState<State, DelegateState> internal constructor(
        val previousState: State,
        val delegateState: DelegateState,
        val expandedState: State,
    ) : StoreEvent {
        override fun toString(): String =
            "expansion > (\"$previousState\", \"$delegateState\") -> \"$expandedState\""
    }

    sealed interface Effect : StoreEvent {
        class Launch internal constructor(
            val effectId: Any?,
        ) : Effect {
            override fun toString(): String =
                "launch > effect" + if (effectId != null) ": \"$effectId\"" else ""
        }

        class Cancel internal constructor(
            val effectId: Any?,
        ) : Effect {
            override fun toString(): String =
                "cancel > effect: \"$effectId\""
        }
    }
}
