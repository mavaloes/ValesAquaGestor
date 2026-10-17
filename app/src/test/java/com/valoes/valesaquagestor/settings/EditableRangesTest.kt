package com.valoes.valesaquagestor.settings

import com.valoes.valesaquagestor.domain.calc.AlertAggregator
import com.valoes.valesaquagestor.domain.calc.FeedBlockReason
import com.valoes.valesaquagestor.domain.calc.FeedingCalculator
import com.valoes.valesaquagestor.domain.calc.MonitoringSchedule
import com.valoes.valesaquagestor.domain.calc.ScheduleField
import com.valoes.valesaquagestor.domain.calc.WaterReadings
import com.valoes.valesaquagestor.domain.model.LatestWaterReadings
import com.valoes.valesaquagestor.domain.model.Reading
import com.valoes.valesaquagestor.domain.model.Tanque
import com.valoes.valesaquagestor.domain.model.TipoTanque
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.AlertLevel
import com.valoes.valesaquagestor.domain.rules.OxygenThresholds
import com.valoes.valesaquagestor.domain.rules.PhThresholds
import com.valoes.valesaquagestor.domain.rules.WaterEvaluator
import com.valoes.valesaquagestor.domain.rules.WaterRangeField
import com.valoes.valesaquagestor.domain.rules.WaterRanges
import com.valoes.valesaquagestor.domain.usecase.FarmOverview
import com.valoes.valesaquagestor.domain.usecase.TankOverview
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Faixas da água e agenda editáveis: os padrões continuam sendo as constantes com fonte, a validação
 * só exige ordem (sem inventar limites) e as faixas ajustadas chegam aos avaliadores.
 */
class EditableRangesTest {

    // ---- Padrões = constantes com fonte ----

    @Test
    fun `os padroes sao exatamente as constantes com fonte`() {
        val default = WaterRanges.DEFAULT
        assertEquals(OxygenThresholds.ADEQUATE_MIN_MG_L, default.oxygenAdequateMinMgL, 0.0)
        assertEquals(OxygenThresholds.CRITICAL_BELOW_MG_L, default.oxygenCriticalBelowMgL, 0.0)
        assertEquals(OxygenThresholds.EMERGENCY_BELOW_MG_L, default.oxygenEmergencyBelowMgL, 0.0)
        assertEquals(PhThresholds.ADEQUATE_MIN, default.phAdequateMin, 0.0)
        assertEquals(PhThresholds.CRITICAL_MAX, default.phCriticalMax, 0.0)
    }

    @Test
    fun `as faixas de fabrica sao validas`() {
        assertTrue(WaterRanges.DEFAULT.invalidFields().isEmpty())
        assertTrue(MonitoringSchedule().invalidFields().isEmpty())
    }

    @Test
    fun `valueOf e with cobrem todos os campos`() {
        WaterRangeField.entries.forEach { field ->
            assertEquals(field.name, 12.5, WaterRanges.DEFAULT.with(field, 12.5).valueOf(field), 0.0)
        }
        ScheduleField.entries.forEach { field ->
            assertEquals(field.name, 7, MonitoringSchedule().with(field, 7).valueOf(field))
        }
    }

    // ---- Validação das faixas ----

    @Test
    fun `oxigenio fora de ordem marca o campo que quebrou a ordem`() {
        val invalid = WaterRanges(oxygenCriticalBelowMgL = 1.0, oxygenEmergencyBelowMgL = 2.0).invalidFields()

        assertEquals(setOf(WaterRangeField.OXYGEN_CRITICAL_BELOW), invalid)
    }

    @Test
    fun `valor zero, negativo ou nao numerico e recusado`() {
        assertEquals(
            setOf(WaterRangeField.HARDNESS_ADEQUATE_MIN),
            WaterRanges(hardnessAdequateMinMgL = 0.0).invalidFields(),
        )
        assertEquals(
            setOf(WaterRangeField.OXYGEN_EMERGENCY_BELOW),
            WaterRanges(oxygenEmergencyBelowMgL = -1.0).invalidFields(),
        )
        assertTrue(WaterRanges(hardnessAdequateMinMgL = Double.NaN).invalidFields().isNotEmpty())
    }

    @Test
    fun `o pH nao passa de 14`() {
        val invalid = WaterRanges(phCriticalMax = 15.0).invalidFields()

        assertEquals(setOf(WaterRangeField.PH_CRITICAL_MAX), invalid)
    }

    @Test
    fun `a ordem do pH vai de emergencia baixa a emergencia alta`() {
        val invalid = WaterRanges(phAdequateMax = 6.0).invalidFields()

        assertTrue(WaterRangeField.PH_ADEQUATE_MAX in invalid)
    }

    // ---- Validação da agenda ----

    @Test
    fun `agenda com hora fora de 0 a 23, manha depois da tarde ou intervalo menor que 1 dia e recusada`() {
        assertEquals(
            setOf(ScheduleField.OXYGEN_AFTERNOON_HOUR),
            MonitoringSchedule(oxygenAfternoonHour = 24).invalidFields(),
        )
        assertEquals(
            setOf(ScheduleField.OXYGEN_AFTERNOON_HOUR),
            MonitoringSchedule(oxygenMorningHour = 18, oxygenAfternoonHour = 17).invalidFields(),
        )
        assertEquals(
            setOf(ScheduleField.TRANSPARENCY_TO_HOUR),
            MonitoringSchedule(transparencyFromHour = 12, transparencyToHour = 12).invalidFields(),
        )
        assertEquals(
            setOf(ScheduleField.ENGORDA_BIOMETRY_INTERVAL),
            MonitoringSchedule(engordaBiometryIntervalDays = 0).invalidFields(),
        )
    }

    // ---- As faixas ajustadas chegam aos avaliadores ----

    private val strictOxygen = WaterRanges(oxygenAdequateMinMgL = 5.0, oxygenCriticalBelowMgL = 4.0)

    @Test
    fun `o avaliador usa a faixa ajustada e, sem ajuste, a faixa de fabrica`() {
        assertEquals(AlertLevel.ADEQUADO, WaterEvaluator.evaluateOxygen(4.5).level)
        assertEquals(AlertLevel.ATENCAO, WaterEvaluator.evaluateOxygen(4.5, strictOxygen).level)
        assertEquals(AlertLevel.CRITICO, WaterEvaluator.evaluateOxygen(3.5, strictOxygen).level)
    }

    @Test
    fun `a trava da racao acompanha as faixas ajustadas`() {
        val water = WaterReadings(oxygenMgL = 3.5, temperatureC = 27.0)

        val byDefault = FeedingCalculator.suggest(1000, 200.0, water)!!
        val strict = FeedingCalculator.suggest(1000, 200.0, water, strictOxygen)!!

        assertTrue(byDefault.blockReasons.isEmpty())
        assertEquals(listOf(FeedBlockReason.LOW_OXYGEN), strict.blockReasons)
        assertEquals(0.0, strict.dailyKg, 0.0)
    }

    @Test
    fun `os alertas da fazenda usam as faixas do retrato da fazenda`() {
        val tank = TankOverview(
            tanque = Tanque(id = 1, nome = "Viveiro 1", tipo = TipoTanque.ESCAVADO, areaM2 = 1000.0, usaAerador = false, renovacaoAguaPctDia = 0.0),
            lots = emptyList(),
            readings = LatestWaterReadings(oxigenioMgL = Reading(4.5, Instant.ofEpochMilli(1_000L))),
            waterToday = emptyList(),
        )

        val byDefault = AlertAggregator.prioritized(FarmOverview(listOf(tank)))
        val strict = AlertAggregator.prioritized(FarmOverview(listOf(tank), waterRanges = strictOxygen))

        assertTrue(byDefault.isEmpty())
        val alert = strict.single()
        assertEquals(AlertKind.OXYGEN, alert.kind)
        assertEquals(AlertLevel.ATENCAO, alert.status.level)
        assertNull(AlertAggregator.worstLevel(tank))
        assertEquals(AlertLevel.ATENCAO, AlertAggregator.worstLevel(tank, strictOxygen))
    }
}
