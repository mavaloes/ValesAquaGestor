package com.valoes.valesaquagestor.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Casos E5 e F5 de `specs/09-casos-de-teste.md` e os limites da tabela de `03`, seção 2. */
class WaterEvaluatorTest {

    private fun assertLevels(expected: List<AlertLevel>, values: List<Double>, evaluate: (Double) -> WaterStatus) {
        values.zip(expected).forEach { (value, level) ->
            assertEquals("valor $value", level, evaluate(value).level)
        }
    }

    @Test
    fun `E5 niveis de oxigenio`() {
        assertLevels(
            listOf(
                AlertLevel.ADEQUADO, AlertLevel.ATENCAO, AlertLevel.ATENCAO,
                AlertLevel.CRITICO, AlertLevel.CRITICO, AlertLevel.EMERGENCIA,
            ),
            listOf(4.0, 3.99, 3.0, 2.99, 2.0, 1.99),
            WaterEvaluator::evaluateOxygen,
        )
    }

    @Test
    fun `F5 niveis de pH`() {
        assertLevels(
            listOf(
                AlertLevel.ADEQUADO, AlertLevel.ADEQUADO, AlertLevel.ATENCAO, AlertLevel.ATENCAO,
                AlertLevel.CRITICO, AlertLevel.CRITICO, AlertLevel.CRITICO, AlertLevel.EMERGENCIA,
            ),
            listOf(6.5, 8.5, 8.51, 9.0, 9.01, 4.0, 11.0, 11.01),
            WaterEvaluator::evaluatePh,
        )
    }

    @Test
    fun `pH abaixo de 6_5 usa a chave de pH baixo`() {
        assertEquals(AlertLevel.ATENCAO, WaterEvaluator.evaluatePh(6.0).level)
        assertEquals("water_ph_low_atencao", WaterEvaluator.evaluatePh(6.0).messageKey)
        assertEquals(AlertLevel.CRITICO, WaterEvaluator.evaluatePh(5.99).level)
        assertEquals(AlertLevel.EMERGENCIA, WaterEvaluator.evaluatePh(3.99).level)
        assertEquals("water_ph_high_atencao", WaterEvaluator.evaluatePh(8.6).messageKey)
    }

    @Test
    fun `chaves seguem o padrao parametro_nivel`() {
        val status = WaterEvaluator.evaluateOxygen(2.5)
        assertEquals("water_oxygen_critico", status.messageKey)
        assertEquals("action_water_oxygen_critico", status.actionKey)
    }

    @Test
    fun `niveis de temperatura`() {
        assertLevels(
            listOf(
                AlertLevel.CRITICO, AlertLevel.ATENCAO, AlertLevel.ATENCAO, AlertLevel.ADEQUADO,
                AlertLevel.ADEQUADO, AlertLevel.CRITICO,
            ),
            listOf(15.9, 16.0, 25.9, 26.0, 32.0, 32.1),
            WaterEvaluator::evaluateTemperature,
        )
    }

    @Test
    fun `niveis de amonia toxica`() {
        assertLevels(
            listOf(AlertLevel.ADEQUADO, AlertLevel.ATENCAO, AlertLevel.ATENCAO, AlertLevel.CRITICO, AlertLevel.CRITICO, AlertLevel.EMERGENCIA),
            listOf(0.049, 0.05, 0.199, 0.2, 0.499, 0.5),
            WaterEvaluator::evaluateToxicAmmonia,
        )
    }

    @Test
    fun `amonia sem pH ou temperatura devolve dado insuficiente`() {
        assertNull(WaterEvaluator.evaluateAmmonia(1.5, ph = null, celsius = 30.0))
        assertNull(WaterEvaluator.evaluateAmmonia(1.5, ph = 7.0, celsius = null))
    }

    @Test
    fun `A2 mesma amonia total com pH 9 e emergencia`() {
        assertEquals(AlertLevel.EMERGENCIA, WaterEvaluator.evaluateAmmonia(1.5, 9.0, 30.0)?.level)
        assertEquals(AlertLevel.ADEQUADO, WaterEvaluator.evaluateAmmonia(1.5, 7.0, 30.0)?.level)
        assertNotNull(WaterEvaluator.evaluateAmmonia(1.5, 7.0, 30.0))
    }

    @Test
    fun `niveis de nitrito`() {
        assertLevels(
            listOf(AlertLevel.ADEQUADO, AlertLevel.ATENCAO, AlertLevel.ATENCAO, AlertLevel.CRITICO, AlertLevel.CRITICO, AlertLevel.EMERGENCIA),
            listOf(0.029, 0.03, 0.299, 0.3, 0.699, 0.7),
            WaterEvaluator::evaluateNitrite,
        )
    }

    @Test
    fun `niveis de transparencia`() {
        assertLevels(
            listOf(
                AlertLevel.CRITICO, AlertLevel.ATENCAO, AlertLevel.ADEQUADO, AlertLevel.ADEQUADO,
                AlertLevel.ATENCAO, AlertLevel.ATENCAO, AlertLevel.CRITICO,
            ),
            listOf(19.9, 20.0, 30.0, 45.0, 45.1, 60.0, 60.1),
            WaterEvaluator::evaluateTransparency,
        )
    }

    @Test
    fun `niveis de alcalinidade e dureza`() {
        assertLevels(
            listOf(AlertLevel.CRITICO, AlertLevel.ATENCAO, AlertLevel.ATENCAO, AlertLevel.ADEQUADO),
            listOf(9.9, 10.0, 19.9, 20.0),
            WaterEvaluator::evaluateAlkalinity,
        )
        assertLevels(
            listOf(AlertLevel.ATENCAO, AlertLevel.ADEQUADO),
            listOf(19.9, 20.0),
            WaterEvaluator::evaluateHardness,
        )
    }
}
