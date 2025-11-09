package at.florianschuster.store

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds

class StoreEventsTest {

    private enum class Action { Start, Add, Cancel }

    @Test
    fun `events are emitted by Store`() = runTest {
        val events = mutableListOf<StoreEvent>()
        fun effectEvents() = events.filterIsInstance<StoreEvent.Effect>()
        fun nonEffectEvents() = events.filterNot { it is StoreEvent.Effect }
        val sut: Store<Unit, Action, Int> = Store(
            initialState = 0,
            effectScope = backgroundScope,
            environment = Unit,
            reducer = Reducer { previousState, action ->
                when (action) {
                    Action.Start -> {
                        effect { awaitCancellation() }
                        effect(Action.Start) {
                            delay(100)
                            dispatch(Action.Add)
                        }
                        previousState
                    }

                    Action.Add -> {
                        effect(Action.Add) {
                            delay(200)
                            dispatch(Action.Cancel)
                        }
                        previousState + 1
                    }

                    Action.Cancel -> {
                        effect(id = Action.Cancel) { }
                        cancelEffect(id = Action.Cancel)
                        previousState
                    }
                }
            },
            events = {
                events.add(it)
                println(it)
            },
        )

        sut.dispatch(Action.Start)
        advanceUntilIdle()

        with(nonEffectEvents()[0]) {
            assertIs<StoreEvent.Initialization<Int, Unit>>(this)
            assertEquals(0, initialState)
            assertEquals(Unit, environment)
            assertFalse(hasInitialEffect)
        }
        with(nonEffectEvents()[1]) {
            assertIs<StoreEvent.Dispatch<Action>>(this)
            assertEquals(Action.Start, action)
        }
        with(nonEffectEvents()[2]) {
            assertIs<StoreEvent.Reduce<Action, Int>>(this)
            assertEquals(0, previousState)
            assertEquals(Action.Start, action)
            assertEquals(0, newState)
        }
        runCurrent()
        with(effectEvents()[0]) {
            assertIs<StoreEvent.Effect.Launch>(this)
            assertNull(effectId)
        }
        with(effectEvents()[1]) {
            assertIs<StoreEvent.Effect.Launch>(this)
            assertEquals(Action.Start, effectId)
        }
        advanceTimeBy(101.milliseconds)
        with(effectEvents()[2]) {
            assertIs<StoreEvent.Effect.Complete>(this)
            assertEquals(Action.Start, effectId)
        }

        with(nonEffectEvents()[3]) {
            assertIs<StoreEvent.Dispatch<Action>>(this)
            assertEquals(Action.Add, action)
        }
        with(nonEffectEvents()[4]) {
            assertIs<StoreEvent.Reduce<Action, Int>>(this)
            assertEquals(0, previousState)
            assertEquals(Action.Add, action)
            assertEquals(1, newState)
        }
        runCurrent()
        with(effectEvents()[3]) {
            assertIs<StoreEvent.Effect.Launch>(this)
            assertEquals(Action.Add, effectId)
        }
        advanceTimeBy(200.milliseconds)
        with(effectEvents()[4]) {
            assertIs<StoreEvent.Effect.Complete>(this)
            assertEquals(Action.Add, effectId)
        }

        with(nonEffectEvents()[5]) {
            assertIs<StoreEvent.Dispatch<Action>>(this)
            assertEquals(Action.Cancel, action)
        }
        with(nonEffectEvents()[6]) {
            assertIs<StoreEvent.Reduce<Action, Int>>(this)
            assertEquals(1, previousState)
            assertEquals(Action.Cancel, action)
            assertEquals(1, newState)
        }
        with(effectEvents()[5]) {
            assertIs<StoreEvent.Effect.Launch>(this)
            assertEquals(Action.Cancel, effectId)
        }
        with(effectEvents()[6]) {
            assertIs<StoreEvent.Effect.Cancel>(this)
            assertEquals(Action.Cancel, effectId)
        }

        assertEquals(7, nonEffectEvents().count())
        assertEquals(7, effectEvents().count())
        assertEquals(14, events.count())
    }
}
