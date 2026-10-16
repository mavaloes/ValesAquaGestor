package com.valoes.valesaquagestor.presentation.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.SnackbarHostState
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
import com.valoes.valesaquagestor.presentation.components.LinkButton
import com.valoes.valesaquagestor.presentation.components.PasswordTextField
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/** Tela de cadastro. Depois de criar a conta, a sessão passa a existir e a navegação leva à Home. */
@Composable
fun SignUpScreen(
    onBackToLogin: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    FeedbackEffect(
        message = state.message?.let { stringResource(it.stringRes()) },
        hostState = snackbarHostState,
        onShown = viewModel::onMessageShown,
    )
    SignUpContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSubmit = viewModel::onSubmit,
        onBackToLogin = onBackToLogin,
    )
}

@Composable
internal fun SignUpContent(
    state: SignUpUiState,
    snackbarHostState: SnackbarHostState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    AuthScaffold(
        title = stringResource(R.string.signup_title),
        subtitle = stringResource(R.string.signup_subtitle),
        snackbarHostState = snackbarHostState,
        help = HelpTips.SignUp,
    ) {
        AppTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.field_name),
            placeholder = stringResource(R.string.field_name_example),
            errorText = state.nameError?.let { stringResource(it.stringRes()) },
            contentType = ContentType.PersonFullName,
            enabled = !state.isLoading,
        )
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
        PasswordRulesChecklist(rules = state.passwordRules, passwordsMatch = state.passwordsMatch)
        PasswordTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.field_password),
            errorText = state.passwordError?.let { stringResource(it.stringRes()) },
            contentType = ContentType.NewPassword,
            enabled = !state.isLoading,
        )
        PasswordTextField(
            value = state.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = stringResource(R.string.field_confirm_password),
            errorText = state.confirmPasswordError?.let { stringResource(it.stringRes()) },
            contentType = ContentType.NewPassword,
            imeAction = ImeAction.Done,
            onImeAction = onSubmit,
            enabled = !state.isLoading,
        )
        LargeButton(
            text = stringResource(R.string.signup_button),
            icon = Icons.Default.Person,
            onClick = onSubmit,
            isLoading = state.isLoading,
        )
        LinkButton(text = stringResource(R.string.signup_have_account), onClick = onBackToLogin)
    }
}

@Preview(name = "Cadastro", showBackground = true, fontScale = 2f)
@Composable
private fun SignUpScreenPreview() {
    AquaGestorTheme {
        SignUpContent(
            state = SignUpUiState(name = "Maria Silva", email = "maria@exemplo.com", password = "abc123"),
            snackbarHostState = SnackbarHostState(),
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
            onBackToLogin = {},
        )
    }
}

@Preview(name = "Cadastro com erros", showBackground = true, fontScale = 2f)
@Composable
private fun SignUpScreenErrorPreview() {
    AquaGestorTheme {
        SignUpContent(
            state = SignUpUiState(
                email = "maria@",
                nameError = AuthFieldError.EMPTY_NAME,
                emailError = AuthFieldError.INVALID_EMAIL,
                passwordError = AuthFieldError.WEAK_PASSWORD,
                confirmPasswordError = AuthFieldError.PASSWORDS_DIFFER,
            ),
            snackbarHostState = SnackbarHostState(),
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
            onBackToLogin = {},
        )
    }
}
