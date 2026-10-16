package com.valoes.valesaquagestor.farm

import com.valoes.valesaquagestor.domain.calc.PondLoadChecker
import com.valoes.valesaquagestor.domain.rules.CompositeAlerts
import com.valoes.valesaquagestor.domain.rules.WaterEvaluator
import com.valoes.valesaquagestor.domain.rules.WaterStatus
import com.valoes.valesaquagestor.presentation.components.actionRes
import com.valoes.valesaquagestor.presentation.components.messageRes
import com.valoes.valesaquagestor.presentation.util.FormError
import com.valoes.valesaquagestor.presentation.util.FormValidation
import com.valoes.valesaquagestor.presentation.util.formatDate
import com.valoes.valesaquagestor.presentation.util.formatDateTime
import com.valoes.valesaquagestor.presentation.util.formatNumber
import com.valoes.valesaquagestor.presentation.util.parseDecimal
import com.valoes.valesaquagestor.presentation.util.parseWholeNumber
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UtilAndTextsTest {

    // ---- Todo alerta que o domínio produz tem texto em strings.xml ----

    @Test
    fun `toda chave de alerta do dominio tem texto de significado e de acao`() {
        val statuses = mutableListOf<WaterStatus>()
        listOf(1.0, 2.0, 3.0, 4.0).forEach { statuses += WaterEvaluator.evaluateOxygen(it) }
        listOf(3.0, 5.0, 6.0, 7.0, 8.6, 9.5, 12.0).forEach { statuses += WaterEvaluator.evaluatePh(it) }
        listOf(10.0, 20.0, 28.0, 33.0).forEach { statuses += WaterEvaluator.evaluateTemperature(it) }
        listOf(0.01, 0.1, 0.3, 0.8).forEach { statuses += WaterEvaluator.evaluateToxicAmmonia(it) }
        listOf(0.01, 0.1, 0.5, 1.0).forEach { statuses += WaterEvaluator.evaluateNitrite(it) }
        listOf(10.0, 25.0, 35.0, 50.0, 70.0).forEach { statuses += WaterEvaluator.evaluateTransparency(it) }
        listOf(5.0, 15.0, 30.0).forEach { statuses += WaterEvaluator.evaluateAlkalinity(it) }
        listOf(10.0, 30.0).forEach { statuses += WaterEvaluator.evaluateHardness(it) }
        statuses += CompositeAlerts.phDailyVariation(6.8, 9.0)!!
        statuses += CompositeAlerts.accumulatedMortality(1000, 200)!!
        statuses += PondLoadChecker.check(dailyFeedKg = 6.1, areaM2 = 1000.0).status!!

        statuses.forEach {
            // `messageRes`/`actionRes` lançam erro se a chave não estiver cadastrada.
            assertNotEquals(it.messageKey, 0, it.messageRes())
            assertNotEquals(it.actionKey, 0, it.actionRes())
        }
    }

    // ---- Números digitados ----

    @Test
    fun `aceita virgula ou ponto como separador decimal`() {
        assertEquals(10.5, parseDecimal("10,5")!!, 0.0)
        assertEquals(10.5, parseDecimal("10.5")!!, 0.0)
        assertEquals(1000.0, parseDecimal("  1000 ")!!, 0.0)
    }

    @Test
    fun `texto que nao e numero finito vira null`() {
        listOf("", "abc", "10,5,2", "NaN", "Infinity", "1e999").forEach { assertNull(it, parseDecimal(it)) }
        assertNull(parseWholeNumber("10,5"))
        assertEquals(1000, parseWholeNumber(" 1000 "))
    }

    @Test
    fun `formata no padrao brasileiro`() {
        assertEquals("1.234,5", formatNumber(1234.5, 1))
        assertEquals("0,012", formatNumber(0.0118, 3))
        assertEquals("3,0", formatNumber(3.0, 1))
        assertEquals("16/10/2026", formatDate(LocalDate.of(2026, 10, 16)))
        assertEquals("16/10 06:10", formatDateTime(java.time.Instant.parse("2026-10-16T09:10:00Z"), ZoneOffset.ofHours(-3)))
    }

    // ---- Validação ----

    @Test
    fun `validacao de campos de formulario`() {
        assertNull(FormValidation.requiredText("a"))
        assertEquals(FormError.REQUIRED, FormValidation.requiredText("  "))

        assertEquals(FormError.REQUIRED, FormValidation.positiveDecimal("", required = true))
        assertNull(FormValidation.positiveDecimal("", required = false))
        assertEquals(FormError.INVALID_NUMBER, FormValidation.positiveDecimal("x", required = false))
        assertEquals(FormError.NOT_POSITIVE, FormValidation.positiveDecimal("0", required = true))
        assertNull(FormValidation.positiveDecimal("0,1", required = true))

        assertEquals(FormError.NOT_POSITIVE, FormValidation.positiveWholeNumber("0", required = true))
        assertEquals(FormError.INVALID_NUMBER, FormValidation.positiveWholeNumber("1,5", required = true))
        assertNull(FormValidation.positiveWholeNumber("10", required = true))
    }

    @Test
    fun `percentual vai de 0 a 100 inclusive`() {
        assertNull(FormValidation.percent("0"))
        assertNull(FormValidation.percent("100"))
        assertNull(FormValidation.percent("5,5"))
        assertEquals(FormError.OUT_OF_RANGE_PERCENT, FormValidation.percent("100,1"))
        assertEquals(FormError.OUT_OF_RANGE_PERCENT, FormValidation.percent("-1"))
        assertEquals(FormError.REQUIRED, FormValidation.percent(""))
        assertEquals(FormError.INVALID_NUMBER, FormValidation.percent("abc"))
    }
}
