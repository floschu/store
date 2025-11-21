package at.florianschuster.store.example

import at.florianschuster.store.Reducer
import at.florianschuster.store.effect
import at.florianschuster.store.example.service.TokenRepository
import at.florianschuster.store.example.service.isAuthenticated
import at.florianschuster.store.example.NavigationState.Route
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine

internal data class NavigationEnvironment(
    val tokenRepository: TokenRepository
)

internal sealed interface NavigationAction {
    data class GoTo(val route: Route) : NavigationAction
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

internal val NavigationReducer = Reducer<NavigationEnvironment, NavigationAction, NavigationState>(
    initialEffect = effect {
        combine(state, environment.tokenRepository.isAuthenticated) { state, isAuthenticated ->
            if (state.route == Route.Login && isAuthenticated) {
                dispatch(NavigationAction.GoTo(Route.Search))
            } else if (state.route != Route.Login && !isAuthenticated) {
                dispatch(NavigationAction.GoTo(Route.Login))
            }
        }.collect()
    },
) { previousState, action ->
    when (action) {
        is NavigationAction.GoTo -> previousState.copy(route = action.route)

        is NavigationAction.GoBack -> when (previousState.route) {
            is Route.Login,
            is Route.Search -> {
                error("Cannot go back from ${previousState.route}")
            }

            is Route.Detail -> {
                previousState.copy(route = Route.Search)
            }
        }
    }
}
