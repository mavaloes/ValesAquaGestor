package com.valoes.valesaquagestor.sync

import com.valoes.valesaquagestor.data.sync.SyncOutcome
import com.valoes.valesaquagestor.domain.model.Arracoamento
import com.valoes.valesaquagestor.domain.model.Biometria
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import com.valoes.valesaquagestor.domain.model.QualidadeAgua
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
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

/**
 * Fase 09: a regra de sincronização de `specs/05-offline-sync.md` e os casos G1 a G3 de
 * `specs/09-casos-de-teste.md`, com dois aparelhos (bancos Room reais em memória) e um servidor simulado.
 */
@RunWith(RobolectricTestRunner::class)
class SyncEngineTest {

    private lateinit var server: FakeRemoteServer
    private lateinit var a: Device
    private lateinit var b: Device
    private val at = Instant.ofEpochMilli(500_000L)

    @Before
    fun setUp() {
        server = FakeRemoteServer()
        a = Device(server)
        b = Device(server)
    }

    @After
    fun tearDown() {
        a.close()
        b.close()
    }

    private suspend fun Device.pending() = env.pendentes.observePendingCount().first()

    private fun tankName(uuid: String): String? = (server.row("tanque", uuid)?.get("nome") as? JsonPrimitive)?.content

    /** Tanque + lote no aparelho, devolvendo (tanque, lote). */
    private suspend fun Device.farm(name: String = "Viveiro 1"): Pair<Long, Long> {
        val tank = env.tanques.save(env.newTank(name))
        return tank to env.lotes.save(env.newLot(tank))
    }

    // ---- Envio ----

    @Test
    fun `envia tudo que esta pendente, pais antes dos filhos, e marca como enviado`() = runBlocking {
        val (tank, lot) = a.farm()
        a.env.agua.save(QualidadeAgua(tanqueId = tank, dataHora = at, momento = MomentoMedicao.MANHA, oxigenioMgL = 5.0))
        a.env.arracoamentos.save(Arracoamento(loteId = lot, dataHora = at, quantidadeKg = 1.0, comportamento = TestComportamento))
        a.env.biometrias.save(Biometria(loteId = lot, dataHora = at, pesoAmostraKg = 1.0, numeroPeixesAmostra = 10))
        a.env.movimentacoes.save(MovimentacaoLote(loteId = lot, dataHora = at, tipo = TipoMovimentacao.MORTALIDADE, quantidade = 3))
        assertEquals(6, a.pending())

        assertEquals(SyncOutcome.SUCCESS, a.engine.run())

        assertEquals(0, a.pending())
        listOf("tanque", "lote", "qualidade_agua", "arracoamento", "biometria", "movimentacao_lote").forEach {
            assertEquals(it, 1, server.rows(it).size)
        }
    }

    @Test
    fun `registro com criador envia o criador e sem internet nada se perde`() = runBlocking {
        a.env.currentUserId = "func-1"
        val (tank, _) = a.farm()
        a.env.agua.save(QualidadeAgua(tanqueId = tank, dataHora = at, momento = MomentoMedicao.MANHA, oxigenioMgL = 5.0))
        server.offline = true

        assertEquals(SyncOutcome.RETRY, a.engine.run())
        assertEquals(3, a.pending()) // nada perdido, nada marcado

        server.offline = false
        assertEquals(SyncOutcome.SUCCESS, a.engine.run())
        assertEquals("func-1", (server.rows("qualidade_agua").single()["criado_por_user_id"] as JsonPrimitive).content)
    }

    // ---- G1: mesmo registro enviado duas vezes ----

    @Test
    fun `G1 reenviar o mesmo registro deixa um unico registro no servidor`() = runBlocking {
        val (tank, _) = a.farm()
        a.engine.run()
        // O servidor aceitou, mas o aparelho caiu antes de marcar como enviado: o registro volta a ficar pendente.
        val dao = a.env.db.tanqueDao()
        dao.update(dao.getByIdIncludingDeleted(tank)!!.copy(sincronizado = false))

        assertEquals(SyncOutcome.SUCCESS, a.engine.run())

        assertEquals(1, server.rows("tanque").size)
        assertEquals(1, server.rows("lote").size)
        assertEquals(0, a.pending())
    }

    @Test
    fun `G1 depois de sincronizado, outra rodada nao envia nem baixa nada de novo`() = runBlocking {
        a.farm()
        a.engine.run()
        val pushes = server.pushCalls

        a.engine.run()

        assertEquals(pushes, server.pushCalls)
        assertEquals(1, a.env.db.tanqueDao().observeActive().first().size)
    }

    // ---- Queda de conexão no meio do envio ----

    @Test
    fun `queda no meio do envio mantem o resto pendente e a rodada seguinte completa sem duplicar`() = runBlocking {
        val (tank, _) = a.farm()
        repeat(150) { i ->
            a.env.agua.save(
                QualidadeAgua(tanqueId = tank, dataHora = Instant.ofEpochMilli(1_000L + i), momento = MomentoMedicao.MANHA, oxigenioMgL = 4.0 + i),
            )
        }
        // Chamadas: 1 tanque, 2 lote, 3 primeiro lote de 100 águas, 4 segundo lote (cai).
        server.dropPushCall = 4

        assertEquals(SyncOutcome.RETRY, a.engine.run())
        assertEquals(100, server.rows("qualidade_agua").size)
        assertEquals(50, a.pending()) // só o que o servidor não confirmou

        assertEquals(SyncOutcome.SUCCESS, a.engine.run())
        assertEquals(150, server.rows("qualidade_agua").size)
        assertEquals(0, a.pending())
    }

    @Test
    fun `edicao feita durante o envio nao e marcada como enviada e vai na proxima rodada`() = runBlocking {
        a.env.clock.now = 2_000_000L
        val tank = a.env.tanques.save(a.env.newTank("Viveiro 1"))
        server.afterAccepting = { table ->
            if (table == "tanque") {
                // O usuário edita o tanque enquanto o envio está em andamento.
                a.env.clock.now = 3_000_000L
                a.env.tanques.save(a.env.tanques.get(tank)!!.copy(nome = "Viveiro Editado"))
            }
        }

        a.engine.run()
        assertEquals(1, a.pending()) // a versão nova ainda não foi enviada

        server.afterAccepting = null
        a.engine.run()
        assertEquals(0, a.pending())
        assertEquals("Viveiro Editado", tankName(a.env.tanques.get(tank)!!.uuid))
    }

    // ---- G2: dois aparelhos editando o mesmo registro ----

    @Test
    fun `G2 dois aparelhos editam offline e vence o maior atualizadoEm, em qualquer ordem de sincronizacao`() = runBlocking {
        val tank = a.env.tanques.save(a.env.newTank("Original"))
        val uuid = a.env.tanques.get(tank)!!.uuid
        a.engine.run()
        b.engine.run()
        val tankOnB = b.env.tanques.observeActive().first().single().id

        // Os dois editam sem conexão: B edita depois (relógio maior), A antes.
        a.env.clock.now = 2_000_000L
        a.env.tanques.save(a.env.tanques.get(tank)!!.copy(nome = "Editado em A"))
        b.env.clock.now = 3_000_000L
        b.env.tanques.save(b.env.tanques.get(tankOnB)!!.copy(nome = "Editado em B"))

        // A sincroniza primeiro, depois B, depois A de novo.
        a.engine.run()
        b.engine.run()
        a.engine.run()

        assertEquals("Editado em B", tankName(uuid))
        assertEquals("Editado em B", a.env.tanques.get(tank)!!.nome)
        assertEquals("Editado em B", b.env.tanques.get(tankOnB)!!.nome)
    }

    @Test
    fun `G2 o aparelho com a edicao mais antiga sincronizando por ultimo nao sobrescreve a mais nova`() = runBlocking {
        val tank = a.env.tanques.save(a.env.newTank("Original"))
        val uuid = a.env.tanques.get(tank)!!.uuid
        a.engine.run()
        b.engine.run()
        val tankOnB = b.env.tanques.observeActive().first().single().id

        a.env.clock.now = 4_000_000L
        a.env.tanques.save(a.env.tanques.get(tank)!!.copy(nome = "Editado em A"))
        b.env.clock.now = 3_000_000L
        b.env.tanques.save(b.env.tanques.get(tankOnB)!!.copy(nome = "Editado em B"))

        // A (edição mais nova) chega primeiro; B (mais velha) tenta sobrescrever depois.
        a.engine.run()
        b.engine.run()
        a.engine.run()

        assertEquals("Editado em A", tankName(uuid))
        assertEquals("Editado em A", b.env.tanques.get(tankOnB)!!.nome)
        assertEquals(0, b.pending()) // a edição perdida não fica pendente para sempre
    }

    // ---- Baixar ----

    @Test
    fun `um aparelho novo baixa tanque, lote e registros do outro, com as referencias certas`() = runBlocking {
        val (tank, lot) = a.farm("Viveiro do A")
        a.env.agua.save(QualidadeAgua(tanqueId = tank, dataHora = at, momento = MomentoMedicao.MANHA, oxigenioMgL = 5.5))
        a.env.arracoamentos.save(Arracoamento(loteId = lot, dataHora = at, quantidadeKg = 2.5, comportamento = TestComportamento))
        a.engine.run()

        assertEquals(SyncOutcome.SUCCESS, b.engine.run())

        val tankOnB = b.env.tanques.observeActive().first().single()
        assertEquals("Viveiro do A", tankOnB.nome)
        val lotOnB = b.env.lotes.observeAllActive().first().single()
        assertEquals(tankOnB.id, lotOnB.tanqueId)
        assertEquals(5.5, b.env.agua.observeRecent(tankOnB.id, 10).first().single().oxigenioMgL!!, 0.0)
        assertEquals(2.5, b.env.arracoamentos.observeRecent(lotOnB.id, 10).first().single().quantidadeKg, 0.0)
        assertEquals(0, b.pending()) // o que veio do servidor já nasce sincronizado
    }

    @Test
    fun `a rodada seguinte retoma o download pela marca guardada`() = runBlocking {
        a.farm()
        a.engine.run()
        b.engine.run()
        val firstCursor = b.cursors.values.getValue("tanque")

        server.pullRequests.clear()
        b.engine.run()

        assertTrue(server.pullRequests.any { it != null })
        assertEquals(firstCursor, b.cursors.values.getValue("tanque"))
    }

    @Test
    fun `registro cujo pai nao existe aqui e deixado de fora sem travar os outros`() = runBlocking {
        // Água de um tanque que nunca chegou a este aparelho (pai ausente) e um tanque válido.
        val (tank, _) = a.farm("Viveiro 1")
        a.env.agua.save(QualidadeAgua(tanqueId = tank, dataHora = at, momento = MomentoMedicao.MANHA, oxigenioMgL = 5.0))
        a.engine.run()
        val orphanUuid = server.rows("qualidade_agua").single()
        server.tables["qualidade_agua"]!!["orfao"] = kotlinx.serialization.json.JsonObject(
            orphanUuid + mapOf("uuid" to JsonPrimitive("orfao"), "tanque_uuid" to JsonPrimitive("nao-existe")),
        )

        assertEquals(SyncOutcome.SUCCESS, b.engine.run())

        assertEquals(1, b.env.tanques.observeActive().first().size)
        val tankOnB = b.env.tanques.observeActive().first().single()
        assertEquals(1, b.env.agua.observeRecent(tankOnB.id, 10).first().size)
    }

    // ---- G3: exclusão lógica ----

    @Test
    fun `G3 excluido nao aparece nas listas mas continua no banco ate o servidor confirmar`() = runBlocking {
        val tank = a.env.tanques.save(a.env.newTank("Viveiro 1"))
        a.engine.run()
        a.env.tanques.delete(tank)

        // Antes de sincronizar: some das listas, mas o registro segue no banco, pendente.
        assertTrue(a.env.tanques.observeActive().first().isEmpty())
        assertNull(a.env.tanques.get(tank))
        val local = a.env.db.tanqueDao().getByIdIncludingDeleted(tank)
        assertNotNull(local)
        assertTrue(local!!.excluido)
        assertFalse(local.sincronizado)
        assertEquals(1, a.pending())
    }

    @Test
    fun `G3 depois da confirmacao do servidor a exclusao chega aos outros aparelhos e sai do banco`() = runBlocking {
        val tank = a.env.tanques.save(a.env.newTank("Viveiro 1"))
        val uuid = a.env.tanques.get(tank)!!.uuid
        a.engine.run()
        b.engine.run()
        val tankOnB = b.env.tanques.observeActive().first().single().id

        a.env.clock.now = 5_000_000L
        a.env.tanques.delete(tank)
        a.engine.run()

        // O servidor guarda o túmulo; o aparelho que excluiu já pode apagar de vez.
        assertEquals(true, (server.row("tanque", uuid)!!["excluido"] as JsonPrimitive).content.toBoolean())
        assertNull(a.env.db.tanqueDao().getByIdIncludingDeleted(tank))

        b.engine.run()
        assertTrue(b.env.tanques.observeActive().first().isEmpty())
        assertNull(b.env.db.tanqueDao().getByIdIncludingDeleted(tankOnB))

        // Nenhum dos dois traz o tanque de volta nas rodadas seguintes.
        a.engine.run()
        b.engine.run()
        assertNull(a.env.db.tanqueDao().findByUuid(uuid))
        assertNull(b.env.db.tanqueDao().findByUuid(uuid))
    }

    @Test
    fun `exclusao de pai com filho ainda no banco nao some de vez`() = runBlocking {
        val (tank, lot) = a.farm()
        a.engine.run()
        a.env.clock.now = 6_000_000L
        a.env.tanques.delete(tank) // o lote continua apontando para o tanque
        a.engine.run()

        // A chave estrangeira impede apagar o tanque; ele fica como excluído e confirmado.
        val kept = a.env.db.tanqueDao().getByIdIncludingDeleted(tank)
        assertNotNull(kept)
        assertTrue(kept!!.excluido && kept.sincronizado)
        assertNotNull(a.env.lotes.get(lot))
    }

    // ---- Falhas ----

    @Test
    fun `sessao recusada pede novo login e nao apaga nenhum dado local`() = runBlocking {
        val (tank, _) = a.farm()
        server.authExpired = true

        assertEquals(SyncOutcome.AUTH_EXPIRED, a.engine.run())

        assertEquals(2, a.pending())
        assertNotNull(a.env.tanques.get(tank))
    }

    @Test
    fun `o estado mostra sincronizando e volta ao normal ou a falha`() = runBlocking {
        a.farm()
        server.offline = true
        a.engine.run()
        assertEquals(com.valoes.valesaquagestor.domain.sync.SyncPhase.FAILED, a.state.phase.first())

        server.offline = false
        a.engine.run()
        assertEquals(com.valoes.valesaquagestor.domain.sync.SyncPhase.IDLE, a.state.phase.first())
    }
}

private val TestComportamento = com.valoes.valesaquagestor.domain.model.ComportamentoPeixe.NORMAL
