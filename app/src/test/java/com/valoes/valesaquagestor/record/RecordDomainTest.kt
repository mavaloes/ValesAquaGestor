package com.valoes.valesaquagestor.record

import com.valoes.valesaquagestor.domain.calc.BiometryCalculator
import com.valoes.valesaquagestor.domain.calc.MomentSuggester
import com.valoes.valesaquagestor.domain.calc.Weighing
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.rules.Plausibility
import com.valoes.valesaquagestor.domain.rules.PlausibilityWarning
import com.valoes.valesaquagestor.presentation.util.FormError
import com.valoes.valesaquagestor.presentation.util.FormValidation
import com.valoes.valesaquagestor.presentation.util.SaveFeedback
import com.valoes.valesaquagestor.presentation.util.formatInput
import com.valoes.valesaquagestor.presentation.util.parseDecimal
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordDomainTest {

    // ---- momento sugerido (specs/08, conflito 14) ----

    @Test
    fun `o momento acompanha a hora do registro`() {
        val expected = listOf(
            LocalTime.of(0, 0) to MomentoMedicao.MANHA,
            LocalTime.of(6, 10) to MomentoMedicao.MANHA,
            LocalTime.of(10, 59) to MomentoMedicao.MANHA,
            LocalTime.of(11, 0) to MomentoMedicao.MEIO_DIA,
            LocalTime.of(13, 59) to MomentoMedicao.MEIO_DIA,
            LocalTime.of(14, 0) to MomentoMedicao.TARDE,
            LocalTime.of(17, 0) to MomentoMedicao.TARDE,
            LocalTime.of(18, 59) to MomentoMedicao.TARDE,
            LocalTime.of(19, 0) to MomentoMedicao.NOITE,
            LocalTime.of(23, 59) to MomentoMedicao.NOITE,
        )
        expected.forEach { (time, moment) -> assertEquals("$time", moment, MomentSuggester.suggest(time)) }
    }

    // ---- valor improvável (specs/08, conflito 15) ----

    @Test
    fun `pH acima de 10 e oxigenio acima de 15 avisam, no limite nao`() {
        assertTrue(Plausibility.checkWater(ph = 10.0, oxygenMgL = 15.0).isEmpty())
        assertEquals(listOf(PlausibilityWarning.PH_VERY_HIGH), Plausibility.checkWater(ph = 10.1, oxygenMgL = null))
        assertEquals(listOf(PlausibilityWarning.OXYGEN_SUPERSATURATED), Plausibility.checkWater(ph = null, oxygenMgL = 15.1))
        assertEquals(2, Plausibility.checkWater(ph = 12.0, oxygenMgL = 20.0).size)
        assertTrue(Plausibility.checkWater(ph = null, oxygenMgL = null).isEmpty())
    }

    @Test
    fun `peso medio 10 vezes maior avisa, abaixo disso nao`() {
        assertNull(Plausibility.checkAverageWeight(newAverageG = 199.9, currentAverageG = 20.0))
        assertEquals(PlausibilityWarning.AVERAGE_WEIGHT_JUMP, Plausibility.checkAverageWeight(200.0, 20.0))
        assertNull("sem peso de referência não avisa", Plausibility.checkAverageWeight(500.0, 0.0))
    }

    @Test
    fun `pH so existe de 0 a 14`() {
        assertTrue(Plausibility.isPhOnScale(0.0))
        assertTrue(Plausibility.isPhOnScale(14.0))
        assertTrue(!Plausibility.isPhOnScale(-0.1))
        assertTrue(!Plausibility.isPhOnScale(14.1))
    }

    // ---- biometria ----

    @Test
    fun `peso medio vem da soma das pesagens sem a tara`() {
        // (6 - 1) + (5,5 - 1,5) = 9 kg em 40 peixes -> 225 g.
        val summary = BiometryCalculator.summarize(listOf(Weighing(6.0, 1.0, 20), Weighing(5.5, 1.5, 20)))!!

        assertEquals(9.0, summary.netWeightKg, 1e-9)
        assertEquals(40, summary.fishCount)
        assertEquals(225.0, summary.averageWeightG, 1e-9)
    }

    @Test
    fun `sem peixes ou sem peso liquido nao ha peso medio`() {
        assertNull(BiometryCalculator.summarize(emptyList()))
        assertNull(BiometryCalculator.summarize(listOf(Weighing(5.0, 1.0, 0))))
        assertNull(BiometryCalculator.summarize(listOf(Weighing(1.0, 1.0, 10))))
    }

    @Test
    fun `amostra recomendada e 10 por cento ate 2000 peixes e 200 acima`() {
        assertEquals(100, BiometryCalculator.recommendedSample(1000))
        assertEquals(101, BiometryCalculator.recommendedSample(1001)) // arredonda para cima
        assertEquals(1, BiometryCalculator.recommendedSample(1))
        assertEquals(0, BiometryCalculator.recommendedSample(0))
        assertEquals(200, BiometryCalculator.recommendedSample(2000))
        assertEquals(200, BiometryCalculator.recommendedSample(2001))
        assertEquals(200, BiometryCalculator.recommendedSample(50_000))
    }

    // ---- validação ----

    @Test
    fun `medida pode ser zero mas nao negativa`() {
        assertNull(FormValidation.nonNegativeDecimal("0"))
        assertNull(FormValidation.nonNegativeDecimal(""))
        assertEquals(FormError.REQUIRED, FormValidation.nonNegativeDecimal("", required = true))
        assertEquals(FormError.NEGATIVE, FormValidation.nonNegativeDecimal("-0,1"))
        assertEquals(FormError.INVALID_NUMBER, FormValidation.nonNegativeDecimal("x"))
    }

    @Test
    fun `validacao do pH`() {
        assertNull(FormValidation.ph(""))
        assertNull(FormValidation.ph("0"))
        assertNull(FormValidation.ph("14"))
        assertNull(FormValidation.ph("10,5"))
        assertEquals(FormError.PH_OUT_OF_SCALE, FormValidation.ph("14,1"))
        assertEquals(FormError.PH_OUT_OF_SCALE, FormValidation.ph("-1"))
        assertEquals(FormError.INVALID_NUMBER, FormValidation.ph("abc"))
    }

    @Test
    fun `nao se retira mais peixes do que existem vivos`() {
        assertNull(FormValidation.wholeNumberAtMost("960", max = 960))
        assertEquals(FormError.EXCEEDS_ALIVE, FormValidation.wholeNumberAtMost("961", max = 960))
        assertEquals(FormError.NOT_POSITIVE, FormValidation.wholeNumberAtMost("0", max = 960))
        assertEquals(FormError.REQUIRED, FormValidation.wholeNumberAtMost("", max = 960))
        assertEquals(FormError.INVALID_NUMBER, FormValidation.wholeNumberAtMost("1,5", max = 960))
    }

    // ---- avisos de salvamento e formatação de campo ----

    @Test
    fun `o aviso depende da conexao`() {
        assertEquals(SaveFeedback.SAVED, SaveFeedback.forConnection(isOnline = true))
        assertEquals(SaveFeedback.SAVED_OFFLINE, SaveFeedback.forConnection(isOnline = false))
    }

    @Test
    fun `numero que preenche um campo e lido de volta`() {
        listOf(0.3225 to 3, 1234.5 to 2, 15.81 to 2, 36.0 to 1).forEach { (value, decimals) ->
            val text = formatInput(value, decimals)
            assertEquals(text, value, parseDecimal(text)!!, 0.0006)
            assertTrue("sem separador de milhar: $text", !text.contains('.'))
        }
        assertEquals("36", formatInput(36.0, 1))
        assertEquals("1234,5", formatInput(1234.5, 2))
    }
}
