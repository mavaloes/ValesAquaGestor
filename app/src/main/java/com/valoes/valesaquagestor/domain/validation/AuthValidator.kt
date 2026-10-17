package com.valoes.valesaquagestor.domain.validation

/** Erro de validação de um campo das telas de autenticação. A tela converte em texto de `strings.xml`. */
enum class AuthFieldError {
    INVALID_EMAIL,
    WEAK_PASSWORD,
    PASSWORDS_DIFFER,
    EMPTY_PASSWORD,
    EMPTY_NAME,
}

/**
 * O que a senha nova já atende. A tela mostra as três regras **antes** de digitar e marca o que
 * falta enquanto digita (`specs/11-autenticacao.md`).
 */
data class PasswordRules(
    val hasMinLength: Boolean,
    val hasLetter: Boolean,
    val hasDigit: Boolean,
) {
    val isValid: Boolean get() = hasMinLength && hasLetter && hasDigit
}

/** Validação dos campos de autenticação, em Kotlin puro (o ViewModel chama, a tela só mostra). */
object AuthValidator {

    /** @see specs/11-autenticacao.md — senha com no mínimo 8 caracteres, com letra e número. */
    const val MIN_PASSWORD_LENGTH = 8

    private val emailPattern = Regex("""^[^@\s]+@[^@\s]+\.[^@\s.]{2,}$""")

    /** E-mail com formato válido (já sem espaços nas pontas). Devolve `null` se estiver certo. */
    fun validateEmail(email: String): AuthFieldError? =
        if (emailPattern.matches(email.trim())) null else AuthFieldError.INVALID_EMAIL

    /** Avalia a senha nova regra por regra. */
    fun passwordRules(password: String) = PasswordRules(
        hasMinLength = password.length >= MIN_PASSWORD_LENGTH,
        hasLetter = password.any { it.isLetter() },
        hasDigit = password.any { it.isDigit() },
    )

    /** Senha nova (cadastro): precisa atender às três regras. */
    fun validateNewPassword(password: String): AuthFieldError? =
        if (passwordRules(password).isValid) null else AuthFieldError.WEAK_PASSWORD

    /** Confirmação igual à senha. */
    fun validateConfirmation(password: String, confirmation: String): AuthFieldError? =
        if (password == confirmation) null else AuthFieldError.PASSWORDS_DIFFER

    /** Nome preenchido. */
    fun validateName(name: String): AuthFieldError? =
        if (name.isBlank()) AuthFieldError.EMPTY_NAME else null

    /**
     * Senha do login: só exige que esteja preenchida. As regras de senha forte valem no cadastro;
     * no login, mostrar "senha fraca" daria pistas sobre a senha.
     */
    fun validateLoginPassword(password: String): AuthFieldError? =
        if (password.isEmpty()) AuthFieldError.EMPTY_PASSWORD else null
}
