package com.valoes.valesaquagestor.auth

import com.valoes.valesaquagestor.data.auth.FakeAuthRepository
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.repository.AuthError
import com.valoes.valesaquagestor.domain.repository.AuthResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FakeAuthRepositoryTest {

    private lateinit var store: FakeSessionStore
    private lateinit var connectivity: FakeConnectivityChecker
    private lateinit var repository: FakeAuthRepository

    @Before
    fun setUp() {
        store = FakeSessionStore()
        connectivity = FakeConnectivityChecker(online = true)
        repository = FakeAuthRepository(store, connectivity)
    }

    private val failure = { error: AuthError -> AuthResult.Failure(error) }

    @Test
    fun `sem login nao ha sessao`() = runBlocking {
        assertNull(repository.currentSession.first())
    }

    @Test
    fun `login com a conta de teste abre a sessao`() = runBlocking {
        val result = repository.signIn("teste@teste.com", "Teste1234")

        assertEquals(AuthResult.Success, result)
        val session = repository.currentSession.first()!!
        assertEquals("teste@teste.com", session.email)
        assertEquals(Papel.ADMIN, session.papel)
    }

    @Test
    fun `e-mail e comparado sem maiusculas e sem espacos`() = runBlocking {
        assertEquals(AuthResult.Success, repository.signIn("  TESTE@Teste.com ", "Teste1234"))
    }

    @Test
    fun `senha errada e e-mail inexistente dao exatamente o mesmo erro`() = runBlocking {
        val wrongPassword = repository.signIn("teste@teste.com", "errada123")
        val unknownEmail = repository.signIn("ninguem@teste.com", "Teste1234")

        assertEquals(failure(AuthError.INVALID_CREDENTIALS), wrongPassword)
        assertEquals(wrongPassword, unknownEmail)
        assertNull(repository.currentSession.first())
    }

    @Test
    fun `sem internet login, cadastro e recuperacao falham com erro de conexao`() = runBlocking {
        connectivity.online = false

        assertEquals(failure(AuthError.NO_CONNECTION), repository.signIn("teste@teste.com", "Teste1234"))
        assertEquals(failure(AuthError.NO_CONNECTION), repository.signUp("Maria", "maria@exemplo.com", "Maria1234"))
        assertEquals(failure(AuthError.NO_CONNECTION), repository.sendPasswordReset("teste@teste.com"))
        assertNull(repository.currentSession.first())
    }

    @Test
    fun `cadastro cria a conta como ADMIN e ja abre a sessao`() = runBlocking {
        val result = repository.signUp("  Maria Silva ", "Maria@Exemplo.com", "Maria1234")

        assertEquals(AuthResult.Success, result)
        val session = repository.currentSession.first()!!
        assertEquals("Maria Silva", session.nome)
        assertEquals("maria@exemplo.com", session.email)
        assertEquals(Papel.ADMIN, session.papel)
        assertNotNull(session.userId)
    }

    @Test
    fun `conta criada no cadastro consegue entrar depois de sair`() = runBlocking {
        repository.signUp("Maria", "maria@exemplo.com", "Maria1234")
        repository.signOut()

        assertEquals(AuthResult.Success, repository.signIn("maria@exemplo.com", "Maria1234"))
    }

    @Test
    fun `cadastro com e-mail ja usado e recusado`() = runBlocking {
        assertEquals(
            failure(AuthError.EMAIL_ALREADY_REGISTERED),
            repository.signUp("Outro", "teste@teste.com", "Outra1234"),
        )
        assertNull(repository.currentSession.first())
    }

    @Test
    fun `recuperacao de senha responde igual com e-mail cadastrado ou nao`() = runBlocking {
        assertEquals(AuthResult.Success, repository.sendPasswordReset("teste@teste.com"))
        assertEquals(AuthResult.Success, repository.sendPasswordReset("ninguem@exemplo.com"))
    }

    @Test
    fun `sair apaga a sessao`() = runBlocking {
        repository.signIn("teste@teste.com", "Teste1234")
        repository.signOut()

        assertNull(repository.currentSession.first())
    }

    @Test
    fun `sessao salva continua disponivel sem internet`() = runBlocking {
        val saved = Session("u1", "Maria", "maria@exemplo.com", Papel.FUNCIONARIO)
        val offlineRepository = FakeAuthRepository(FakeSessionStore(saved), FakeConnectivityChecker(online = false))

        assertEquals(saved, offlineRepository.currentSession.first())
    }

    @Test
    fun `sair funciona sem internet`() = runBlocking {
        repository.signIn("teste@teste.com", "Teste1234")
        connectivity.online = false

        repository.signOut()

        assertNull(repository.currentSession.first())
    }
}
