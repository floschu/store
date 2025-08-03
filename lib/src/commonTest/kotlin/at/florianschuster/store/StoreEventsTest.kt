package at.florianschuster.store

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds

class StoreEventsTest {

    private enum class Action { Start, Add, Cancel }

    @Test
    fun `events are emitted by Store`() = runTest {
        val events = mutableListOf<StoreEvent>()
        val sut: Store<Unit, Action, Int> = Store(
            initialState = 0,
            effectScope = backgroundScope,
            environment = Unit,
            reducer = Reducer { previousState, action ->
                when (action) {
                    Action.Start -> {
                        effect("effect_start") {
                            delay(100)
                            dispatch(Action.Add)
                        }
                        previousState
                    }

                    Action.Add -> {
                        effect("effect_add") {
                            delay(200)
                            dispatch(Action.Cancel)
                        }
                        previousState + 1
                    }

                    Action.Cancel -> {
                        effect(id = "effect_cancel") { }
                        cancelEffect(id = "effect_cancel")
                        previousState
                    }
                }
            },
            events = events::add,
        )

        sut.dispatch(Action.Start)

        with(events[0]) {
            assertIs<StoreEvent.Initialization<Int, Unit>>(this)
            assertEquals(0, initialState)
            assertEquals(Unit, environment)
            assertFalse(hasInitialEffect)
        }
        with(events[1]) {
            assertIs<StoreEvent.Dispatch<Action>>(this)
            assertEquals(Action.Start, action)
        }
        with(events[2]) {
            assertIs<StoreEvent.Reduce<Action, Int>>(this)
            assertEquals(0, previousState)
            assertEquals(Action.Start, action)
            assertEquals(0, newState)
        }

        advanceTimeBy(101.milliseconds)
        with(events[3]) {
            assertIs<StoreEvent.Effect.Launch>(this)
            assertEquals("effect_start", effectId)
        }
        with(events[4]) {
            assertIs<StoreEvent.Dispatch<Action>>(this)
            assertEquals(Action.Add, action)
        }
        with(events[5]) {
            assertIs<StoreEvent.Reduce<Action, Int>>(this)
            assertEquals(0, previousState)
            assertEquals(Action.Add, action)
            assertEquals(1, newState)
        }

        advanceTimeBy(200.milliseconds)
        with(events[6]) {
            assertIs<StoreEvent.Effect.Launch>(this)
            assertEquals("effect_add", effectId)
        }
        with(events[7]) {
            assertIs<StoreEvent.Dispatch<Action>>(this)
            assertEquals(Action.Cancel, action)
        }
        with(events[8]) {
            assertIs<StoreEvent.Reduce<Action, Int>>(this)
            assertEquals(1, previousState)
            assertEquals(Action.Cancel, action)
            assertEquals(1, newState)
        }

        with(events[9]) {
            assertIs<StoreEvent.Effect.Launch>(this)
            assertEquals("effect_cancel", effectId)
        }

        with(events[10]) {
            assertIs<StoreEvent.Effect.Cancel>(this)
            assertEquals("effect_cancel", effectId)
        }

        assertEquals(11, events.count())
    }
}
