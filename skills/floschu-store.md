---
name: floschu-store
description: Implement, debug, and test floschu/store - a unidirectional data flow state management kmp library with coroutines
---

# floschu/store - Kotlin State Management

A skill for working with the [store](https://github.com/floschu/store) library - an opinionated Kotlin Coroutines multiplatform library for unidirectional data flow (UDF) state management.

## Core Concepts

The library follows a Redux/Flux-like pattern:
- **State**: Immutable data held in a `StateFlow<State>`
- **Actions**: Events dispatched to trigger state changes
- **Reducers**: Pure functions that process actions and return new state
- **Effects**: Async side effects (network, database, etc.)
- **Environment**: Dependency container for effects

 ```
                      dispatch(Action)
view ▶──────────────────────┬◀─────────────────────────────┐
                            │                               │
                            │                               │
     ┏━━━━━━━━━━━━━━━━━━━━━━▼━━━━━━━━━━━━━━━━━━━━━┓         │
     ┃ Store                │ actions             ┃         │
     ┃ with Environment     │                     ┃         │
     ┃                ┏━━━━━▼━━━━━┓               ┃  ┏━━━━━━━━━━━━━━┓
     ┃  ┌────────────▶┃  reducer  ┃ ─ ─ ─ ─ ─ ─ ─ - ─▶    effect    ┃
     ┃  │             ┗━━━━━━━━━━━┛  can produce  ┃  ┗━━━━━━━━━━━━━━┛
     ┃  │                   │                     ┃   uses Environment
     ┃  │ previous          │                     ┃
     ┃  │ state             │ new state           ┃
     ┃  │                   │                     ┃
     ┃  │             ┏━━━━━▼━━━━━┓               ┃
     ┃  └─────────────┃   State   ┃               ┃
     ┃                ┗━━━━━━━━━━━┛               ┃
     ┃                      │                     ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━▼━━━━━━━━━━━━━━━━━━━━━┛
                            │
view ◀─────────────────────┘
```

## Implementation Patterns

### 1. Define the Components

```kotlin
// Environment: holds dependencies for side effects
class LoginEnvironment(
    val authService: AuthenticationService,
    val tokenRepo: TokenRepository,
)

// Actions: sealed interface for all possible events
sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object Login : LoginAction
    data class LoginResult(val result: Result<Token>) : LoginAction
}

// State: immutable data class
data class LoginState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: Boolean = false,
) {
    val isInputValid = email.isNotEmpty() && password.isNotEmpty()
}
```

### 2. Create the Reducer

```kotlin
val LoginReducer = Reducer<LoginEnvironment, LoginAction, LoginState> { previousState, action ->
    when (action) {
        is LoginAction.EmailChanged -> previousState.copy(email = action.value)
        is LoginAction.PasswordChanged -> previousState.copy(password = action.value)
        is LoginAction.Login -> {
            if (!previousState.isInputValid) return@Reducer previousState
            // Cancel any in-flight login, then start new one
            cancelEffect(id = LoginAction.Login)
            effect(id = LoginAction.Login) {
                val result = environment.authService.authenticate(
                    previousState.email,
                    previousState.password,
                )
                dispatch(LoginAction.LoginResult(result))
            }
            previousState.copy(loading = true, error = false)
        }
        is LoginAction.LoginResult -> {
            action.result.onSuccess { token ->
                effect { environment.tokenRepo.store(token) }
            }
            previousState.copy(loading = false, error = action.result.isFailure)
        }
    }
}
```

### 3. Create the Store

Use Kotlin's delegation pattern:

```kotlin
class LoginStore(
    effectScope: CoroutineScope,
    environment: LoginEnvironment,
) : Store<LoginEnvironment, LoginAction, LoginState> by Store(
    initialState = LoginState(),
    environment = environment,
    effectScope = effectScope,
    reducer = LoginReducer,
)
```

### 4. Use in UI (Compose)

```kotlin
@Composable
fun LoginScreen(store: LoginStore) {
    val state by store.state.collectAsState()
    
    TextField(
        value = state.email,
        onValueChange = { store.dispatch(LoginAction.EmailChanged(it)) }
    )
    
    Button(
        onClick = { store.dispatch(LoginAction.Login) },
        enabled = state.isInputValid && !state.loading
    ) {
        if (state.loading) CircularProgressIndicator() else Text("Login")
    }
}
```

## Effect Patterns

### Restarting and Debouncing (e.g., search)

```kotlin
is SearchAction.QueryChanged -> {
    cancelEffect(id = SearchAction.QueryChanged::class)
    effect(id = SearchAction.QueryChanged::class) {
        delay(300.milliseconds)  // Debounce
        val items = environment.searchRepo.search(action.query)
        dispatch(SearchAction.ItemsLoaded(items))
    }
    previousState.copy(loading = true)
}
```

### Initial Effect (runs on store creation)

```kotlin
val NavigationReducer = Reducer<NavEnv, NavAction, NavState>(
    initialEffect = effect {
        environment.tokenRepo.token.collect { token ->
            if (token != null) dispatch(NavAction.GoTo(Route.Home))
        }
    }
) { previousState, action -> /* ... */ }
```

### Accessing Current State in Effects

```kotlin
effect {
    val currentState = state.value  // StateFlow<State>
    val result = environment.api.fetch(currentState.query)
    dispatch(Action.Result(result))
}
```

## Store Composition (Delegation)

Compose multiple stores into a parent:

```kotlin
val appStore = Store(
    initialState = AppState(),
    environment = appEnv,
    effectScope = scope,
    delegates = listOf(
        loginStore.delegate(
            scopeAction = scopeAction(AppAction.Login::action),
            expandState = { state, loginState -> state.copy(login = loginState) }
        ),
        searchStore.delegate(
            scopeAction = scopeAction(AppAction.Search::action),
            expandState = { state, searchState -> state.copy(search = searchState) }
        ),
    )
)
```

## Debugging

Enable logging with `StoreEvents`:

```kotlin
val store = Store(
    initialState = State(),
    environment = env,
    effectScope = scope,
    reducer = reducer,
    events = StoreEvents.Println("MyStore"),  // Logs all events
)
// Output: MyStore > init > (initialState="...", ...)
// Output: MyStore > dispatch > "..."
// Output: MyStore > reduce > ("...", "...") -> "..."
```

## Testing

### Test Reducers Directly

```kotlin
@Test
fun `email changed updates state`() = runTest {
    val store = LoginStore()
    store.dispatch(LoginAction.EmailChanged("test@example.com"))
    assertEquals("test@example.com", store.state.value.email)
}
```

### Test with Turbine (StateFlow testing)

```kotlin
@Test
fun `login flow`() = runTest {
    val store = LoginStore()
    store.state.test {
        assertEquals(LoginState(), awaitItem())  // Initial
        
        store.dispatch(LoginAction.Login)
        assertEquals(true, awaitItem().loading)  // Loading
        
        assertEquals(false, awaitItem().loading)  // Complete
    }
}
```

### Mock Environment for Testing

```kotlin
val testLoginEnvironment = LoginEnvironment(
    authService = object : AuthenticationService {
        override suspend fun authenticate(email: String, password: String) = 
            Result.success(Token("test-token"))
    },
    tokenRepo = FakeTokenRepository(),
)
```

## Common Mistakes to Avoid

1. **Mutating state directly** - Always use `copy()` to create new state
2. **Blocking in reducers** - Reducers must be synchronous; use `effect {}` for async
3. **Forgetting effect IDs** - Use IDs when you need cancellation or deduplication
4. **Not handling all actions** - Use exhaustive `when` to handle all sealed cases
5. **Leaking coroutine scope** - Pass a lifecycle-aware scope (e.g., `viewModelScope`)

## Installation

```groovy
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("at.florianschuster.store:store:$version")
            }
        }
    }
}
```

See the [changelog](https://github.com/floschu/store/blob/main/CHANGELOG.md) for versions.
