package com.valoes.valesaquagestor.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Casos B1 a B5 e C1 a C3 de `specs/09-casos-de-teste.md` (o C4 é de uma fase seguinte). */
class FeedingCalculatorTest {

    private val tolerance = 1e-6

    @Test
    fun `B1 500 peixes de 43 g`() {
        val suggestion = FeedingCalculator.suggest(500, 43.0)!!
        assertEquals(21.5, FeedingCalculator.biomassKg(500, 43.0), tolerance)
        assertEquals(20.0, suggestion.band.minG, 0.0)
        assertEquals(4.5, suggestion.band.pctBodyWeight, 0.0)
        assertEquals(0.9675, suggestion.dailyKg, tolerance)
        assertEquals(3, suggestion.band.meals)
        assertEquals(0.3225, suggestion.perMealKg, tolerance)
    }

    @Test
    fun `B2 fator em 23 graus e 0_8`() {
        assertEquals(0.8, FeedingCalculator.temperatureFactor(23.0), 0.0)
        assertEquals(6.4, 8.0 * FeedingCalculator.temperatureFactor(23.0), tolerance)
    }

    @Test
    fun `B3 temperaturas limite`() {
        val expected = listOf(15.9 to 0.0, 16.0 to 0.6, 20.0 to 0.8, 25.0 to 1.0, 30.0 to 0.8, 32.0 to 0.8, 32.1 to 0.0)
        expected.forEach { (celsius, factor) ->
            assertEquals("$celsius °C", factor, FeedingCalculator.temperatureFactor(celsius), 0.0)
        }
    }

    @Test
    fun `B4 peso exatamente 50 g cai na faixa de 50 a 150`() {
        val band = TilapiaFeedTable.bandFor(50.0)!!
        assertEquals(50.0, band.minG, 0.0)
        assertEquals(3.4, band.pctBodyWeight, 0.0)
        assertEquals(20.0, TilapiaFeedTable.bandFor(49.99)!!.minG, 0.0)
    }

    @Test
    fun `peso fora da tabela devolve null`() {
        assertNull(TilapiaFeedTable.bandFor(0.9))
        assertNull(TilapiaFeedTable.bandFor(1800.0))
        assertNull(FeedingCalculator.suggest(100, 2000.0))
        assertNotNull(TilapiaFeedTable.bandFor(1799.9))
    }

    @Test
    fun `B5 oxigenio 2_9 zera a racao`() {
        val suggestion = FeedingCalculator.suggest(500, 43.0, WaterReadings(oxygenMgL = 2.9, temperatureC = 27.0))!!
        assertEquals(0.0, suggestion.dailyKg, 0.0)
        assertEquals(0.0, suggestion.perMealKg, 0.0)
        assertEquals(listOf(FeedBlockReason.LOW_OXYGEN), suggestion.blockReasons)
    }

    @Test
    fun `B5 oxigenio 3_0 e permitido com aviso`() {
        val suggestion = FeedingCalculator.suggest(500, 43.0, WaterReadings(oxygenMgL = 3.0, temperatureC = 27.0))!!
        assertTrue(suggestion.blockReasons.isEmpty())
        assertEquals(0.9675, suggestion.dailyKg, tolerance)
        assertEquals(listOf(FeedWarning.OXYGEN_ATTENTION), suggestion.warnings)
    }

    @Test
    fun `travas de temperatura amonia toxica e nitrito`() {
        val cold = FeedingCalculator.suggest(500, 43.0, WaterReadings(temperatureC = 15.9))!!
        assertEquals(listOf(FeedBlockReason.TEMPERATURE), cold.blockReasons)
        val ammonia = FeedingCalculator.suggest(500, 43.0, WaterReadings(temperatureC = 27.0, toxicAmmoniaMgL = 0.2))!!
        assertEquals(listOf(FeedBlockReason.TOXIC_AMMONIA), ammonia.blockReasons)
        val nitrite = FeedingCalculator.suggest(500, 43.0, WaterReadings(temperatureC = 27.0, nitriteMgL = 0.3))!!
        assertEquals(listOf(FeedBlockReason.NITRITE), nitrite.blockReasons)
        val ok = FeedingCalculator.suggest(500, 43.0, WaterReadings(temperatureC = 27.0, toxicAmmoniaMgL = 0.199, nitriteMgL = 0.299))!!
        assertTrue(ok.blockReasons.isEmpty())
    }

    @Test
    fun `fator de temperatura reduz a racao e a falta de temperatura gera aviso`() {
        val cool = FeedingCalculator.suggest(500, 43.0, WaterReadings(temperatureC = 22.0))!!
        assertEquals(0.9675 * 0.8, cool.dailyKg, tolerance)
        val unknown = FeedingCalculator.suggest(500, 43.0)!!
        assertEquals(0.9675, unknown.dailyKg, tolerance)
        assertEquals(listOf(FeedWarning.TEMPERATURE_MISSING), unknown.warnings)
    }

    @Test
    fun `C1 reajuste semanal`() {
        val adjustment = FeedingCalculator.weeklyAdjustment(fishCount = 5000, avgWeightG = 259.0, feedOfferedKg = 199.43)!!
        assertEquals(1295.0, FeedingCalculator.biomassKg(5000, 259.0), 0.005)
        assertEquals(142.45, adjustment.weightGainKg, 0.005)
        assertEquals(1437.45, adjustment.adjustedBiomassKg, 0.005)
        assertEquals(287.5, adjustment.adjustedAvgWeightG, 0.05)
    }

    @Test
    fun `C2 continuacao do C1 na faixa de 250 a 400 g`() {
        val adjustment = FeedingCalculator.weeklyAdjustment(5000, 259.0, 199.43)!!
        val suggestion = FeedingCalculator.suggest(5000, adjustment.adjustedAvgWeightG, WaterReadings(temperatureC = 27.0))!!
        assertEquals(2.2, suggestion.band.pctBodyWeight, 0.0)
        assertEquals(2, suggestion.band.meals)
        assertEquals(31.62, suggestion.dailyKg, 0.005)
        assertEquals(15.81, suggestion.perMealKg, 0.005)
    }

    @Test
    fun `C3 ganho de peso e TCA real`() {
        val gain = FeedingCalculator.weightGainKg(fishCount = 500, previousAvgWeightG = 200.0, currentAvgWeightG = 450.0)
        assertEquals(125.0, gain, tolerance)
        assertEquals(1.52, FeedingCalculator.realFcr(190.0, gain)!!, 0.005)
    }

    @Test
    fun `TCA sem ganho e GPD`() {
        assertNull(FeedingCalculator.realFcr(10.0, 0.0))
        assertNull(FeedingCalculator.dailyGainG(100.0, 130.0, 0))
        assertEquals(2.0, FeedingCalculator.dailyGainG(100.0, 130.0, 15)!!, tolerance)
    }
}
