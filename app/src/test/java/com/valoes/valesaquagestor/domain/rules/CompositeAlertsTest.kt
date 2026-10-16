package com.valoes.valesaquagestor.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Casos F3 e F4 de `specs/09-casos-de-teste.md`. */
class CompositeAlertsTest {

    @Test
    fun `F3 mortalidade de 10_1 por cento alerta`() {
        val status = CompositeAlerts.accumulatedMortality(initialCount = 1000, deadCount = 101)
        assertEquals(AlertLevel.ATENCAO, status?.level)
    }

    @Test
    fun `mortalidade de exatamente 10 por cento nao alerta`() {
        assertNull(CompositeAlerts.accumulatedMortality(initialCount = 1000, deadCount = 100))
    }

    @Test
    fun `lote sem peixes iniciais nao alerta`() {
        assertNull(CompositeAlerts.accumulatedMortality(initialCount = 0, deadCount = 5))
    }

    @Test
    fun `F4 variacao de pH de 2_2 alerta`() {
        val status = CompositeAlerts.phDailyVariation(phMorning = 6.8, phAfternoon = 9.0)
        assertEquals(AlertLevel.ATENCAO, status?.level)
    }

    @Test
    fun `variacao de pH de exatamente 2 nao alerta e o sentido nao importa`() {
        assertNull(CompositeAlerts.phDailyVariation(phMorning = 7.0, phAfternoon = 9.0))
        assertNotNull(CompositeAlerts.phDailyVariation(phMorning = 9.0, phAfternoon = 6.8))
    }
}
