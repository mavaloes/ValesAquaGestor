package com.valoes.valesaquagestor.farm

import app.cash.turbine.test
import com.valoes.valesaquagestor.domain.model.StatusLote
import com.valoes.valesaquagestor.domain.model.StatusTanque
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveFarmOverviewTest {

    @Test
    fun `sem tanques o retrato e vazio`() = runBlocking {
        assertTrue(FarmFixture().overview().first().tanks.isEmpty())
    }

    @Test
    fun `junta tanques, lotes, medicoes de hoje, tratos e biometrias`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1, "A"), tank(2, "B")),
            lots = listOf(lot(1, tankId = 1)),
            waterRecords = listOf(
                water(1, tankId = 1, at = hoursAgo(2), oxigenio = 5.0),
                water(2, tankId = 1, at = daysAgo(2), oxigenio = 3.0, ph = 7.0), // anterior a hoje
            ),
            feedings = listOf(feeding(1, at = hoursAgo(1)), feeding(2, at = daysAgo(1))),
            biometries = listOf(biometry(1, at = daysAgo(3), kg = 1.0, fish = 10)),
            movements = listOf(movement(1, quantity = 40, tipo = TipoMovimentacao.MORTALIDADE)),
        )

        val overview = farm.overview().first()

        assertEquals(listOf("A", "B"), overview.tanks.map { it.tanque.nome }.sorted())
        val a = overview.tanks.single { it.tanque.id == 1L }
        assertEquals(5.0, a.readings.oxigenioMgL!!.value, 0.0)
        assertEquals("pH vem de outra medição, mais antiga", 7.0, a.readings.ph!!.value, 0.0)
        assertEquals(listOf(1L), a.waterToday.map { it.id })
        val lotOverview = a.lots.single()
        assertEquals(1, lotOverview.feedingsToday.size)
        assertEquals(1, lotOverview.latestBiometries.size)
        assertEquals(960, lotOverview.currentQuantity)
        assertEquals(4.0, lotOverview.mortalityPct, 1e-9)
        assertTrue(overview.tanks.single { it.tanque.id == 2L }.lots.isEmpty())
    }

    @Test
    fun `tanque inativo e lote encerrado ficam de fora`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1, "A"), tank(2, "B").copy(status = StatusTanque.INATIVO)),
            lots = listOf(lot(1, tankId = 1), lot(2, tankId = 1).copy(status = StatusLote.ENCERRADO)),
        )

        val overview = farm.overview().first()

        assertEquals(listOf(1L), overview.tanks.map { it.tanque.id })
        assertEquals(listOf(1L), overview.tanks.single().lots.map { it.lote.id })
    }

    @Test
    fun `o retrato se atualiza quando um registro novo chega`() = runBlocking {
        val farm = FarmFixture(tanks = listOf(tank(1)), lots = listOf(lot(1)))

        farm.overview().test {
            assertTrue(awaitItem().tanks.single().readings.oxigenioMgL == null)

            farm.water.save(water(1, at = hoursAgo(1), oxigenio = 2.0))

            assertEquals(2.0, awaitItem().tanks.single().readings.oxigenioMgL!!.value, 0.0)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
