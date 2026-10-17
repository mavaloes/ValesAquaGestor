package com.valoes.valesaquagestor.auth

import com.valoes.valesaquagestor.data.auth.FakeAuthRepository
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.StatusUsuario
import com.valoes.valesaquagestor.domain.model.Usuario
import com.valoes.valesaquagestor.domain.repository.AuthError
import com.valoes.valesaquagestor.domain.repository.AuthResult
import com.valoes.valesaquagestor.settings.FakeUserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Convites no FakeAuth (Fase 08): quem foi convidado entra como o ADMIN combinou; quem foi desativado não entra. */
class FakeAuthInvitesTest {

    private val invited = Usuario(
        id = 1, uuid = "convite-1", nome = "João", email = "joao@exemplo.com",
        papel = Papel.FUNCIONARIO, status = StatusUsuario.CONVIDADO,
    )

    private fun repository(users: FakeUserRepository) =
        FakeAuthRepository(FakeSessionStore(), FakeConnectivityChecker(online = true), users)

    @Test
    fun `quem foi convidado cria a conta como FUNCIONARIO com o userId do convite`() = runBlocking {
        val users = FakeUserRepository(listOf(invited))
        val repository = repository(users)

        val result = repository.signUp("João Souza", "Joao@Exemplo.com", "Joao12345")

        assertEquals(AuthResult.Success, result)
        val session = repository.currentSession.first()!!
        assertEquals(Papel.FUNCIONARIO, session.papel)
        assertEquals("convite-1", session.userId)
        assertEquals("João Souza", session.nome)
        val user = users.get("convite-1")!!
        assertEquals(StatusUsuario.ATIVO, user.status)
        assertEquals("João Souza", user.nome)
    }

    @Test
    fun `convidado como ADMIN entra como ADMIN`() = runBlocking {
        val repository = repository(FakeUserRepository(listOf(invited.copy(papel = Papel.ADMIN))))

        repository.signUp("João", "joao@exemplo.com", "Joao12345")

        assertEquals(Papel.ADMIN, repository.currentSession.first()!!.papel)
    }

    @Test
    fun `quem nao foi convidado continua virando ADMIN da propria fazenda`() = runBlocking {
        val repository = repository(FakeUserRepository(listOf(invited)))

        repository.signUp("Maria", "maria@exemplo.com", "Maria1234")

        assertEquals(Papel.ADMIN, repository.currentSession.first()!!.papel)
    }

    @Test
    fun `usuario desativado nao entra e recebe o mesmo erro generico`() = runBlocking {
        val users = FakeUserRepository(listOf(invited.copy(status = StatusUsuario.DESATIVADO)))
        val repository = repository(users)
        // A conta de teste existe no fake; basta desativá-la na lista de usuários.
        users.save(Usuario(uuid = "fake-user-teste", nome = "Teste", email = "teste@teste.com", papel = Papel.ADMIN, status = StatusUsuario.DESATIVADO))

        val result = repository.signIn("teste@teste.com", "Teste1234")

        assertEquals(AuthResult.Failure(AuthError.INVALID_CREDENTIALS), result)
        assertNull(repository.currentSession.first())
        assertEquals(
            AuthResult.Failure(AuthError.EMAIL_ALREADY_REGISTERED),
            repository.signUp("João", "joao@exemplo.com", "Joao12345"),
        )
    }

    @Test
    fun `o perfil da lista de usuarios vale no login`() = runBlocking {
        val users = FakeUserRepository(
            listOf(Usuario(uuid = "fake-user-teste", nome = "Teste", email = "teste@teste.com", papel = Papel.FUNCIONARIO)),
        )
        val repository = repository(users)

        repository.signIn("teste@teste.com", "Teste1234")

        assertEquals(Papel.FUNCIONARIO, repository.currentSession.first()!!.papel)
    }
}
