package com.valoes.valesaquagestor.farm

import com.valoes.valesaquagestor.domain.model.LatestWaterReadings
import com.valoes.valesaquagestor.domain.model.Reading
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.AlertLevel
import com.valoes.valesaquagestor.domain.rules.WaterReadingsEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WaterReadingsEvaluatorTest {

    @Test
    fun `sem leituras nao ha avaliacao`() {
        val report = WaterReadingsEvaluator.evaluate(LatestWaterReadings())

        assertTrue(report.alerts.isEmpty())
        assertFalse(report.ammoniaNeedsPhAndTemperature)
        assertNull(report.worstLevel)
    }

    @Test
    fun `cada parametro medido e avaliado na ordem fixa`() {
        val readings = LatestWaterReadings(
            oxigenioMgL = Reading(2.5, NOW),
            ph = Reading(7.4, NOW),
            temperaturaC = Reading(28.0, NOW),
            nitritoMgL = Reading(0.5, NOW),
            transparenciaCm = Reading(35.0, NOW),
            alcalinidadeMgL = Reading(15.0, NOW),
            durezaMgL = Reading(10.0, NOW),
        )

        val report = WaterReadingsEvaluator.evaluate(readings)

        assertEquals(
            listOf(
                AlertKind.OXYGEN to AlertLevel.CRITICO,
                AlertKind.TEMPERATURE to AlertLevel.ADEQUADO,
                AlertKind.PH to AlertLevel.ADEQUADO,
                AlertKind.NITRITE to AlertLevel.CRITICO,
                AlertKind.TRANSPARENCY to AlertLevel.ADEQUADO,
                AlertKind.ALKALINITY to AlertLevel.ATENCAO,
                AlertKind.HARDNESS to AlertLevel.ATENCAO,
            ),
            report.alerts.map { it.kind to it.status.level },
        )
        assertEquals(AlertLevel.CRITICO, report.worstLevel)
    }

    @Test
    fun `amonia total vira toxica com pH e temperatura recentes`() {
        val readings = LatestWaterReadings(
            amoniaTotalMgL = Reading(1.5, NOW),
            ph = Reading(7.0, NOW),
            temperaturaC = Reading(30.0, NOW),
        )

        val report = WaterReadingsEvaluator.evaluate(readings)

        val toxic = report.alerts.first { it.kind == AlertKind.TOXIC_AMMONIA }
        assertEquals(0.01185, toxic.value, 0.0002)
        assertEquals(AlertLevel.ADEQUADO, toxic.status.level)
        assertFalse(report.ammoniaNeedsPhAndTemperature)
    }

    @Test
    fun `a mesma amonia total com pH 9 e emergencia`() {
        val readings = LatestWaterReadings(
            amoniaTotalMgL = Reading(1.5, NOW),
            ph = Reading(9.0, NOW),
            temperaturaC = Reading(30.0, NOW),
        )

        val toxic = WaterReadingsEvaluator.evaluate(readings).alerts.first { it.kind == AlertKind.TOXIC_AMMONIA }

        assertEquals(AlertLevel.EMERGENCIA, toxic.status.level)
    }

    @Test
    fun `amonia sem pH ou sem temperatura nao e comparada com limite e avisa`() {
        val withoutPh = WaterReadingsEvaluator.evaluate(
            LatestWaterReadings(amoniaTotalMgL = Reading(1.5, NOW), temperaturaC = Reading(30.0, NOW)),
        )
        val withoutTemperature = WaterReadingsEvaluator.evaluate(
            LatestWaterReadings(amoniaTotalMgL = Reading(1.5, NOW), ph = Reading(7.0, NOW)),
        )

        listOf(withoutPh, withoutTemperature).forEach {
            assertTrue(it.alerts.none { alert -> alert.kind == AlertKind.TOXIC_AMMONIA })
            assertTrue(it.ammoniaNeedsPhAndTemperature)
        }
    }

    @Test
    fun `pH medido ate 24 horas da amonia vale e depois disso nao`() {
        fun report(phAge: Long) = WaterReadingsEvaluator.evaluate(
            LatestWaterReadings(
                amoniaTotalMgL = Reading(1.5, NOW),
                ph = Reading(7.0, hoursAgo(phAge)),
                temperaturaC = Reading(30.0, NOW),
            ),
        )

        assertFalse(report(24).ammoniaNeedsPhAndTemperature)
        assertTrue(report(25).ammoniaNeedsPhAndTemperature)
    }

    @Test
    fun `cada avaliacao guarda quando a leitura foi feita`() {
        val report = WaterReadingsEvaluator.evaluate(LatestWaterReadings(oxigenioMgL = Reading(5.0, hoursAgo(3))))

        assertEquals(hoursAgo(3), report.alerts.single().measuredAt)
    }
}
