package com.valoes.valesaquagestor.data

import com.valoes.valesaquagestor.domain.model.Arracoamento
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.model.QualidadeAgua
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WaterAndFeedingRepositoryTest {

    private lateinit var env: TestEnvironment

    @Before
    fun setUp() {
        env = TestEnvironment()
    }

    @After
    fun tearDown() = env.close()

    private fun water(tankId: Long, at: Long, block: QualidadeAgua.() -> QualidadeAgua) =
        QualidadeAgua(tanqueId = tankId, dataHora = Instant.ofEpochMilli(at), momento = MomentoMedicao.MANHA).block()

    @Test
    fun `medicao sem nenhuma medida e recusada`() = runBlocking {
        val tank = env.tanques.save(env.newTank())
        try {
            env.agua.save(water(tank, 1_000L) { this })
            fail("deveria exigir pelo menos uma medida")
        } catch (expected: IllegalArgumentException) {
            // esperado
        }
    }

    @Test
    fun `ultima leitura de cada parametro vem da medicao mais recente em que ele foi informado`() = runBlocking {
        val tank = env.tanques.save(env.newTank())
        env.agua.save(water(tank, 1_000L) { copy(oxigenioMgL = 5.0, amoniaTotalMgL = 0.4) })
        env.agua.save(water(tank, 2_000L) { copy(oxigenioMgL = 3.5, ph = 7.2) })
        val removed = env.agua.save(water(tank, 3_000L) { copy(oxigenioMgL = 1.0) })
        env.agua.delete(removed)

        val latest = env.agua.observeLatestReadings(tank).first()

        assertEquals(3.5, latest.oxigenioMgL!!.value, 0.0)
        assertEquals(Instant.ofEpochMilli(2_000L), latest.oxigenioMgL.dataHora)
        assertEquals(7.2, latest.ph!!.value, 0.0)
        assertEquals("amônia semanal continua visível", 0.4, latest.amoniaTotalMgL!!.value, 0.0)
        assertNull(latest.temperaturaC)
        assertNull(latest.nitritoMgL)
    }

    @Test
    fun `medicoes do dia usam intervalo aberto no fim`() = runBlocking {
        val tank = env.tanques.save(env.newTank())
        val otherTank = env.tanques.save(env.newTank("Outro"))
        env.agua.save(water(tank, 999L) { copy(ph = 7.0) })
        env.agua.save(water(tank, 1_000L) { copy(ph = 7.1) })
        env.agua.save(water(tank, 1_999L) { copy(ph = 7.2) })
        env.agua.save(water(tank, 2_000L) { copy(ph = 7.3) })
        env.agua.save(water(otherTank, 1_500L) { copy(ph = 9.0) })

        val day = env.agua.observeBetween(tank, Instant.ofEpochMilli(1_000L), Instant.ofEpochMilli(2_000L)).first()

        assertEquals(listOf(7.1, 7.2), day.map { it.ph })
    }

    @Test
    fun `tratos do lote por periodo, sem excluidos`() = runBlocking {
        val tank = env.tanques.save(env.newTank())
        val lot = env.lotes.save(env.newLot(tank))
        val otherLot = env.lotes.save(env.newLot(tank))
        fun feed(loteId: Long, at: Long, kg: Double) = Arracoamento(
            loteId = loteId, dataHora = Instant.ofEpochMilli(at), quantidadeKg = kg,
            comportamento = TestEnvironment.behavior,
        )
        env.arracoamentos.save(feed(lot, 1_000L, 1.0))
        val second = env.arracoamentos.save(feed(lot, 2_000L, 2.0))
        val removed = env.arracoamentos.save(feed(lot, 2_500L, 9.0))
        env.arracoamentos.delete(removed)
        env.arracoamentos.save(feed(lot, 5_000L, 3.0))
        env.arracoamentos.save(feed(otherLot, 2_000L, 7.0))

        val list = env.arracoamentos.observeByLot(lot, Instant.ofEpochMilli(1_000L), Instant.ofEpochMilli(3_000L)).first()

        assertEquals(listOf(1.0, 2.0), list.map { it.quantidadeKg })
        assertNotNull(list.firstOrNull { it.id == second })
    }
}
