package at.florianschuster.store.example

import at.florianschuster.store.example.NavigationAction.GoTo
import at.florianschuster.store.example.NavigationState.Route
import at.florianschuster.store.example.login.LoginAction
import at.florianschuster.store.example.search.SearchAction
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * We only test that our actions are correctly delegated, the delegates are tested in their own tests.
 */
class AppStoreTest {

    @Test
    fun `when NavigationAction dispatched - then delegated to NavigationStore`() = runTest {
        val sut = AppStore(backgroundScope)
        sut.dispatch(AppAction.Navigation(GoTo(Route.Search)))
        runCurrent()
        assertEquals(Route.Search, sut.state.value.navigationState.route)
    }

    @Test
    fun `when LoginAction dispatched - then delegated to LoginStore`() = runTest {
        val sut = AppStore(backgroundScope)
        val input = "user@test.com"
        sut.dispatch(AppAction.Login(LoginAction.OnEmailEntered(input)))
        runCurrent()
        assertEquals(input, sut.state.value.loginState.email)
    }

    @Test
    fun `when SearchAction dispatched - then delegated to SearchReducer`() = runTest {
        val sut = AppStore(backgroundScope)
        val items = listOf("A", "B")
        sut.dispatch(AppAction.Search(SearchAction.ItemsLoaded(items)))
        runCurrent()
        assertEquals(items, sut.state.value.searchState.items)
    }
}