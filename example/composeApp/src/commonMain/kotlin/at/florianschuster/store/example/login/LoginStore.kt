package at.florianschuster.store.example.login

import androidx.compose.runtime.Immutable
import at.florianschuster.store.Reducer
import at.florianschuster.store.Store
import at.florianschuster.store.effect
import at.florianschuster.store.example.Log
import at.florianschuster.store.example.service.AuthenticationService
import at.florianschuster.store.example.service.InputValidator
import at.florianschuster.store.example.service.TokenRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope

internal class LoginEnvironment(
    val inputValidator: InputValidator,
    val authenticationService: AuthenticationService,
    val tokenRepository: TokenRepository,
)

internal sealed interface LoginAction {
    data class OnEmailEntered(val email: String) : LoginAction
    data class OnPasswordEntered(val password: String) : LoginAction
    data object Authenticate : LoginAction
    data object OnAuthenticationSuccess : LoginAction
    data object OnAuthenticationFailure : LoginAction
}

@Immutable
internal data class LoginState(
    val email: String? = null,
    val emailValid: Boolean? = null,
    val password: String? = null,
    val passwordValid: Boolean? = null,
    val authenticationResult: AuthenticationResult = AuthenticationResult.Uninitialized
) {
    val canLogIn: Boolean = emailValid == true && passwordValid == true
    val isAuthenticated: Boolean = authenticationResult is AuthenticationResult.Success

    sealed interface AuthenticationResult {
        data object Uninitialized : AuthenticationResult
        data object Loading : AuthenticationResult
        data object Success : AuthenticationResult
        data object Failure : AuthenticationResult
    }
}

internal class LoginStore(
    initialState: LoginState,
    environment: LoginEnvironment,
    scope: CoroutineScope,
) : Store<LoginEnvironment, LoginAction, LoginState> by Store(
    initialState = initialState,
    environment = environment,
    effectScope = scope,
    reducer = Reducer { previousState, action ->
        when (action) {
            is LoginAction.OnEmailEntered -> {
                previousState.copy(
                    email = action.email,
                    emailValid = environment.inputValidator.validateEmail(action.email),
                    authenticationResult = LoginState.AuthenticationResult.Uninitialized,
                )
            }

            is LoginAction.OnPasswordEntered -> {
                previousState.copy(
                    password = action.password,
                    passwordValid = environment.inputValidator.validatePassword(action.password),
                    authenticationResult = LoginState.AuthenticationResult.Uninitialized,
                )
            }

            is LoginAction.Authenticate -> {
                if (!previousState.canLogIn) return@Reducer previousState

                // This effect has an Id, so it will not be executed if this authentication
                // effect is already in progress.
                effect(id = LoginAction.Authenticate) {
                    runCatching {
                        environment.authenticationService.authenticate(
                            checkNotNull(previousState.email),
                            checkNotNull(previousState.password),
                        )
                    }.fold(
                        onSuccess = { token ->
                            environment.tokenRepository.store(token)
                            dispatch(LoginAction.OnAuthenticationSuccess)
                        },
                        onFailure = { error ->
                            Log.e(error)
                            if (error !is CancellationException) {
                                dispatch(LoginAction.OnAuthenticationFailure)
                            }
                        }
                    )
                }
                previousState.copy(authenticationResult = LoginState.AuthenticationResult.Loading)
            }

            is LoginAction.OnAuthenticationSuccess -> {
                previousState.copy(authenticationResult = LoginState.AuthenticationResult.Success)
            }

            is LoginAction.OnAuthenticationFailure -> {
                previousState.copy(authenticationResult = LoginState.AuthenticationResult.Failure)
            }
        }
    }
)
