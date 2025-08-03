package at.florianschuster.store.example

import at.florianschuster.store.Store
import at.florianschuster.store.StoreEvents
import at.florianschuster.store.delegate
import at.florianschuster.store.effect
import at.florianschuster.store.scopeAction
import at.florianschuster.store.example.login.LoginAction
import at.florianschuster.store.example.login.LoginEnvironment
import at.florianschuster.store.example.login.LoginState
import at.florianschuster.store.example.login.LoginStore
import at.florianschuster.store.example.search.SearchAction
import at.florianschuster.store.example.search.SearchEnvironment
import at.florianschuster.store.example.search.SearchReducer
import at.florianschuster.store.example.search.SearchState
import at.florianschuster.store.example.service.MockInputValidator
import at.florianschuster.store.example.service.MockAuthenticationService
import at.florianschuster.store.example.service.MockSearchRepository
import at.florianschuster.store.example.service.MockTokenRepository
import kotlinx.coroutines.CoroutineScope

internal sealed interface AppAction {
    data class Navigation(val action: NavigationAction) : AppAction
    data class Login(val action: LoginAction) : AppAction
    data class Search(val action: SearchAction) : AppAction
}

internal data class AppState(
    val navigationState: NavigationState = NavigationState(),
    val loginState: LoginState = LoginState(),
    val searchState: SearchState = SearchState(),
)

internal class AppStore(
    effectScope: CoroutineScope,
    initialState: AppState = AppState(),
    loginEnvironment: LoginEnvironment = LoginEnvironment(
        inputValidator = MockInputValidator(),
        authenticationService = MockAuthenticationService(),
        tokenRepository = MockTokenRepository(),
    ),
    searchEnvironment: SearchEnvironment = SearchEnvironment(
        searchRepository = MockSearchRepository(),
    ),
) : Store<Unit, AppAction, AppState> by Store(
    initialState = initialState,
    effectScope = effectScope,
    environment = Unit,
    delegates = listOf(
        NavigationStore(
            scope = effectScope,
        ).delegate(
            scopeAction = scopeAction(AppAction.Navigation::action),
            expandState = { appState, navigationState -> appState.copy(navigationState = navigationState) },
        ),
        LoginStore(
            initialState = initialState.loginState,
            environment = loginEnvironment,
            scope = effectScope,
        ).delegate(
            scopeAction = scopeAction(AppAction.Login::action),
            expandState = { appState, loginState ->
                if (loginState.isAuthenticated) {
                    effect("effect_navigate_to_search") {
                        dispatch(AppAction.Navigation(NavigationAction.GoTo(NavigationState.Route.Search)))
                    }
                }
                appState.copy(loginState = loginState)
            },
        ),
        SearchReducer.delegate(
            initialState = initialState.searchState,
            environment = searchEnvironment,
            effectScope = effectScope,
            scopeAction = scopeAction(AppAction.Search::action),
            expandState = { appState, searchState -> appState.copy(searchState = searchState) },
        ),
    ),
    events = StoreEvents.Println("app"),
)
