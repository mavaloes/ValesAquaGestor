package com.valoes.valesaquagestor.farm

import com.valoes.valesaquagestor.domain.calc.AlertAggregator
import com.valoes.valesaquagestor.domain.model.LatestWaterReadings
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.model.QualidadeAgua
import com.valoes.valesaquagestor.domain.model.Reading
import com.valoes.valesaquagestor.domain.model.Tanque
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.AlertLevel
import com.valoes.valesaquagestor.domain.usecase.FarmOverview
import com.valoes.valesaquagestor.domain.usecase.LotOverview
import com.valoes.valesaquagestor.domain.usecase.TankOverview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertAggregatorTest {

    private fun overview(
        tanque: Tanque = tank(),
        readings: LatestWaterReadings = LatestWaterReadings(),
        waterToday: List<QualidadeAgua> = emptyList(),
        deadCount: Int = 0,
    ): TankOverview {
        val movements = if (deadCount > 0) listOf(movement(1, quantity = deadCount, tipo = TipoMovimentacao.MORTALIDADE)) else emptyList()
        return TankOverview(tanque, listOf(LotOverview(lot(), movements, emptyList(), emptyList())), readings, waterToday)
    }

    private fun oxygen(value: Double) = LatestWaterReadings(oxigenioMgL = Reading(value, NOW))

    @Test
    fun `valores adequados nao geram alerta`() {
        val tank = overview(readings = LatestWaterReadings(oxigenioMgL = Reading(6.0, NOW), ph = Reading(7.5, NOW)))

        assertTrue(AlertAggregator.alertsFor(tank).isEmpty())
        assertNull(AlertAggregator.worstLevel(tank))
    }

    @Test
    fun `emergencia vem primeiro, depois critico e atencao`() {
        val farm = FarmOverview(
            listOf(
                overview(tank(1, "A"), oxygen(3.5)), // ATENCAO
                overview(tank(2, "B"), oxygen(1.5)), // EMERGENCIA
                overview(tank(3, "C"), LatestWaterReadings(ph = Reading(9.5, NOW))), // CRITICO
            ),
        )

        val levels = AlertAggregator.prioritized(farm).map { it.tankName to it.status.level }

        assertEquals(
            listOf("B" to AlertLevel.EMERGENCIA, "C" to AlertLevel.CRITICO, "A" to AlertLevel.ATENCAO),
            levels,
        )
    }

    @Test
    fun `no mesmo nivel ordena por tanque e depois por tipo`() {
        val farm = FarmOverview(
            listOf(
                overview(tank(1, "Zeta"), oxygen(3.5)),
                overview(tank(2, "Alfa"), LatestWaterReadings(oxigenioMgL = Reading(3.5, NOW), ph = Reading(8.6, NOW))),
            ),
        )

        val order = AlertAggregator.prioritized(farm).map { it.tankName to it.kind }

        assertEquals(
            listOf("Alfa" to AlertKind.OXYGEN, "Alfa" to AlertKind.PH, "Zeta" to AlertKind.OXYGEN),
            order,
        )
    }

    @Test
    fun `variacao do pH no dia acima de 2 gera alerta composto`() {
        val today = listOf(
            water(1, at = hoursAgo(9), momento = MomentoMedicao.MANHA, ph = 6.8),
            water(2, at = hoursAgo(1), momento = MomentoMedicao.TARDE, ph = 9.0),
        )

        val kinds = AlertAggregator.alertsFor(overview(waterToday = today)).map { it.kind }

        assertTrue(AlertKind.PH_VARIATION in kinds)
    }

    @Test
    fun `variacao de pH de exatamente 2 nao gera alerta`() {
        val today = listOf(
            water(1, at = hoursAgo(9), momento = MomentoMedicao.MANHA, ph = 7.0),
            water(2, at = hoursAgo(1), momento = MomentoMedicao.TARDE, ph = 9.0),
        )

        assertTrue(AlertAggregator.alertsFor(overview(waterToday = today)).none { it.kind == AlertKind.PH_VARIATION })
    }

    @Test
    fun `sem pH da tarde nao ha variacao`() {
        val today = listOf(water(1, momento = MomentoMedicao.MANHA, ph = 6.8))

        assertTrue(AlertAggregator.alertsFor(overview(waterToday = today)).isEmpty())
    }

    @Test
    fun `mortalidade acima de 10 por cento gera alerta do lote`() {
        val alerts = AlertAggregator.alertsFor(overview(deadCount = 101))

        val mortality = alerts.single { it.kind == AlertKind.MORTALITY }
        assertEquals(1L, mortality.lotId)
        assertEquals(10.1, mortality.value!!, 1e-9)
        assertEquals(AlertLevel.ATENCAO, mortality.status.level)
    }

    @Test
    fun `mortalidade de exatamente 10 por cento nao gera alerta`() {
        assertTrue(AlertAggregator.alertsFor(overview(deadCount = 100)).isEmpty())
    }

    @Test
    fun `nivel geral do tanque considera leituras adequadas e alertas compostos`() {
        assertNull(AlertAggregator.overallLevel(overview()))
        assertEquals(AlertLevel.ADEQUADO, AlertAggregator.overallLevel(overview(readings = oxygen(6.0))))
        assertEquals(AlertLevel.CRITICO, AlertAggregator.overallLevel(overview(readings = oxygen(2.5), deadCount = 101)))
        assertEquals(AlertLevel.ATENCAO, AlertAggregator.overallLevel(overview(readings = oxygen(6.0), deadCount = 101)))
    }
}
