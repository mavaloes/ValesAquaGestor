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
 * Estado da tela de login. A [password] existe só enquanto o usuário digita: no instante do envio
 * ela é copiada para a chamada e **apagada do estado** (`specs/11`: nenhuma senha em `UiState` depois de enviada).
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: AuthFieldError? = null,
    val passwordError: AuthFieldError? = null,
    val isLoading: Boolean = false,
    val message: AuthMessage? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(email: String) = _state.update { it.copy(email = email, emailError = null) }

    fun onPasswordChange(password: String) = _state.update { it.copy(password = password, passwordError = null) }

    fun onMessageShown() = _state.update { it.copy(message = null) }

    /** Valida e entra. Ignora o toque enquanto outro envio está em andamento (evita toque duplo). */
    fun onSubmit() {
        val current = _state.value
        if (current.isLoading) return

        val emailError = AuthValidator.validateEmail(current.email)
        val passwordError = AuthValidator.validateLoginPassword(current.password)
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        val email = current.email.trim()
        val password = current.password
        _state.update { it.copy(password = "", isLoading = true, emailError = null, passwordError = null) }
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            _state.update {
                it.copy(
                    isLoading = false,
                    // Sucesso: a tela inicial muda sozinha porque a sessão passa a existir.
                    message = (result as? AuthResult.Failure)?.error?.toMessage(AuthMessage.NO_CONNECTION_FIRST_LOGIN),
                )
            }
        }
    }
}
