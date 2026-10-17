package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.rules.AlertItem
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.AlertLevel
import com.valoes.valesaquagestor.domain.rules.CompositeAlerts
import com.valoes.valesaquagestor.domain.rules.WaterRanges
import com.valoes.valesaquagestor.domain.rules.WaterReadingsEvaluator
import com.valoes.valesaquagestor.domain.usecase.FarmOverview
import com.valoes.valesaquagestor.domain.usecase.TankOverview
import kotlin.math.abs

/**
 * Reúne os alertas de um tanque (e da fazenda) e os coloca em ordem de prioridade:
 * EMERGENCIA primeiro, depois CRITICO e ATENCAO. Valores ADEQUADO não são alerta.
 *
 * Fontes de alerta: leituras de água (cada parâmetro), variação do pH no dia (manhã × tarde de
 * hoje) e mortalidade acumulada de cada lote ativo.
 */
object AlertAggregator {

    /** Alertas de um tanque, sem ordem definida. */
    fun alertsFor(tank: TankOverview, ranges: WaterRanges = WaterRanges.DEFAULT): List<AlertItem> {
        val tanque = tank.tanque
        val water = WaterReadingsEvaluator.evaluate(tank.readings, ranges).alerts
            .filter { it.status.level > AlertLevel.ADEQUADO }
            .map { AlertItem(tanque.id, tanque.nome, null, it.kind, it.value, it.status, it.measuredAt) }

        val phMorning = tank.waterToday.firstOrNull { it.momento == MomentoMedicao.MANHA && it.ph != null }?.ph
        val phAfternoon = tank.waterToday.firstOrNull { it.momento == MomentoMedicao.TARDE && it.ph != null }?.ph
        val phVariation = if (phMorning != null && phAfternoon != null) {
            CompositeAlerts.phDailyVariation(phMorning, phAfternoon)?.let {
                AlertItem(tanque.id, tanque.nome, null, AlertKind.PH_VARIATION, abs(phAfternoon - phMorning), it, null)
            }
        } else {
            null
        }

        val mortality = tank.lots.mapNotNull { lot ->
            CompositeAlerts.accumulatedMortality(lot.lote.quantidadeInicial, LotCalculator.deadCount(lot.movements))?.let {
                AlertItem(tanque.id, tanque.nome, lot.lote.id, AlertKind.MORTALITY, lot.mortalityPct, it, null)
            }
        }
        return water + listOfNotNull(phVariation) + mortality
    }

    /** Pior nível de alerta do tanque, ou `null` se não há nenhum alerta. */
    fun worstLevel(tank: TankOverview, ranges: WaterRanges = WaterRanges.DEFAULT): AlertLevel? =
        alertsFor(tank, ranges).maxOfOrNull { it.status.level }

    /**
     * Nível que resume o tanque na lista: o pior entre as leituras de água (inclusive ADEQUADO) e os
     * alertas compostos. `null` se o tanque não tem nenhuma medição.
     */
    fun overallLevel(tank: TankOverview, ranges: WaterRanges = WaterRanges.DEFAULT): AlertLevel? =
        listOfNotNull(WaterReadingsEvaluator.evaluate(tank.readings, ranges).worstLevel, worstLevel(tank, ranges)).maxOrNull()

    /** Todos os alertas da fazenda: EMERGENCIA primeiro; no mesmo nível, por tanque e depois por tipo. */
    fun prioritized(farm: FarmOverview): List<AlertItem> = farm.tanks
        .flatMap { alertsFor(it, farm.waterRanges) }
        .sortedWith(
            compareByDescending<AlertItem> { it.status.level }
                .thenBy { it.tankName }
                .thenBy { it.kind.ordinal },
        )
}
