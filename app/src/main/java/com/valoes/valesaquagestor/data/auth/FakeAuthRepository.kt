package com.valoes.valesaquagestor.data.auth

import com.valoes.valesaquagestor.data.session.SessionStore
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.model.StatusUsuario
import com.valoes.valesaquagestor.domain.repository.AuthError
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.AuthResult
import com.valoes.valesaquagestor.domain.repository.ConnectivityChecker
import com.valoes.valesaquagestor.domain.repository.UserRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Autenticação de mentira para construir e testar as telas antes de existir backend
 * (decisão 🔶 de `specs/11-autenticacao.md`). Aceita `teste@teste.com` / `Teste1234` e as contas
 * criadas durante a execução; **as contas ficam só na memória** e somem quando o app fecha
 * (a sessão, não: ela é salva criptografada pelo [SessionStore]).
 *
 * Imita o servidor: cadastro, login e recuperação de senha falham sem internet; abrir o app com
 * sessão salva e sair não dependem de conexão. Escolhida por `BuildConfig.USE_FAKE_AUTH`.
 *
 * Convites (Fase 08): quem foi convidado pelo ADMIN cria a conta com o e-mail do convite e entra com o
 * perfil e o `userId` do convite, em vez de virar ADMIN de uma fazenda nova. Quem foi desativado não
 * entra mais (mesmo erro genérico de senha errada).
 */
@Singleton
class FakeAuthRepository @Inject constructor(
    private val sessionStore: SessionStore,
    private val connectivity: ConnectivityChecker,
    private val users: UserRepository,
) : AuthRepository {

    private class FakeAccount(val session: Session, val password: String)

    private val accounts = mutableMapOf(
        TEST_EMAIL to FakeAccount(
            Session(userId = "fake-user-teste", nome = "Teste", email = TEST_EMAIL, papel = Papel.ADMIN),
            TEST_PASSWORD,
        ),
    )

    override val currentSession: Flow<Session?> = sessionStore.session

    override suspend fun signIn(email: String, password: String): AuthResult {
        if (!connectivity.isOnline()) return AuthResult.Failure(AuthError.NO_CONNECTION)
        val account = accounts[normalize(email)]
        // Mesmo erro para e-mail inexistente e senha errada: não revela se a conta existe.
        if (account == null || account.password != password) return AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        val user = users.findByEmail(email)
        if (user?.status == StatusUsuario.DESATIVADO) return AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        // O perfil vale o que o ADMIN deixou na lista de usuários (ele pode ter trocado depois do cadastro).
        sessionStore.save(if (user == null) account.session else account.session.copy(papel = user.papel))
        return AuthResult.Success
    }

    override suspend fun signUp(nome: String, email: String, password: String): AuthResult {
        if (!connectivity.isOnline()) return AuthResult.Failure(AuthError.NO_CONNECTION)
        val key = normalize(email)
        if (key in accounts) return AuthResult.Failure(AuthError.EMAIL_ALREADY_REGISTERED)
        val invite = users.findByEmail(key)
        if (invite?.status == StatusUsuario.DESATIVADO) return AuthResult.Failure(AuthError.EMAIL_ALREADY_REGISTERED)
        val session = if (invite?.status == StatusUsuario.CONVIDADO) {
            // Aceitou o convite: entra com o perfil e o userId combinados com o ADMIN.
            users.save(invite.copy(nome = nome.trim(), status = StatusUsuario.ATIVO))
            Session(userId = invite.uuid, nome = nome.trim(), email = key, papel = invite.papel)
        } else {
            Session(userId = UUID.randomUUID().toString(), nome = nome.trim(), email = key, papel = Papel.ADMIN)
        }
        accounts[key] = FakeAccount(session, password)
        sessionStore.save(session)
        return AuthResult.Success
    }

    override suspend fun sendPasswordReset(email: String): AuthResult {
        if (!connectivity.isOnline()) return AuthResult.Failure(AuthError.NO_CONNECTION)
        // Resposta sempre igual, exista a conta ou não.
        return AuthResult.Success
    }

    override suspend fun signOut() = sessionStore.clear()

    private fun normalize(email: String) = email.trim().lowercase()

    companion object {
        const val TEST_EMAIL = "teste@teste.com"
        const val TEST_PASSWORD = "Teste1234"
    }
}
