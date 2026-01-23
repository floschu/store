package at.florianschuster.store.example.search

import androidx.compose.runtime.Immutable
import at.florianschuster.store.Reducer
import at.florianschuster.store.cancelEffect
import at.florianschuster.store.effect
import at.florianschuster.store.example.Log
import at.florianschuster.store.example.service.SearchRepository
import at.florianschuster.store.example.service.TokenRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

internal data class SearchEnvironment(
    val tokenRepository: TokenRepository,
    val searchRepository: SearchRepository,
)

internal sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction
    data class ItemsLoaded(val items: List<String>) : SearchAction
    data object ResetQuery : SearchAction
    data object Logout : SearchAction
}

@Immutable
internal data class SearchState(
    val query: String = "",
    val items: List<String> = emptyList(),
    val loading: Boolean = false,
)

internal val SearchReducer = Reducer<SearchEnvironment, SearchAction, SearchState> { previousState, action ->
    when (action) {
        is SearchAction.QueryChanged -> {
            // Cancel any previous search effect
            cancelEffect(id = SearchAction.QueryChanged::class)

            // Don't trigger search for empty queries - just clear results
            if (action.query.isEmpty()) {
                return@Reducer previousState.copy(
                    query = "",
                    items = emptyList(),
                    loading = false,
                )
            }

            effect(id = SearchAction.QueryChanged::class) {
                delay(300.milliseconds) // we debounce the search query
                runCatching { environment.searchRepository.loadQueryItems(action.query) }.fold(
                    onSuccess = { items -> dispatch(SearchAction.ItemsLoaded(items)) },
                    onFailure = { error ->
                        Log.e(error)
                        // On error, dispatch empty results to clear loading state
                        // and avoid leaving the UI in a loading state forever
                        if (error !is CancellationException) {
                            dispatch(SearchAction.ItemsLoaded(emptyList()))
                        }
                    }
                )
            }
            previousState.copy(
                query = action.query,
                loading = true
            )
        }

        is SearchAction.ItemsLoaded -> {
            previousState.copy(
                items = action.items,
                loading = false,
            )
        }

        is SearchAction.ResetQuery -> {
            // if a query effect is currently being processed, we cancel it
            cancelEffect(id = SearchAction.QueryChanged::class)
            previousState.copy(
                query = "",
                items = emptyList(),
                loading = false,
            )
        }

        is SearchAction.Logout -> {
            effect("effect_logout") {
                environment.tokenRepository.clear()
            }
            previousState
        }
    }
}
