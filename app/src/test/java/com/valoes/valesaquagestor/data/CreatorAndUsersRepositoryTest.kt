package com.valoes.valesaquagestor.data

import com.valoes.valesaquagestor.domain.model.Arracoamento
import com.valoes.valesaquagestor.domain.model.Biometria
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.QualidadeAgua
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.model.StatusUsuario
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import com.valoes.valesaquagestor.domain.model.Usuario
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Fase 08: quem criou cada registro (`criadoPorUserId`), usuários no Room e contagem de pendências. */
@RunWith(RobolectricTestRunner::class)
class CreatorAndUsersRepositoryTest {

    private lateinit var env: TestEnvironment
    private val at = Instant.ofEpochMilli(1_000L)

    @Before
    fun setUp() {
        env = TestEnvironment()
    }

    @After
    fun tearDown() = env.close()

    /** Tanque e lote prontos para receber registros. */
    private suspend fun tankAndLot(): Pair<Long, Long> {
        val tank = env.tanques.save(env.newTank())
        return tank to env.lotes.save(env.newLot(tank))
    }

    // ---- criadoPorUserId ----

    @Test
    fun `cada registro novo guarda o usuario logado como criador`() = runBlocking {
        env.currentUserId = "func-1"
        val (tank, lot) = tankAndLot()

        env.agua.save(QualidadeAgua(tanqueId = tank, dataHora = at, momento = MomentoMedicao.MANHA, oxigenioMgL = 5.0))
        env.arracoamentos.save(
            Arracoamento(loteId = lot, dataHora = at, quantidadeKg = 1.0, comportamento = TestEnvironment.behavior),
        )
        env.biometrias.save(Biometria(loteId = lot, dataHora = at, pesoAmostraKg = 1.0, numeroPeixesAmostra = 10))
        env.movimentacoes.save(MovimentacaoLote(loteId = lot, dataHora = at, tipo = TipoMovimentacao.MORTALIDADE, quantidade = 2))

        assertEquals("func-1", env.agua.observeRecent(tank, 10).first().single().criadoPorUserId)
        assertEquals("func-1", env.arracoamentos.observeRecent(lot, 10).first().single().criadoPorUserId)
        assertEquals("func-1", env.biometrias.observeLatest(lot, 10).first().single().criadoPorUserId)
        assertEquals("func-1", env.movimentacoes.observeByLot(lot).first().single().criadoPorUserId)
    }

    @Test
    fun `editar um registro nao troca o criador`() = runBlocking {
        env.currentUserId = "func-1"
        val (tank, _) = tankAndLot()
        val id = env.agua.save(QualidadeAgua(tanqueId = tank, dataHora = at, momento = MomentoMedicao.MANHA, oxigenioMgL = 5.0))

        env.currentUserId = "admin-1"
        val saved = env.agua.observeRecent(tank, 10).first().single()
        env.agua.save(saved.copy(oxigenioMgL = 6.0))

        val edited = env.agua.observeRecent(tank, 10).first().single()
        assertEquals(id, edited.id)
        assertEquals(6.0, edited.oxigenioMgL!!, 0.0)
        assertEquals("func-1", edited.criadoPorUserId)
    }

    @Test
    fun `sem sessao o criador fica vazio`() = runBlocking {
        env.currentUserId = null
        val (tank, _) = tankAndLot()
        env.agua.save(QualidadeAgua(tanqueId = tank, dataHora = at, momento = MomentoMedicao.MANHA, oxigenioMgL = 5.0))

        assertNull(env.agua.observeRecent(tank, 10).first().single().criadoPorUserId)
    }

    // ---- Usuários ----

    @Test
    fun `salvar usuario novo gera uuid, normaliza o e-mail e marca como nao enviado`() = runBlocking {
        val uuid = env.usuarios.save(Usuario(nome = " Maria ", email = " Maria@Exemplo.COM ", papel = Papel.FUNCIONARIO, status = StatusUsuario.CONVIDADO))

        val user = env.usuarios.get(uuid)!!
        assertEquals("Maria", user.nome)
        assertEquals("maria@exemplo.com", user.email)
        assertEquals(StatusUsuario.CONVIDADO, user.status)
        assertFalse(user.sincronizado)
        assertEquals(uuid, env.usuarios.findByEmail("MARIA@exemplo.com")!!.uuid)
    }

    @Test
    fun `trocar o perfil atualiza o mesmo usuario sem criar outro`() = runBlocking {
        val uuid = env.usuarios.save(Usuario(nome = "João", email = "joao@exemplo.com", papel = Papel.FUNCIONARIO))
        val user = env.usuarios.get(uuid)!!

        env.usuarios.save(user.copy(papel = Papel.ADMIN))

        val all = env.usuarios.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(Papel.ADMIN, all.single().papel)
        assertEquals(uuid, all.single().uuid)
    }

    @Test
    fun `a sessao entra na lista de usuarios uma unica vez e sem regravar o que nao mudou`() = runBlocking {
        val session = Session("admin-1", "Maria Silva", "maria@exemplo.com", Papel.ADMIN)

        env.usuarios.ensureFromSession(session)
        // Simula o envio ao servidor: se nada mudou, a próxima chamada não pode marcar como pendente de novo.
        val dao = env.db.usuarioDao()
        dao.update(dao.findByUuid("admin-1")!!.copy(sincronizado = true))
        env.usuarios.ensureFromSession(session)

        val all = env.usuarios.observeAll().first()
        assertEquals(1, all.size)
        assertEquals("admin-1", all.single().uuid)
        assertEquals(StatusUsuario.ATIVO, all.single().status)
        assertTrue(all.single().sincronizado)
    }

    @Test
    fun `quando o perfil da sessao muda, a lista acompanha`() = runBlocking {
        env.usuarios.ensureFromSession(Session("u1", "João", "joao@exemplo.com", Papel.FUNCIONARIO))
        env.usuarios.ensureFromSession(Session("u1", "João", "joao@exemplo.com", Papel.ADMIN))

        assertEquals(Papel.ADMIN, env.usuarios.get("u1")!!.papel)
    }

    // ---- Pendências ----

    @Test
    fun `a contagem de pendentes soma todas as tabelas e cai quando o registro e enviado`() = runBlocking {
        assertEquals(0, env.pendentes.observePendingCount().first())

        env.currentUserId = "admin-1"
        val (tank, lot) = tankAndLot() // tanque + lote
        env.agua.save(QualidadeAgua(tanqueId = tank, dataHora = at, momento = MomentoMedicao.MANHA, oxigenioMgL = 5.0))
        env.biometrias.save(Biometria(loteId = lot, dataHora = at, pesoAmostraKg = 1.0, numeroPeixesAmostra = 10))
        env.usuarios.save(Usuario(nome = "João", email = "joao@exemplo.com", papel = Papel.FUNCIONARIO, status = StatusUsuario.CONVIDADO))
        assertEquals(5, env.pendentes.observePendingCount().first())

        val dao = env.db.tanqueDao()
        dao.update(dao.getByIdIncludingDeleted(tank)!!.copy(sincronizado = true))
        assertEquals(4, env.pendentes.observePendingCount().first())
        assertNotNull(env.usuarios.findByEmail("joao@exemplo.com"))
    }
}
