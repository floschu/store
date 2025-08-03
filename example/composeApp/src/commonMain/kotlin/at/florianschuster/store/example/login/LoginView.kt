package at.florianschuster.store.example.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.florianschuster.store.example.AppTheme
import example.composeapp.generated.resources.Res
import example.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun LoginView(
    state: LoginState,
    dispatch: (LoginAction) -> Unit = {},
    onGoToSearch: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .safeContentPadding()
            .padding(AppTheme.dimensions.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.size(200.dp),
            painter = painterResource(Res.drawable.logo),
            contentDescription = null,
        )
        val focusManager = LocalFocusManager.current
        OutlinedTextField(
            modifier = Modifier.semantics { contentType = ContentType.EmailAddress },
            value = state.email ?: "",
            onValueChange = { dispatch(LoginAction.OnEmailEntered(it)) },
            label = { Text("Email") },
            supportingText = {
                if (state.emailValid == false) {
                    Text("Please enter a valid email address.")
                }
            },
            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
            isError = state.emailValid == false,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) }
            ),
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.small))
        OutlinedTextField(
            modifier = Modifier.semantics { contentType = ContentType.Password },
            value = state.password ?: "",
            onValueChange = { dispatch(LoginAction.OnPasswordEntered(it)) },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (state.canLogIn) {
                        focusManager.clearFocus()
                        dispatch(LoginAction.Authenticate)
                    }
                }
            ),
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.large))
        if (state.authenticationResult is LoginState.AuthenticationResult.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { dispatch(LoginAction.Authenticate) },
                enabled = state.canLogIn,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Login")
                    Icon(Icons.AutoMirrored.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
        if (state.authenticationResult is LoginState.AuthenticationResult.Failure) {
            Spacer(modifier = Modifier.height(AppTheme.dimensions.medium))
            Text(
                text = "Login failed. Please try again.",
                color = AppTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
@Preview
private fun Preview() {
    AppTheme {
        LoginView(state = LoginState())
    }
}
