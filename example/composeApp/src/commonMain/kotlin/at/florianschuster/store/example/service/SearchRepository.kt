package at.florianschuster.store.example.service

import kotlinx.coroutines.delay

internal interface SearchRepository {
    suspend fun loadQueryItems(query: String): List<String>
}

internal class MockSearchRepository : SearchRepository {
    override suspend fun loadQueryItems(
        query: String,
    ): List<String> {
        delay(500)
        return DefaultItems.filter { item ->
            item.contains(query, ignoreCase = true)
        }
    }

    companion object {
        val DefaultItems = (0..100).map { "Item $it" }
    }
}