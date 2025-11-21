package at.florianschuster.store.example

import at.florianschuster.store.Store
import at.florianschuster.store.example.service.MockTokenRepository
import at.florianschuster.store.example.service.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NavigationReducerTest {

    private class Setup(scope: CoroutineScope) {
        val tokenRepository = MockTokenRepository()
        val sut = Store(
            initialState = NavigationState(),
            environment = NavigationEnvironment(tokenRepository),
            effectScope = scope,
            reducer = NavigationReducer,
        )

        val currentState: NavigationState
            get() = sut.state.value
    }

    @Test
    fun `initial state is Login`() = runTest {
        with(Setup(backgroundScope)) {
            with(currentState) {
                assertIs<NavigationState.Route.Login>(route)
                assertFalse(canGoBack)
            }
            assertFailsWith<IllegalStateException> { sut.dispatch(NavigationAction.GoBack) }
        }
    }

    @Test
    fun `when GoTo Search dispatched - then route is search and cannot go back`() = runTest {
        with(Setup(backgroundScope)) {

            sut.dispatch(NavigationAction.GoTo(NavigationState.Route.Search))
            with(currentState) {
                assertIs<NavigationState.Route.Search>(route)
                assertFalse(canGoBack)
            }
            assertFailsWith<IllegalStateException> { sut.dispatch(NavigationAction.GoBack) }
        }
    }

    @Test
    fun `when GoTo Detail dispatched - then route is detail and can go back`() = runTest {
        with(Setup(backgroundScope)) {

            val id = "123"
            sut.dispatch(NavigationAction.GoTo(NavigationState.Route.Detail(id)))
            with(currentState) {
                assertIs<NavigationState.Route.Detail>(route)
                assertEquals(id, route.id)
                assertTrue(canGoBack)
            }

            sut.dispatch(NavigationAction.GoBack)
            assertIs<NavigationState.Route.Search>(currentState.route)
        }
    }

    @Test
    fun `given on Search - when not authenticated - then navigates to Login`() = runTest {
        with(Setup(backgroundScope)) {
            tokenRepository.clear()
            sut.dispatch(NavigationAction.GoTo(NavigationState.Route.Search))
            runCurrent()
            assertIs<NavigationState.Route.Login>(currentState.route)
        }
    }

    @Test
    fun `given on Login - when authenticated - then navigates to Search`() = runTest {
        with(Setup(backgroundScope)) {
            tokenRepository.store(Token("valid"))
            sut.dispatch(NavigationAction.GoTo(NavigationState.Route.Login))
            runCurrent()
            assertIs<NavigationState.Route.Search>(currentState.route)
        }
    }
}