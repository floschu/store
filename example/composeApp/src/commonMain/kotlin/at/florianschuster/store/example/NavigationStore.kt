package at.florianschuster.store.example

import at.florianschuster.store.Reducer
import at.florianschuster.store.Store
import kotlinx.coroutines.CoroutineScope

internal sealed interface NavigationAction {
    data class GoTo(val route: NavigationState.Route) : NavigationAction
    data object GoBack : NavigationAction
}

internal data class NavigationState(
    val route: Route = initial,
) {
    val canGoBack = route is Route.Detail

    sealed interface Route {
        data object Login : Route
        data object Search : Route
        data class Detail(val id: String) : Route
    }

    companion object {
        val initial = Route.Login
    }
}

internal class NavigationStore(
    scope: CoroutineScope,
) : Store<Unit, NavigationAction, NavigationState> by Store(
    initialState = NavigationState(),
    environment = Unit,
    effectScope = scope,
    reducer = Reducer { previousState, action ->
        when (action) {
            is NavigationAction.GoTo -> previousState.copy(route = action.route)

            is NavigationAction.GoBack -> when (previousState.route) {
                is NavigationState.Route.Login,
                is NavigationState.Route.Search -> {
                    error("Cannot go back from ${previousState.route}")
                }

                is NavigationState.Route.Detail -> {
                    previousState.copy(route = NavigationState.Route.Search)
                }
            }
        }
    },
)
