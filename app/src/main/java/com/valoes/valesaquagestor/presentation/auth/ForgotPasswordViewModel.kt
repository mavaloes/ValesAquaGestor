package com.valoes.valesaquagestor.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.AuthResult
import com.valoes.valesaquagestor.domain.validation.AuthFieldError
import com.valoes.valesaquagestor.domain.validation.AuthValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado da tela "Esqueci minha senha". Depois do envio, [linkSent] fica `true` e a resposta é
 * sempre a mesma, exista o e-mail ou não.
 */
data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: AuthFieldError? = null,
    val isLoading: Boolean = false,
    val linkSent: Boolean = false,
    val message: AuthMessage? = null,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(email: String) = _state.update { it.copy(email = email, emailError = null) }

    fun onMessageShown() = _state.update { it.copy(message = null) }

    fun onSubmit() {
        val current = _state.value
        if (current.isLoading) return

        val emailError = AuthValidator.validateEmail(current.email)
        if (emailError != null) {
            _state.update { it.copy(emailError = emailError) }
            return
        }

        _state.update { it.copy(isLoading = true, emailError = null) }
        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(current.email.trim())
            _state.update {
                when (result) {
                    AuthResult.Success -> it.copy(isLoading = false, linkSent = true, message = AuthMessage.RESET_LINK_SENT)
                    is AuthResult.Failure -> it.copy(isLoading = false, message = result.error.toMessage())
                }
            }
        }
    }
}
