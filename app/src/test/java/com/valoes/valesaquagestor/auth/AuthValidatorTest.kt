package com.valoes.valesaquagestor.auth

import com.valoes.valesaquagestor.domain.validation.AuthFieldError
import com.valoes.valesaquagestor.domain.validation.AuthValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun `e-mails validos`() {
        listOf("nome@exemplo.com", "maria.silva@fazenda.com.br", "  teste@teste.com  ", "a+b@x.io").forEach {
            assertNull(it, AuthValidator.validateEmail(it))
        }
    }

    @Test
    fun `e-mails invalidos`() {
        listOf("", "   ", "maria", "maria@", "@exemplo.com", "maria@exemplo", "maria@exemplo.", "ma ria@exemplo.com", "a@@b.com")
            .forEach { assertEquals(it, AuthFieldError.INVALID_EMAIL, AuthValidator.validateEmail(it)) }
    }

    @Test
    fun `senha valida tem 8 caracteres, letra e numero`() {
        assertNull(AuthValidator.validateNewPassword("Teste1234"))
        assertNull(AuthValidator.validateNewPassword("abcdefg1"))
        assertNull(AuthValidator.validateNewPassword("12345678a"))
    }

    @Test
    fun `senha fraca e recusada`() {
        listOf("", "abc123", "abcdefgh", "12345678", "abcdefg").forEach {
            assertEquals(it, AuthFieldError.WEAK_PASSWORD, AuthValidator.validateNewPassword(it))
        }
    }

    @Test
    fun `senha com 7 caracteres falha e com 8 passa`() {
        assertEquals(AuthFieldError.WEAK_PASSWORD, AuthValidator.validateNewPassword("abcde12"))
        assertNull(AuthValidator.validateNewPassword("abcde123"))
    }

    @Test
    fun `regras da senha mostram o que falta`() {
        val empty = AuthValidator.passwordRules("")
        assertFalse(empty.hasMinLength || empty.hasLetter || empty.hasDigit)

        val partial = AuthValidator.passwordRules("abc")
        assertTrue(partial.hasLetter)
        assertFalse(partial.hasDigit)
        assertFalse(partial.hasMinLength)
        assertFalse(partial.isValid)

        assertTrue(AuthValidator.passwordRules("abcdefg1").isValid)
    }

    @Test
    fun `confirmacao precisa ser igual`() {
        assertNull(AuthValidator.validateConfirmation("Teste1234", "Teste1234"))
        assertEquals(AuthFieldError.PASSWORDS_DIFFER, AuthValidator.validateConfirmation("Teste1234", "teste1234"))
        assertEquals(AuthFieldError.PASSWORDS_DIFFER, AuthValidator.validateConfirmation("Teste1234", ""))
    }

    @Test
    fun `nome precisa estar preenchido`() {
        assertNull(AuthValidator.validateName("Maria"))
        assertEquals(AuthFieldError.EMPTY_NAME, AuthValidator.validateName("   "))
    }

    @Test
    fun `senha do login so exige estar preenchida`() {
        assertNull(AuthValidator.validateLoginPassword("a"))
        assertEquals(AuthFieldError.EMPTY_PASSWORD, AuthValidator.validateLoginPassword(""))
    }
}
