package at.florianschuster.store

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class StoreTest {

    @Test
    fun `store initialEffect works as expected`() = runTest {
        val sut: Store<Unit, Int, Int> = Store(
            initialState = 42,
            effectScope = backgroundScope,
            environment = Unit,
            initialEffect = effect { dispatch(1) },
            reducer = Reducer { previousState, action -> previousState + action },
        )
        runCurrent()
        assertEquals(43, sut.state.value)
    }

    @Test
    fun `store reducer works as expected`() = runTest {
        val injectedEnvironment = "something"

        val sut: Store<String, Int, Int> = Store(
            initialState = 0,
            effectScope = backgroundScope,
            environment = injectedEnvironment,
            reducer = Reducer { previousState, action ->
                assertEquals(injectedEnvironment, environment)
                previousState + action
            },
        )

        sut.dispatch(1)
        sut.dispatch(2)
        sut.dispatch(3)

        assertEquals(6, sut.state.value)
    }

    private enum class AddAction { Start, Add, Different }

    @Test
    fun `store  effects work as expected`() = runTest {
        val injectedEnvironment = "something"
        val sut: Store<String, AddAction, Int> = Store(
            initialState = 0,
            environment = injectedEnvironment,
            effectScope = backgroundScope,
            reducer = Reducer { previousState, action ->
                when (action) {
                    AddAction.Start -> {
                        effect {
                            assertEquals(injectedEnvironment, environment)
                            dispatch(AddAction.Add)
                        }
                        effect {
                            assertEquals(injectedEnvironment, environment)
                            dispatch(AddAction.Add)
                        }
                        effect {
                            assertEquals(injectedEnvironment, environment)
                            dispatch(AddAction.Different)
                            dispatch(AddAction.Add)
                        }
                        previousState
                    }

                    AddAction.Add -> previousState + 1
                    AddAction.Different -> previousState + 2
                }
            },
            events = StoreEvents.Println(),
        )

        sut.dispatch(AddAction.Start)
        runCurrent()

        assertEquals(5, sut.state.value)
    }

    enum class StopWatchAction { Start, CountUp, Cancel }

    @Test
    fun `cancellation works as expected`() = runTest {
        val stopWatchDelay = 1.seconds
        val sut: Store<Unit, StopWatchAction, Long> = Store(
            initialState = 0,
            environment = Unit,
            effectScope = backgroundScope,
            reducer = Reducer { previousState, action ->
                when (action) {
                    StopWatchAction.Start -> {
                        effect(id = StopWatchAction.Start) {
                            while (true) {
                                delay(stopWatchDelay)
                                dispatch(StopWatchAction.CountUp)
                            }
                        }
                        previousState
                    }

                    StopWatchAction.Cancel -> {
                        cancelEffect(id = StopWatchAction.Start)
                        previousState
                    }

                    StopWatchAction.CountUp -> {
                        previousState + 1
                    }
                }
            },

            events = StoreEvents.Println(),
        )

        sut.dispatch(StopWatchAction.Start)
        advanceTimeBy(stopWatchDelay * 3 + 1.milliseconds)
        sut.dispatch(StopWatchAction.Cancel)
        assertEquals(3, sut.state.value)

        sut.dispatch(StopWatchAction.Start)
        advanceTimeBy(stopWatchDelay / 2)
        sut.dispatch(StopWatchAction.Cancel)
        assertEquals(3, sut.state.value)

        sut.dispatch(StopWatchAction.Start)
        advanceTimeBy(stopWatchDelay + 1.milliseconds)
        sut.dispatch(StopWatchAction.Cancel)
        assertEquals(4, sut.state.value)

        sut.dispatch(StopWatchAction.Start)
        advanceTimeBy(stopWatchDelay * 2 + 1.milliseconds)
        sut.dispatch(StopWatchAction.Cancel)
        assertEquals(6, sut.state.value)
    }

    @Test
    fun `restarting works as expected`() = runTest {
        val sut: Store<Unit, Unit, Int> = Store(
            initialState = 0,
            environment = Unit,
            effectScope = backgroundScope,
            reducer = Reducer { previousState, _ ->
                cancelEffect(id = "restartable_action")
                effect(id = "restartable_action") {
                    delay(1_000)
                    dispatch(Unit)
                }
                previousState + 1
            },
            events = StoreEvents.Println(),
        )

        sut.dispatch(Unit)
        advanceTimeBy(1_000 + 1)
        assertEquals(2, sut.state.value)

        sut.dispatch(Unit)
        advanceTimeBy(500 + 1)
        sut.dispatch(Unit)
        advanceTimeBy(1_000 + 1)
        assertEquals(5, sut.state.value)
    }

    @Test
    fun `reducer throws`() = runTest {
        val sut: Store<Unit, Int, Int> = Store(
            initialState = 0,
            effectScope = backgroundScope,
            environment = Unit,
            reducer = Reducer { _, _ -> error("error") },
        )
        assertFailsWith<IllegalStateException> { sut.dispatch(1) }
    }
}
