package at.florianschuster.store.example.search

import at.florianschuster.store.Store
import at.florianschuster.store.example.service.SearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class SearchReducerTest {

    private class Setup(scope: CoroutineScope) {
        val executedQueries = mutableListOf<String>()
        private val testSearchRepository = object : SearchRepository {
            override suspend fun loadQueryItems(query: String): List<String> {
                executedQueries += query
                return TestQueryResult
            }
        }

        val sut = Store(
            initialState = SearchState(),
            environment = SearchEnvironment(testSearchRepository),
            effectScope = scope,
            reducer = SearchReducer
        )

        val currentState: SearchState
            get() = sut.state.value

        companion object {
            val TestQueryResult = listOf("QueryResult")
        }
    }

    @Test
    fun `when QueryChanged dispatched - then state updates and repository called after debounce`() = runTest {
        with(Setup(backgroundScope)) {
            sut.dispatch(SearchAction.QueryChanged("test"))

            advanceTimeBy(300.milliseconds)
            // Then: still loading, no repo call yet
            assertTrue(currentState.loading)
            assertEquals("test", currentState.query)
            assertTrue(executedQueries.isEmpty())

            advanceTimeBy(1.milliseconds)
            // Then: repo called, items loaded
            assertEquals(listOf("test"), executedQueries)
            assertEquals(Setup.TestQueryResult, currentState.items)
            assertFalse(currentState.loading)
        }
    }

    @Test
    fun `when ItemsLoaded dispatched - then items and loading are updated`() = runTest {
        with(Setup(backgroundScope)) {
            sut.dispatch(SearchAction.ItemsLoaded(listOf("A", "B")))

            assertEquals(listOf("A", "B"), currentState.items)
            assertFalse(currentState.loading)
        }
    }

    @Test
    fun `given pending query - when ResetQuery dispatched - then state resets and effect is cancelled`() =
        runTest {
            with(Setup(backgroundScope)) {
                sut.dispatch(SearchAction.QueryChanged("test"))

                advanceTimeBy(100.milliseconds)
                sut.dispatch(SearchAction.ResetQuery)
                assertTrue(currentState.query.isEmpty())
                assertTrue(currentState.items.isEmpty())

                // Then: effect should be cancelled, repo not called
                advanceTimeBy(300.milliseconds)
                assertTrue(executedQueries.isEmpty())
            }
        }
}
