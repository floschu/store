package at.florianschuster.store.example.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlin.jvm.JvmInline

@JvmInline
value class Token(val value: String)

internal interface TokenRepository {
    val token: StateFlow<Token?>
    suspend fun store(token: Token)
    suspend fun clear()
}

internal val TokenRepository.isAuthenticated: Flow<Boolean>
    get() = token.map { it != null }

internal class MockTokenRepository : TokenRepository {
    private val _token = MutableStateFlow<Token?>(null)
    override val token: StateFlow<Token?> = _token.asStateFlow()

    override suspend fun store(token: Token) {
        _token.value = token
    }

    override suspend fun clear() {
        _token.value = null
    }
}
