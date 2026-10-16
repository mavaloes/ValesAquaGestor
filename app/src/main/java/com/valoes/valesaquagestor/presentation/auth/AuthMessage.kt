package com.valoes.valesaquagestor.presentation.auth

import androidx.annotation.StringRes
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.repository.AuthError
import com.valoes.valesaquagestor.domain.validation.AuthFieldError

/**
 * Mensagem de feedback das telas de autenticação (aparece no Snackbar). O ViewModel guarda o
 * tipo; a tela converte em texto com [stringRes]. Assim os textos ficam só em `strings.xml`.
 *
 * Os erros de credencial e de "esqueci a senha" são genéricos: não revelam se o e-mail existe.
 */
enum class AuthMessage {
    INVALID_CREDENTIALS,
    EMAIL_TAKEN,
    NO_CONNECTION_FIRST_LOGIN,
    NO_CONNECTION,
    TOO_MANY_ATTEMPTS,
    UNKNOWN_ERROR,
    RESET_LINK_SENT,
}

/** Converte a falha do repositório na mensagem certa. No login, a falta de internet tem texto próprio. */
internal fun AuthError.toMessage(noConnection: AuthMessage = AuthMessage.NO_CONNECTION): AuthMessage = when (this) {
    AuthError.INVALID_CREDENTIALS -> AuthMessage.INVALID_CREDENTIALS
    AuthError.EMAIL_ALREADY_REGISTERED -> AuthMessage.EMAIL_TAKEN
    AuthError.NO_CONNECTION -> noConnection
    AuthError.TOO_MANY_ATTEMPTS -> AuthMessage.TOO_MANY_ATTEMPTS
    AuthError.UNKNOWN -> AuthMessage.UNKNOWN_ERROR
}

@StringRes
fun AuthMessage.stringRes(): Int = when (this) {
    AuthMessage.INVALID_CREDENTIALS -> R.string.message_invalid_credentials
    AuthMessage.EMAIL_TAKEN -> R.string.message_email_taken
    AuthMessage.NO_CONNECTION_FIRST_LOGIN -> R.string.message_no_connection_first_login
    AuthMessage.NO_CONNECTION -> R.string.message_no_connection
    AuthMessage.TOO_MANY_ATTEMPTS -> R.string.message_too_many_attempts
    AuthMessage.UNKNOWN_ERROR -> R.string.message_unknown_error
    AuthMessage.RESET_LINK_SENT -> R.string.message_reset_link_sent
}

@StringRes
fun AuthFieldError.stringRes(): Int = when (this) {
    AuthFieldError.INVALID_EMAIL -> R.string.error_invalid_email
    AuthFieldError.WEAK_PASSWORD -> R.string.error_weak_password
    AuthFieldError.PASSWORDS_DIFFER -> R.string.error_passwords_differ
    AuthFieldError.EMPTY_PASSWORD -> R.string.error_empty_password
    AuthFieldError.EMPTY_NAME -> R.string.error_empty_name
}
