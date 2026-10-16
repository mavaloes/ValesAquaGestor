package com.valoes.valesaquagestor.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.AuthResult
import com.valoes.valesaquagestor.domain.validation.AuthFieldError
import com.valoes.valesaquagestor.domain.validation.AuthValidator
import com.valoes.valesaquagestor.domain.validation.PasswordRules
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado da tela de cadastro. As senhas existem só enquanto o usuário digita e são **apagadas** no
 * envio. [passwordRules] mostra o que a senha já atende, enquanto o usuário digita.
 */
data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: AuthFieldError? = null,
    val emailError: AuthFieldError? = null,
    val passwordError: AuthFieldError? = null,
    val confirmPasswordError: AuthFieldError? = null,
    val isLoading: Boolean = false,
    val message: AuthMessage? = null,
) {
    val passwordRules: PasswordRules get() = AuthValidator.passwordRules(password)

    /** A confirmação já foi digitada e é igual à senha. */
    val passwordsMatch: Boolean get() = confirmPassword.isNotEmpty() && confirmPassword == password
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    fun onNameChange(name: String) = _state.update { it.copy(name = name, nameError = null) }

    fun onEmailChange(email: String) = _state.update { it.copy(email = email, emailError = null) }

    fun onPasswordChange(password: String) = _state.update { it.copy(password = password, passwordError = null) }

    fun onConfirmPasswordChange(confirmPassword: String) =
        _state.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }

    fun onMessageShown() = _state.update { it.copy(message = null) }

    /** Valida tudo e cria a conta. Quem se cadastra vira ADMIN da própria fazenda (feito no repositório). */
    fun onSubmit() {
        val current = _state.value
        if (current.isLoading) return

        val nameError = AuthValidator.validateName(current.name)
        val emailError = AuthValidator.validateEmail(current.email)
        val passwordError = AuthValidator.validateNewPassword(current.password)
        val confirmError = AuthValidator.validateConfirmation(current.password, current.confirmPassword)
        if (listOf(nameError, emailError, passwordError, confirmError).any { it != null }) {
            _state.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError,
                )
            }
            return
        }

        val name = current.name
        val email = current.email.trim()
        val password = current.password
        _state.update {
            it.copy(
                password = "",
                confirmPassword = "",
                isLoading = true,
                nameError = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
            )
        }
        viewModelScope.launch {
            val result = authRepository.signUp(name, email, password)
            _state.update {
                it.copy(
                    isLoading = false,
                    // Sucesso: a sessão passa a existir e a navegação leva o usuário para dentro do app.
                    message = (result as? AuthResult.Failure)?.error?.toMessage(),
                )
            }
        }
    }
}
