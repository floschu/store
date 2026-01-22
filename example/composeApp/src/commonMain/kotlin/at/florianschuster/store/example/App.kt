package at.florianschuster.store.example

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.florianschuster.store.example.AppAction.Navigation
import at.florianschuster.store.example.NavigationState.Route.Detail
import at.florianschuster.store.example.NavigationState.Route.Login
import at.florianschuster.store.example.NavigationState.Route.Search
import at.florianschuster.store.example.detail.DetailState
import at.florianschuster.store.example.detail.DetailView
import at.florianschuster.store.example.login.LoginView
import at.florianschuster.store.example.search.SearchView
import example.composeapp.generated.resources.Res
import example.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    val store = rememberAppStore()
    val state by store.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.navigationState.canGoBack) {
        store.dispatch(Navigation(NavigationAction.GoBack))
    }
    AppTheme {
        Scaffold(
            topBar = { AppTopBar(state = state, dispatch = store::dispatch) },
        ) { paddingValues ->
            AnimatedContent(
                targetState = state.navigationState.route,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                contentKey = { route ->
                    when (route) {
                        is Login -> "login"
                        is Search -> "search"
                        is Detail -> "detail-${route.id}"
                    }
                },
            ) { targetRoute ->
                when (targetRoute) {
                    is Login -> {
                        LoginView(
                            paddingValues = paddingValues,
                            state = state.loginState,
                            dispatch = { store.dispatch(AppAction.Login(it)) },
                        )
                    }

                    is Search -> SearchView(
                        paddingValues = paddingValues,
                        state = state.searchState,
                        dispatch = { store.dispatch(AppAction.Search(it)) },
                        onGoToDetail = { title ->
                            store.dispatch(Navigation(NavigationAction.GoTo(Detail(title))))
                        }
                    )

                    is Detail -> DetailView(
                        paddingValues = paddingValues,
                        state = DetailState(targetRoute.id),
                    )
                }
            }
        }
    }
}

@Composable
internal fun rememberAppStore(): AppStore {
    val scope = rememberCoroutineScope()
    return remember(scope) { AppStore(scope) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppTopBar(
    state: AppState,
    dispatch: (AppAction) -> Unit
) {
    val route = state.navigationState.route
    if (route is Login) return
    CenterAlignedTopAppBar(
        title = {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
            )
        },
        navigationIcon = {
            if (state.navigationState.canGoBack) {
                IconButton(
                    onClick = { dispatch(Navigation(NavigationAction.GoBack)) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "go back",
                    )
                }
            }
        },
    )
}