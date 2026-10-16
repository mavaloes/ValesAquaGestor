package com.valoes.valesaquagestor.data.supabase

import com.valoes.valesaquagestor.data.session.SessionStore
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.repository.AuthError
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.AuthResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Autenticação de verdade no Supabase (troca o `FakeAuthRepository`, Fase 09). Mantém as regras de
 * `specs/11-autenticacao.md`:
 *
 * - Cadastro, login e recuperação de senha precisam de internet; abrir o app e sair, não.
 * - Erro de credencial e recuperação de senha são sempre genéricos (não revelam se o e-mail existe).
 * - A senha só vai ao servidor; no aparelho ficam a sessão e as chaves, criptografadas.
 * - Quem se cadastra vira ADMIN da própria fazenda, a menos que tenha sido convidado (quem decide é
 *   o banco, no gatilho de novo usuário em `backend/supabase/schema.sql`).
 */
@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val api: SupabaseAuthApi,
    private val tokens: TokenStore,
    private val sessionStore: SessionStore,
) : AuthRepository {

    override val currentSession: Flow<Session?> = sessionStore.session

    override suspend fun signIn(email: String, password: String): AuthResult =
        finishLogin(api.signIn(normalize(email), password))

    override suspend fun signUp(nome: String, email: String, password: String): AuthResult =
        finishLogin(api.signUp(nome.trim(), normalize(email), password))

    override suspend fun sendPasswordReset(email: String): AuthResult = when (api.recover(normalize(email))) {
        // Mesma resposta exista a conta ou não.
        SupabaseAuthApi.RecoverResult.SENT -> AuthResult.Success
        SupabaseAuthApi.RecoverResult.TOO_MANY -> AuthResult.Failure(AuthError.TOO_MANY_ATTEMPTS)
        SupabaseAuthApi.RecoverResult.OFFLINE -> AuthResult.Failure(AuthError.NO_CONNECTION)
        SupabaseAuthApi.RecoverResult.UNEXPECTED -> AuthResult.Failure(AuthError.UNKNOWN)
    }

    /** Encerra a sessão neste aparelho, sem internet. Os registros locais não são apagados. */
    override suspend fun signOut() {
        tokens.clear()
        sessionStore.clear()
    }

    /** Depois de entrar, busca o perfil (papel e `uuid` na fazenda) e só então abre a sessão. */
    private suspend fun finishLogin(result: TokenResult): AuthResult {
        val ok = when (result) {
            is TokenResult.Ok -> result
            TokenResult.InvalidCredentials -> return AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
            TokenResult.EmailTaken -> return AuthResult.Failure(AuthError.EMAIL_ALREADY_REGISTERED)
            TokenResult.TooManyAttempts -> return AuthResult.Failure(AuthError.TOO_MANY_ATTEMPTS)
            TokenResult.Offline -> return AuthResult.Failure(AuthError.NO_CONNECTION)
            TokenResult.Unexpected -> return AuthResult.Failure(AuthError.UNKNOWN)
        }
        return when (val profile = api.fetchProfile(ok.tokens.accessToken, ok.authUserId)) {
            is ProfileResult.Found -> {
                tokens.save(ok.tokens)
                sessionStore.save(profile.session)
                AuthResult.Success
            }
            // Conta desativada ou sem fazenda: mesmo erro de senha errada.
            ProfileResult.NoAccess -> AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
            ProfileResult.Offline -> AuthResult.Failure(AuthError.NO_CONNECTION)
            ProfileResult.Unexpected -> AuthResult.Failure(AuthError.UNKNOWN)
        }
    }

    private fun normalize(email: String) = email.trim().lowercase()
}
