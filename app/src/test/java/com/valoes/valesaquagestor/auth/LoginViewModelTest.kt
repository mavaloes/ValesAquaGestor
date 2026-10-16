package com.valoes.valesaquagestor.auth

import com.valoes.valesaquagestor.domain.repository.AuthError
import com.valoes.valesaquagestor.domain.repository.AuthResult
import com.valoes.valesaquagestor.domain.validation.AuthFieldError
import com.valoes.valesaquagestor.presentation.auth.AuthMessage
import com.valoes.valesaquagestor.presentation.auth.LoginViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = ScriptedAuthRepository()
    private val viewModel = LoginViewModel(repository)

    private fun fill(email: String = "teste@teste.com", password: String = "Teste1234") {
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
    }

    @Test
    fun `e-mail invalido mostra erro e nao chama o servidor`() {
        fill(email = "teste@")

        viewModel.onSubmit()

        assertEquals(AuthFieldError.INVALID_EMAIL, viewModel.state.value.emailError)
        assertTrue(repository.signInCalls.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `senha vazia mostra erro e nao chama o servidor`() {
        fill(password = "")

        viewModel.onSubmit()

        assertEquals(AuthFieldError.EMPTY_PASSWORD, viewModel.state.value.passwordError)
        assertTrue(repository.signInCalls.isEmpty())
    }

    @Test
    fun `digitar apaga o erro do campo`() {
        fill(email = "x")
        viewModel.onSubmit()
        assertEquals(AuthFieldError.INVALID_EMAIL, viewModel.state.value.emailError)

        viewModel.onEmailChange("teste@teste.com")

        assertNull(viewModel.state.value.emailError)
    }

    @Test
    fun `envia o e-mail sem espacos e apaga a senha do estado`() {
        fill(email = "  teste@teste.com ", password = "Teste1234")

        viewModel.onSubmit()

        assertEquals(listOf(ScriptedAuthRepository.SignInCall("teste@teste.com", "Teste1234")), repository.signInCalls)
        assertEquals("a senha não pode ficar no UiState depois do envio", "", viewModel.state.value.password)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `credencial errada mostra a mensagem generica e a senha continua apagada`() {
        repository.result = AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        fill()

        viewModel.onSubmit()

        assertEquals(AuthMessage.INVALID_CREDENTIALS, viewModel.state.value.message)
        assertEquals("", viewModel.state.value.password)
        assertEquals("teste@teste.com", viewModel.state.value.email)
    }

    @Test
    fun `sem internet no login usa a mensagem do primeiro acesso`() {
        repository.result = AuthResult.Failure(AuthError.NO_CONNECTION)
        fill()

        viewModel.onSubmit()

        assertEquals(AuthMessage.NO_CONNECTION_FIRST_LOGIN, viewModel.state.value.message)
    }

    @Test
    fun `muitas tentativas e erro desconhecido tem mensagem propria`() {
        repository.result = AuthResult.Failure(AuthError.TOO_MANY_ATTEMPTS)
        fill()
        viewModel.onSubmit()
        assertEquals(AuthMessage.TOO_MANY_ATTEMPTS, viewModel.state.value.message)

        viewModel.onMessageShown()
        repository.result = AuthResult.Failure(AuthError.UNKNOWN)
        fill()
        viewModel.onSubmit()
        assertEquals(AuthMessage.UNKNOWN_ERROR, viewModel.state.value.message)
    }

    @Test
    fun `mensagem some depois de mostrada`() {
        repository.result = AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        fill()
        viewModel.onSubmit()

        viewModel.onMessageShown()

        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `toque duplo envia so uma vez`() {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        fill()

        viewModel.onSubmit()
        assertTrue(viewModel.state.value.isLoading)
        viewModel.onSubmit()
        viewModel.onSubmit()
        assertEquals(1, repository.signInCalls.size)

        gate.complete(Unit)
        assertFalse(viewModel.state.value.isLoading)
    }
}
