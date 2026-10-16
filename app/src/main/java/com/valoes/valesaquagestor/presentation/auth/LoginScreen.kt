package com.valoes.valesaquagestor.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.presentation.components.AppTextField
import com.valoes.valesaquagestor.presentation.components.FeedbackEffect
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.LinkButton
import com.valoes.valesaquagestor.presentation.components.PasswordTextField
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.domain.validation.AuthFieldError

/** Tela de login (e-mail e senha). A navegação para a Home acontece quando a sessão passa a existir. */
@Composable
fun LoginScreen(
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    FeedbackEffect(
        message = state.message?.let { stringResource(it.stringRes()) },
        hostState = snackbarHostState,
        onShown = viewModel::onMessageShown,
    )
    LoginContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::onSubmit,
        onForgotPassword = onForgotPassword,
        onSignUp = onSignUp,
    )
}

@Composable
internal fun LoginContent(
    state: LoginUiState,
    snackbarHostState: SnackbarHostState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
) {
    AuthScaffold(
        title = null,
        subtitle = stringResource(R.string.login_subtitle),
        snackbarHostState = snackbarHostState,
        centerVertically = true,
        logo = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                LogoPlaceholder()
            }
        },
    ) {
        AppTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.field_email),
            placeholder = stringResource(R.string.field_email_example),
            errorText = state.emailError?.let { stringResource(it.stringRes()) },
            keyboardType = KeyboardType.Email,
            contentType = ContentType.EmailAddress,
            enabled = !state.isLoading,
        )
        PasswordTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.field_password),
            errorText = state.passwordError?.let { stringResource(it.stringRes()) },
            imeAction = ImeAction.Done,
            onImeAction = onSubmit,
            enabled = !state.isLoading,
        )
        LargeButton(
            text = stringResource(R.string.login_button),
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = onSubmit,
            isLoading = state.isLoading,
        )
        LinkButton(text = stringResource(R.string.login_forgot_password), onClick = onForgotPassword)
        LinkButton(text = stringResource(R.string.login_create_account), onClick = onSignUp)
        Text(
            text = stringResource(R.string.login_first_access_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(name = "Login", showBackground = true, fontScale = 2f)
@Composable
private fun LoginScreenPreview() {
    AquaGestorTheme {
        LoginContent(
            state = LoginUiState(email = "maria@exemplo.com"),
            snackbarHostState = SnackbarHostState(),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onForgotPassword = {},
            onSignUp = {},
        )
    }
}

@Preview(name = "Login com erros", showBackground = true, fontScale = 2f)
@Composable
private fun LoginScreenErrorPreview() {
    AquaGestorTheme {
        LoginContent(
            state = LoginUiState(
                email = "maria@",
                emailError = AuthFieldError.INVALID_EMAIL,
                passwordError = AuthFieldError.EMPTY_PASSWORD,
            ),
            snackbarHostState = SnackbarHostState(),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onForgotPassword = {},
            onSignUp = {},
        )
    }
}
