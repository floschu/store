package at.florianschuster.store.example

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NavigationStoreTest {

    @Test
    fun `initial state is Login`() = runTest {
        val sut = NavigationStore(backgroundScope)

        with(sut.state.value) {
            assertIs<NavigationState.Route.Login>(route)
            assertFalse(canGoBack)
        }
        assertFailsWith<IllegalStateException> { sut.dispatch(NavigationAction.GoBack) }
    }

    @Test
    fun `when GoTo Search dispatched, then route is search and cannot go back`() = runTest {
        val sut = NavigationStore(backgroundScope)

        sut.dispatch(NavigationAction.GoTo(NavigationState.Route.Search))
        with(sut.state.value) {
            assertIs<NavigationState.Route.Search>(route)
            assertFalse(canGoBack)
        }
        assertFailsWith<IllegalStateException> { sut.dispatch(NavigationAction.GoBack) }
    }


    @Test
    fun `when GoTo Detail dispatched, then route is detail and can go back`() = runTest {
        val sut = NavigationStore(backgroundScope)

        val id = "123"
        sut.dispatch(NavigationAction.GoTo(NavigationState.Route.Detail(id)))
        with(sut.state.value) {
            assertIs<NavigationState.Route.Detail>(route)
            assertEquals(id, route.id)
            assertTrue(canGoBack)
        }

        sut.dispatch(NavigationAction.GoBack)
        with(sut.state.value) {
            assertIs<NavigationState.Route.Search>(route)
        }
    }
}