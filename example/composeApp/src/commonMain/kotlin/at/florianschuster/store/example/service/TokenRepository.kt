package at.florianschuster.store.example.service

import kotlin.jvm.JvmInline

@JvmInline
value class Token(val value: String)

internal interface TokenRepository {
    suspend fun store(token: Token)
}

internal class MockTokenRepository : TokenRepository {
    private var storedToken: Token? = null

    override suspend fun store(token: Token) {
        storedToken = token
    }
}
