package at.florianschuster.store.example.search

import androidx.compose.runtime.Immutable
import at.florianschuster.store.Reducer
import at.florianschuster.store.cancelEffect
import at.florianschuster.store.effect
import at.florianschuster.store.example.service.SearchRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

internal data class SearchEnvironment(
    val searchRepository: SearchRepository,
)

internal sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction
    data class ItemsLoaded(val items: List<String>) : SearchAction
    data object ResetQuery : SearchAction
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
            // if a new query is entered, we cancel the previous effect
            cancelEffect(id = SearchAction.QueryChanged::class)
            effect(id = SearchAction.QueryChanged::class) {
                delay(300.milliseconds) // we debounce the search query
                val items = environment.searchRepository.loadQueryItems(action.query)
                dispatch(SearchAction.ItemsLoaded(items))
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
            )
        }
    }
}
