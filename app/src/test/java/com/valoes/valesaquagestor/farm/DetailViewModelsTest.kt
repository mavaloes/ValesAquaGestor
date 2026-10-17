package com.valoes.valesaquagestor.farm

import androidx.lifecycle.SavedStateHandle
import com.valoes.valesaquagestor.auth.MainDispatcherRule
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.AlertLevel
import com.valoes.valesaquagestor.presentation.lot.LotDetailViewModel
import com.valoes.valesaquagestor.presentation.tank.TankDetailViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DetailViewModelsTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private fun tankDetail(farm: FarmFixture, id: Long = 1) =
        TankDetailViewModel(SavedStateHandle(mapOf(TankDetailViewModel.TANK_ID_ARG to id)), farm.overview, farm.water)

    private fun lotDetail(farm: FarmFixture, id: Long = 1) = LotDetailViewModel(
        SavedStateHandle(mapOf(LotDetailViewModel.LOT_ID_ARG to id)),
        farm.lots, farm.tanks, farm.water, farm.settings, farm.movements, farm.biometry, farm.feeding,
    )

    // ---- Detalhe do tanque ----

    @Test
    fun `o tanque mostra as ultimas medicoes avaliadas e os lotes ativos`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1, "Viveiro 1")),
            lots = listOf(lot(1)),
            waterRecords = listOf(
                water(1, at = hoursAgo(5), oxigenio = 2.5, ph = 7.4, temperatura = 28.0),
                water(2, at = hoursAgo(1), amoniaTotal = 1.5, ph = 9.0),
            ),
        )

        val data = tankDetail(farm).state.first { !it.isLoading }.tank!!

        assertEquals("Viveiro 1", data.tanque.nome)
        val byKind = data.waterAlerts.associateBy { it.kind }
        assertEquals(AlertLevel.CRITICO, byKind.getValue(AlertKind.OXYGEN).status.level)
        // O pH mais recente (9,0) vem da segunda medição e a amônia tóxica é calculada com ele.
        assertEquals(AlertLevel.ATENCAO, byKind.getValue(AlertKind.PH).status.level)
        assertEquals(AlertLevel.EMERGENCIA, byKind.getValue(AlertKind.TOXIC_AMMONIA).status.level)
        assertFalse(data.ammoniaNeedsPhAndTemperature)
        assertEquals(1, data.lots.size)
    }

    @Test
    fun `amonia sem pH e temperatura avisa em vez de avaliar`() = runBlocking {
        val farm = FarmFixture(tanks = listOf(tank(1)), waterRecords = listOf(water(1, amoniaTotal = 1.5)))

        val data = tankDetail(farm).state.first { !it.isLoading }.tank!!

        assertTrue(data.ammoniaNeedsPhAndTemperature)
        assertTrue(data.waterAlerts.none { it.kind == AlertKind.TOXIC_AMMONIA })
    }

    @Test
    fun `a variacao do pH de hoje aparece entre as avaliacoes`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1)),
            waterRecords = listOf(
                water(1, at = hoursAgo(8), momento = MomentoMedicao.MANHA, ph = 6.8),
                water(2, at = hoursAgo(1), momento = MomentoMedicao.TARDE, ph = 9.0),
            ),
        )

        val data = tankDetail(farm).state.first { !it.isLoading }.tank!!

        assertTrue(data.waterAlerts.any { it.kind == AlertKind.PH_VARIATION })
    }

    @Test
    fun `tanque sem medicoes nem lotes mostra listas vazias`() = runBlocking {
        val data = tankDetail(FarmFixture(tanks = listOf(tank(1)))).state.first { !it.isLoading }.tank!!

        assertTrue(data.waterAlerts.isEmpty())
        assertTrue(data.lots.isEmpty())
    }

    @Test
    fun `tanque que nao existe fica sem dados`() = runBlocking {
        val state = tankDetail(FarmFixture(tanks = listOf(tank(1))), id = 99).state.first { !it.isLoading }

        assertNull(state.tank)
    }

    // ---- Detalhe do lote ----

    @Test
    fun `o lote mostra peixes vivos, biomassa, peso medio e mortalidade`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1, "Viveiro 1")),
            lots = listOf(lot(1, quantidadeInicial = 1000, pesoMedioAtualG = 250.0)),
            movements = listOf(movement(1, quantity = 40), movement(2, quantity = 60)),
        )

        val data = lotDetail(farm).state.first { !it.isLoading }.lot!!

        assertEquals("Viveiro 1", data.tankName)
        assertEquals(900, data.currentQuantity)
        assertEquals(225.0, data.biomassKg, 1e-9)
        assertEquals(10.0, data.mortalityPct, 1e-9)
        assertNull("sem biometria não há TCA", data.fcr)
        assertFalse(data.hasBiometry)
        assertTrue("10% exato não alerta", data.alerts.isEmpty())
    }

    @Test
    fun `com biometria e racao o lote mostra a TCA`() = runBlocking {
        // 1.000 peixes de 10 g; biometria há 5 dias com 50 g; 60 kg de ração no período -> ganho 40 kg -> TCA 1,5.
        val farm = FarmFixture(
            tanks = listOf(tank(1)),
            lots = listOf(lot(1, quantidadeInicial = 1000, dataPovoamento = daysAgo(20))),
            biometries = listOf(biometry(1, at = daysAgo(5), kg = 5.0, fish = 100)),
            feedings = listOf(feeding(1, at = daysAgo(15), kg = 30.0), feeding(2, at = daysAgo(8), kg = 30.0)),
        )

        val data = lotDetail(farm).state.first { !it.isLoading }.lot!!

        assertTrue(data.hasBiometry)
        assertEquals(1.5, data.fcr!!, 1e-9)
    }

    @Test
    fun `os alertas do lote trazem mortalidade e agua do tanque do mais grave ao menos grave`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1)),
            lots = listOf(lot(1, quantidadeInicial = 1000)),
            movements = listOf(movement(1, quantity = 101)),
            waterRecords = listOf(water(1, oxigenio = 1.5, ph = 7.0)),
        )

        val alerts = lotDetail(farm).state.first { !it.isLoading }.lot!!.alerts

        assertEquals(listOf(AlertKind.OXYGEN, AlertKind.MORTALITY), alerts.map { it.kind })
        assertEquals(listOf(AlertLevel.EMERGENCIA, AlertLevel.ATENCAO), alerts.map { it.status.level })
        assertNotNull(alerts.last().lotId)
    }

    @Test
    fun `lote que nao existe fica sem dados`() = runBlocking {
        val state = lotDetail(FarmFixture(tanks = listOf(tank(1))), id = 99).state.first { !it.isLoading }

        assertNull(state.lot)
    }
}
