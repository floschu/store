package at.florianschuster.store

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DelegateTest {

    @Test
    fun `delegating Reducers`() = runTest {
        val sut: Store<Unit, Int, Int> = Store(
            initialState = 0,
            environment = Unit,
            effectScope = backgroundScope,
            delegates = listOf(
                reducer1.delegate(
                    initialState = 0,
                    environment = 1,
                    effectScope = backgroundScope,
                    scopeAction = { it },
                    expandState = { state, delegateState -> state + delegateState }
                ),
                reducer2.delegate(
                    initialState = 0,
                    environment = 2,
                    effectScope = backgroundScope,
                    scopeAction = { it },
                    expandState = { state, delegateState -> state + delegateState }
                ),
                reducer3.delegate(
                    initialState = 0,
                    environment = 3,
                    effectScope = backgroundScope,
                    scopeAction = { it },
                    expandState = { state, delegateState -> state + delegateState }
                ),
            )
        )

        assertEquals(0, sut.state.value)

        sut.dispatch(42)
        runCurrent()

        // 0 +42 +2 +42*2
        assertEquals(128, sut.state.value)
    }

    @Test
    fun `delegating Stores`() = runTest {
        val sut: Store<Unit, Int, Int> = Store(
            initialState = 0,
            environment = Unit,
            effectScope = backgroundScope,
            delegates = listOf(
                Store(
                    initialState = 0,
                    environment = 1,
                    effectScope = backgroundScope,
                    reducer = reducer1
                ).delegate(
                    scopeAction = { it },
                    expandState = { parentState, childState -> parentState + childState }
                ),
                Store(
                    initialState = 0,
                    environment = 2,
                    effectScope = backgroundScope,
                    reducer = reducer2
                ).delegate(
                    scopeAction = { it },
                    expandState = { parentState, childState -> parentState + childState }
                ),
                Store(
                    initialState = 0,
                    environment = 3,
                    effectScope = backgroundScope,
                    reducer = reducer3
                ).delegate(
                    scopeAction = { it },
                    expandState = { parentState, childState -> parentState + childState }
                ),
            )
        )

        assertEquals(0, sut.state.value)

        sut.dispatch(42)
        runCurrent()

        // 0 +42 +2 +42*2
        assertEquals(128, sut.state.value)
    }

    @Test
    fun `delegating Reducers but not all can handle actions`() = runTest {
        val sut: Store<Unit, Int, Int> = Store(
            initialState = 0,
            environment = Unit,
            effectScope = backgroundScope,
            delegates = listOf(
                reducer1.delegate(
                    initialState = 0,
                    environment = 1,
                    effectScope = backgroundScope,
                    scopeAction = { it },
                    expandState = { parentState, childState -> parentState + childState }
                ),
                reducer2.delegate(
                    initialState = 0,
                    environment = 2,
                    effectScope = backgroundScope,
                    scopeAction = { null },
                    expandState = { parentState, childState -> parentState + childState }
                ),
            )
        )

        assertEquals(0, sut.state.value)

        sut.dispatch(42)
        runCurrent()

        // 0 +42
        assertEquals(42, sut.state.value)
    }

    @Test
    fun `delegating Reducers with effects in reduce`() = runTest {
        val sut: Store<Unit, Int, Int> = Store(
            initialState = 0,
            environment = Unit,
            effectScope = backgroundScope,
            delegates = listOf(
                reducer1.delegate(
                    initialState = 0,
                    environment = 1,
                    effectScope = backgroundScope,
                    scopeAction = { it },
                    expandState = { parentState, childState ->
                        parentState + childState
                    },
                    events = StoreEvents.Println("reducer1"),
                ),
                reducer2.delegate(
                    initialState = 0,
                    environment = 2,
                    effectScope = backgroundScope,
                    scopeAction = { it },
                    expandState = { parentState, _ ->
                        effect(id = "2") {
                            dispatch(12)
                            suspend { }
                        }
                        parentState
                    },
                    events = StoreEvents.Println("reducer2"),
                ),
                reducer3.delegate(
                    initialState = 0,
                    environment = 3,
                    effectScope = backgroundScope,
                    scopeAction = { null },
                    expandState = { parentState, _ -> parentState },
                    events = StoreEvents.Println("reducer3"),
                ),
            ),
            events = StoreEvents.Println("sut"),
        )

        assertEquals(0, sut.state.value)

        sut.dispatch(42)

        runCurrent()
        // sut > expand > (0, 42) -> 42 via [expansion > (0, 42) -> 42, then expansion > (42, 2) -> 42]
        // sut > expand > (42, 12) -> 96 via [expansion > (42, 54) -> 96, then expansion > (96, 4) -> 96]
        assertEquals(96, sut.state.value)
    }

    @Test
    fun `delegating Stores where delegate expand state by themselves`() = runTest {
        val sut: Store<Unit, Int, Int> = Store(
            initialState = 0,
            environment = Unit,
            effectScope = backgroundScope,
            delegates = listOf(
                Store(
                    initialState = 42,
                    environment = 1,
                    effectScope = backgroundScope,
                    reducer = Reducer { previousState, action ->
                        effect(id = "id") {
                            delay(1.seconds)
                            dispatch(1)

                            delay(1.seconds)
                            dispatch(2)

                            delay(1.seconds)
                            dispatch(3)
                        }
                        previousState + action
                    },
                    events = StoreEvents.Println("reducer1"),
                ).delegate(
                    scopeAction = { it },
                    expandState = { _, childState -> childState },
                ),
            ),
            events = StoreEvents.Println("sut"),
        )

        runCurrent()
        assertEquals(42, sut.state.value)

        sut.dispatch(0)
        runCurrent()

        advanceTimeBy(1.seconds + 1.milliseconds)
        assertEquals(43, sut.state.value)

        advanceTimeBy(1.seconds + 1.milliseconds)
        assertEquals(45, sut.state.value)

        advanceTimeBy(1.seconds + 1.milliseconds)
        assertEquals(48, sut.state.value)
    }

    @Test
    fun `delegating Stores where delegate has initialEffect`() = runTest {
        val sut: Store<Unit, Int, Int> = Store(
            initialState = 0,
            environment = Unit,
            effectScope = backgroundScope,
            delegates = listOf(
                Store(
                    initialState = 42,
                    environment = 1,
                    effectScope = backgroundScope,
                    reducer = Reducer(
                        initialEffect = effect { dispatch(0) }
                    ) { previousState, action ->
                        effect(id = "id") {
                            delay(1.seconds)
                            dispatch(1)

                            delay(1.seconds)
                            dispatch(2)

                            delay(1.seconds)
                            dispatch(3)
                        }
                        previousState + action
                    },
                    events = StoreEvents.Println("reducer1"),
                ).delegate(
                    scopeAction = { it },
                    expandState = { _, childState -> childState },
                ),
            ),
            events = StoreEvents.Println("sut"),
        )

        runCurrent()
        assertEquals(42, sut.state.value)

        advanceTimeBy(1.seconds + 1.milliseconds)
        assertEquals(43, sut.state.value)

        advanceTimeBy(1.seconds + 1.milliseconds)
        assertEquals(45, sut.state.value)

        advanceTimeBy(1.seconds + 1.milliseconds)
        assertEquals(48, sut.state.value)
    }

    sealed interface Child1Action {
        data object Test : Child1Action
    }

    sealed interface Child2Action {
        data object Test : Child2Action
    }

    sealed interface ParentAction {
        data class Child1(val action: Child1Action) : ParentAction
        data class Child2(val action: Child2Action) : ParentAction
    }

    @Test
    fun scopeAction() {
        val sutChild1 = scopeAction(ParentAction.Child1::action)
        val sutChild2 = scopeAction(ParentAction.Child2::action)

        assertEquals(Child1Action.Test, sutChild1(ParentAction.Child1(Child1Action.Test)))
        assertNull(sutChild1(ParentAction.Child2(Child2Action.Test)))

        assertNull(sutChild2(ParentAction.Child1(Child1Action.Test)))
        assertEquals(Child2Action.Test, sutChild2(ParentAction.Child2(Child2Action.Test)))
    }

    companion object {
        val reducer1 = Reducer<Int, Int, Int> { previousState, action ->
            (previousState + action)
        }
        val reducer2 = Reducer<Int, Int, Int> { previousState, _ ->
            (previousState + environment)
        }
        val reducer3 = Reducer<Int, Int, Int> { previousState, action ->
            (previousState + action * 2)
        }
    }
}
