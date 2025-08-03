<p align="center"><img alt="flow" width="350" src=".media/logo.webp"></p>

<p align=center>
    <a href="https://search.maven.org/artifact/at.florianschuster.store/store"><img alt="version" src="https://img.shields.io/maven-central/v/at.florianschuster.store/store?label=version&logoColor=1AA2D4" /></a>
</p>

<p align=center>
    <a href="https://github.com/floschu/store/actions"><img alt="build" src="https://github.com/floschu/store/workflows/build/badge.svg" /></a>
    <a href="https://github.com/floschu/store/"><img alt="last commit" src="https://img.shields.io/github/last-commit/floschu/store?logoColor=B75EA4" /></a>
    <a href="LICENSE"><img alt="license" src="https://img.shields.io/badge/license-Apache%202.0-blue.svg?color=7b6fe2" /></a>
</p>

## installation

``` groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation("at.florianschuster.store:store:$version")
}
```

see [changelog](https://github.com/floschu/store/blob/main/CHANGELOG.md) for versions

## usage

Go to [store](https://github.com/floschu/store/blob/main/src/commonMain/kotlin/at/florianschuster/store/store.kt) as entry point for more information.

```kotlin
class LoginEnvironment(
    val authenticationService: AuthenticationService,
    val tokenRepository: TokenRepository,
)

sealed interface LoginAction {
    data class EmailChanged(val value: String): LoginAction
    data class PasswordChanged(val value: String): LoginAction
    data object Login: LoginAction
    data class LoginResult(val result: Result<Token>): LoginAction
}

data class LoginState(
    val emailAddress: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val displayLoginError: Boolean = false,
) {
    val isInputValid = emailAddress.isNotEmpty() && password.isNotEmpty()
}

val LoginReducer = Reducer<LoginEnvironment, LoginAction, LoginState> { previousState, action ->
    when(action) {
        is LoginAction.EmailChanged -> previousState.copy(emailAddress = action.value)
        is LoginAction.PasswordChanged -> previousState.copy(password = action.value)
        is LoginAction.Login -> {
            if(!previousState.isInputValid) return@Reducer previousState
            cancelEffect(id = LoginAction.Login) // if an authentication is already in progress, we cancel it
            effect(id = LoginAction.Login) {
                val result = environment.authenticationService.authenticate(
                    previousState.emailAddress,
                    previousState.password,
                )
                dispatch(LoginAction.LoginResult(result))
            }
            previousState.copy(
                loading = true,
                displayLoginError = false
            )
        }
        is LoginAction.LoginResult -> {
            action.result.onSuccess { token ->
                effect(id = LoginAction.LoginResult::class) {
                    environment.tokenRepository.store(token)
                }
            }
            previousState.copy(
                loading = false,
                displayLoginError = action.result.isFailure
            )
        }
    }
}

class LoginStore(
    effectScope: CoroutineScope,
    environment: LoginEnvironment,
): Store<LoginEnvironment, LoginAction, LoginState> by Store(
    initialState = LoginState(),
    environment = environment,
    effectScope = effectScope,
    reducer = LoginReducer,
)
```

A more complex [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) example can be found in the [example directory](https://github.com/floschu/store/blob/main/example).

## author

visit my [website](https://florianschuster.at/).
