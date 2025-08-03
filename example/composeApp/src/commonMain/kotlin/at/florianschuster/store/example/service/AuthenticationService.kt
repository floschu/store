package at.florianschuster.store.example.service

import kotlinx.coroutines.delay

internal interface AuthenticationService {
    suspend fun authenticate(email: String, password: String): Token
}

internal class MockAuthenticationService : AuthenticationService {
    override suspend fun authenticate(
        email: String,
        password: String,
    ): Token {
        delay(2000) // mock authentication delay
        return Token("userId-${email.hashCode()}-${password.hashCode()}")
    }
}