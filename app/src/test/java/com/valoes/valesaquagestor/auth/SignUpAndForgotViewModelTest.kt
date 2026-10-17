package com.valoes.valesaquagestor.auth

import com.valoes.valesaquagestor.domain.repository.AuthError
import com.valoes.valesaquagestor.domain.repository.AuthResult
import com.valoes.valesaquagestor.domain.validation.AuthFieldError
import com.valoes.valesaquagestor.presentation.auth.AuthMessage
import com.valoes.valesaquagestor.presentation.auth.ForgotPasswordViewModel
import com.valoes.valesaquagestor.presentation.auth.SignUpViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignUpAndForgotViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = ScriptedAuthRepository()

    private fun signUpViewModel() = SignUpViewModel(repository)

    private fun SignUpViewModel.fill(
        name: String = "Maria Silva",
        email: String = "maria@exemplo.com",
        password: String = "Maria1234",
        confirm: String = "Maria1234",
    ) {
        onNameChange(name)
        onEmailChange(email)
        onPasswordChange(password)
        onConfirmPasswordChange(confirm)
    }

    @Test
    fun `cadastro com tudo vazio mostra os quatro erros`() {
        val viewModel = signUpViewModel()

        viewModel.onSubmit()

        val state = viewModel.state.value
        assertEquals(AuthFieldError.EMPTY_NAME, state.nameError)
        assertEquals(AuthFieldError.INVALID_EMAIL, state.emailError)
        assertEquals(AuthFieldError.WEAK_PASSWORD, state.passwordError)
        assertTrue(repository.signUpCalls.isEmpty())
    }

    @Test
    fun `senhas diferentes mostram erro na confirmacao`() {
        val viewModel = signUpViewModel()
        viewModel.fill(confirm = "Outra1234")

        viewModel.onSubmit()

        assertEquals(AuthFieldError.PASSWORDS_DIFFER, viewModel.state.value.confirmPasswordError)
        assertNull(viewModel.state.value.passwordError)
        assertTrue(repository.signUpCalls.isEmpty())
    }

    @Test
    fun `senha fraca e recusada`() {
        val viewModel = signUpViewModel()
        viewModel.fill(password = "abc123", confirm = "abc123")

        viewModel.onSubmit()

        assertEquals(AuthFieldError.WEAK_PASSWORD, viewModel.state.value.passwordError)
    }

    @Test
    fun `regras da senha acompanham a digitacao`() {
        val viewModel = signUpViewModel()
        assertFalse(viewModel.state.value.passwordRules.hasLetter)

        viewModel.onPasswordChange("abc")
        assertTrue(viewModel.state.value.passwordRules.hasLetter)
        assertFalse(viewModel.state.value.passwordRules.hasDigit)

        viewModel.onPasswordChange("abcdefg1")
        assertTrue(viewModel.state.value.passwordRules.isValid)
    }

    @Test
    fun `cadastro valido envia os dados e apaga as senhas do estado`() {
        val viewModel = signUpViewModel()
        viewModel.fill(name = "Maria Silva", email = " maria@exemplo.com ")

        viewModel.onSubmit()

        assertEquals(
            listOf(ScriptedAuthRepository.SignUpCall("Maria Silva", "maria@exemplo.com", "Maria1234")),
            repository.signUpCalls,
        )
        assertEquals("", viewModel.state.value.password)
        assertEquals("", viewModel.state.value.confirmPassword)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `e-mail ja cadastrado mostra a mensagem`() {
        repository.result = AuthResult.Failure(AuthError.EMAIL_ALREADY_REGISTERED)
        val viewModel = signUpViewModel()
        viewModel.fill()

        viewModel.onSubmit()

        assertEquals(AuthMessage.EMAIL_TAKEN, viewModel.state.value.message)
    }

    @Test
    fun `sem internet no cadastro usa a mensagem geral`() {
        repository.result = AuthResult.Failure(AuthError.NO_CONNECTION)
        val viewModel = signUpViewModel()
        viewModel.fill()

        viewModel.onSubmit()

        assertEquals(AuthMessage.NO_CONNECTION, viewModel.state.value.message)
    }

    @Test
    fun `esqueci a senha com e-mail invalido nao chama o servidor`() {
        val viewModel = ForgotPasswordViewModel(repository)
        viewModel.onEmailChange("maria")

        viewModel.onSubmit()

        assertEquals(AuthFieldError.INVALID_EMAIL, viewModel.state.value.emailError)
        assertTrue(repository.resetCalls.isEmpty())
        assertFalse(viewModel.state.value.linkSent)
    }

    @Test
    fun `esqueci a senha mostra sempre a mesma resposta`() {
        val viewModel = ForgotPasswordViewModel(repository)
        viewModel.onEmailChange(" maria@exemplo.com ")

        viewModel.onSubmit()

        assertEquals(listOf("maria@exemplo.com"), repository.resetCalls)
        assertTrue(viewModel.state.value.linkSent)
        assertEquals(AuthMessage.RESET_LINK_SENT, viewModel.state.value.message)
    }

    @Test
    fun `esqueci a senha sem internet mostra erro de conexao e nao confirma envio`() {
        repository.result = AuthResult.Failure(AuthError.NO_CONNECTION)
        val viewModel = ForgotPasswordViewModel(repository)
        viewModel.onEmailChange("maria@exemplo.com")

        viewModel.onSubmit()

        assertFalse(viewModel.state.value.linkSent)
        assertEquals(AuthMessage.NO_CONNECTION, viewModel.state.value.message)
    }
}
