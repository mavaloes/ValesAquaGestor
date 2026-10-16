package com.valoes.valesaquagestor.domain.repository

import com.valoes.valesaquagestor.domain.model.Session
import kotlinx.coroutines.flow.Flow

/** Motivo de uma falha de autenticação. As mensagens ao usuário ficam na camada de apresentação. */
enum class AuthError {
    /** E-mail ou senha incorretos. Nunca diz qual dos dois está errado. */
    INVALID_CREDENTIALS,

    /** Já existe conta com esse e-mail (só no cadastro). */
    EMAIL_ALREADY_REGISTERED,

    /** Sem internet: cadastro, recuperação de senha e primeiro login dependem do servidor. */
    NO_CONNECTION,

    /** O servidor limitou as tentativas. */
    TOO_MANY_ATTEMPTS,

    /** Qualquer outra falha. */
    UNKNOWN,
}

/** Resultado de uma operação de autenticação. */
sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}

/**
 * Autenticação por e-mail e senha (`specs/11-autenticacao.md`).
 *
 * O app continua 100% offline depois do primeiro login: a sessão fica salva no aparelho e
 * [currentSession] não depende de internet. Só [signIn], [signUp] e [sendPasswordReset] precisam
 * de conexão.
 */
interface AuthRepository {

    /** Sessão atual: `null` sem login. Emite o valor salvo no aparelho, mesmo sem internet. */
    val currentSession: Flow<Session?>

    /** Entra com e-mail e senha. A senha não é guardada no aparelho. */
    suspend fun signIn(email: String, password: String): AuthResult

    /** Cria a conta. Quem se cadastra vira ADMIN da própria fazenda e já fica com a sessão aberta. */
    suspend fun signUp(nome: String, email: String, password: String): AuthResult

    /**
     * Pede o link de nova senha. Devolve sucesso exista o e-mail ou não (a resposta é sempre a mesma,
     * para não revelar quem tem conta). Só falha por falta de conexão ou erro inesperado.
     */
    suspend fun sendPasswordReset(email: String): AuthResult

    /** Encerra a sessão neste aparelho. Os registros locais não são apagados. */
    suspend fun signOut()
}
