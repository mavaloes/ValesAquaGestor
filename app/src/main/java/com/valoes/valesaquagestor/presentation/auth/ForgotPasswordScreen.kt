package com.valoes.valesaquagestor.presentation.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.validation.AuthFieldError
import com.valoes.valesaquagestor.presentation.components.AppTextField
import com.valoes.valesaquagestor.presentation.components.FeedbackEffect
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/** Tela "Esqueci minha senha": pede o e-mail e mostra sempre a mesma resposta. */
@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    FeedbackEffect(
        message = state.message?.let { stringResource(it.stringRes()) },
        hostState = snackbarHostState,
        onShown = viewModel::onMessageShown,
    )
    ForgotPasswordContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::onSubmit,
        onBackToLogin = onBackToLogin,
    )
}

@Composable
internal fun ForgotPasswordContent(
    state: ForgotPasswordUiState,
    snackbarHostState: SnackbarHostState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    AuthScaffold(
        title = stringResource(R.string.forgot_title),
        subtitle = if (state.linkSent) null else stringResource(R.string.forgot_subtitle),
        snackbarHostState = snackbarHostState,
        help = HelpTips.ForgotPassword,
    ) {
        if (state.linkSent) {
            Text(
                text = stringResource(R.string.message_reset_link_sent),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            AppTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.field_email),
                placeholder = stringResource(R.string.field_email_example),
                errorText = state.emailError?.let { stringResource(it.stringRes()) },
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                contentType = ContentType.EmailAddress,
                onImeAction = onSubmit,
                enabled = !state.isLoading,
            )
            LargeButton(
                text = stringResource(R.string.forgot_button),
                icon = Icons.AutoMirrored.Filled.Send,
                onClick = onSubmit,
                isLoading = state.isLoading,
            )
        }
        LargeButton(
            text = stringResource(R.string.forgot_back_to_login),
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            onClick = onBackToLogin,
        )
    }
}

@Preview(name = "Esqueci a senha", showBackground = true, fontScale = 2f)
@Composable
private fun ForgotPasswordScreenPreview() {
    AquaGestorTheme {
        ForgotPasswordContent(
            state = ForgotPasswordUiState(email = "maria@", emailError = AuthFieldError.INVALID_EMAIL),
            snackbarHostState = SnackbarHostState(),
            onEmailChange = {},
            onSubmit = {},
            onBackToLogin = {},
        )
    }
}

@Preview(name = "Esqueci a senha, link enviado", showBackground = true, fontScale = 2f)
@Composable
private fun ForgotPasswordSentPreview() {
    AquaGestorTheme {
        ForgotPasswordContent(
            state = ForgotPasswordUiState(email = "maria@exemplo.com", linkSent = true),
            snackbarHostState = SnackbarHostState(),
            onEmailChange = {},
            onSubmit = {},
            onBackToLogin = {},
        )
    }
}
