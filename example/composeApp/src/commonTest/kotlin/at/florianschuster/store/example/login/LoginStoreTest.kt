package at.florianschuster.store.example.login

import at.florianschuster.store.example.login.LoginStoreTest.Setup.Companion.TestToken
import at.florianschuster.store.example.service.AuthenticationService
import at.florianschuster.store.example.service.InputValidator
import at.florianschuster.store.example.service.Token
import at.florianschuster.store.example.service.TokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginStoreTest {
    private class Setup(scope: CoroutineScope) {
        var givenEmailValid = true
        var givenPasswordValid = true
        val testInputValidator = object : InputValidator {
            override fun validateEmail(email: String) = givenEmailValid
            override fun validatePassword(password: String) = givenPasswordValid
        }

        var givenAuthenticationSucceed = true
        val testAuthenticationService = object : AuthenticationService {
            override suspend fun authenticate(email: String, password: String): Token {
                if (!givenAuthenticationSucceed) error("Authentication failed")
                return TestToken
            }
        }

        var storedToken: Token? = null
        val testTokenRepository = object : TokenRepository {
            override suspend fun store(token: Token) {
                storedToken = token
            }
        }

        val sut = LoginStore(
            initialState = LoginState(),
            environment = LoginEnvironment(
                inputValidator = testInputValidator,
                authenticationService = testAuthenticationService,
                tokenRepository = testTokenRepository
            ),
            scope = scope
        )

        val currentState: LoginState
            get() = sut.state.value

        companion object {
            val TestToken = Token("mock")
        }
    }

    @Test
    fun `when OnEmailEntered dispatched - then state updates and validation runs`() = runTest {
        with(Setup(backgroundScope)) {
            givenEmailValid = true
            val input = "user@test.com"

            sut.dispatch(LoginAction.OnEmailEntered(input))

            assertEquals(input, currentState.email)
            assertTrue(currentState.emailValid!!)
        }
    }

    @Test
    fun `when OnPasswordEntered dispatched- then state updates and validation runs`() = runTest {
        with(Setup(backgroundScope)) {
            givenPasswordValid = true
            val input = "password"

            sut.dispatch(LoginAction.OnPasswordEntered(input))

            assertEquals(input, currentState.password)
            assertTrue(currentState.passwordValid!!)
        }
    }

    @Test
    fun `given invalid email- when Authenticate dispatched- then authentication does not proceed`() = runTest {
        with(Setup(backgroundScope)) {
            givenEmailValid = false
            givenPasswordValid = true

            sut.dispatch(LoginAction.OnEmailEntered("bademail"))
            assertFalse(currentState.emailValid!!)
            assertFalse(currentState.canLogIn)

            sut.dispatch(LoginAction.OnPasswordEntered("password"))
            assertTrue(currentState.passwordValid!!)
            assertFalse(currentState.canLogIn)

            sut.dispatch(LoginAction.Authenticate)
            runCurrent()

            assertNull(storedToken)
            assertEquals(LoginState.AuthenticationResult.Uninitialized, currentState.authenticationResult)
        }
    }

    @Test
    fun `given invalid password- when Authenticate dispatched- then authentication does not proceed`() = runTest {
        with(Setup(backgroundScope)) {
            givenEmailValid = true
            givenPasswordValid = false

            sut.dispatch(LoginAction.OnEmailEntered("bademail"))
            assertTrue(currentState.emailValid!!)
            assertFalse(currentState.canLogIn)

            sut.dispatch(LoginAction.OnPasswordEntered("password"))
            assertFalse(currentState.passwordValid!!)
            assertFalse(currentState.canLogIn)

            sut.dispatch(LoginAction.Authenticate)
            runCurrent()

            assertNull(storedToken)
            assertEquals(LoginState.AuthenticationResult.Uninitialized, currentState.authenticationResult)
        }
    }

    @Test
    fun `given authentication fails- when Authenticate dispatched- then authenticationResult is Failure`() = runTest {
        with(Setup(backgroundScope)) {
            givenEmailValid = true
            givenPasswordValid = true
            givenAuthenticationSucceed = false

            sut.dispatch(LoginAction.OnEmailEntered("user@test.com"))
            sut.dispatch(LoginAction.OnPasswordEntered("password"))

            sut.dispatch(LoginAction.Authenticate)
            runCurrent()

            assertEquals(LoginState.AuthenticationResult.Failure, currentState.authenticationResult)
            assertNull(storedToken)
        }
    }

    @Test
    fun `given valid credentials- when Authenticate dispatched- then authentication succeeds and token is stored`() =
        runTest {
            with(Setup(backgroundScope)) {
                givenEmailValid = true
                givenPasswordValid = true

                sut.dispatch(LoginAction.OnEmailEntered("user@test.com"))
                assertTrue(currentState.emailValid!!)
                assertFalse(currentState.canLogIn)

                sut.dispatch(LoginAction.OnPasswordEntered("password"))
                assertTrue(currentState.passwordValid!!)
                assertTrue(currentState.canLogIn)

                sut.dispatch(LoginAction.Authenticate)
                runCurrent()

                assertEquals(LoginState.AuthenticationResult.Success, currentState.authenticationResult)
                assertEquals(TestToken, storedToken)
            }
        }
}