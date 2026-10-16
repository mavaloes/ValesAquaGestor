package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.rules.AlertLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Casos E1 a E3 de `specs/09-casos-de-teste.md`. O E4 espera a série da F11 p.16. */
class OxygenForecasterTest {

    @Test
    fun `E1 metodo 2 com 12 mg por litro as 20 h e 10 as 22 h`() {
        val result = OxygenForecaster.nightMethod(12.0, 20.0, 10.0, 22.0)
        assertEquals(1.0, result.dropPerHourMgL, 1e-6)
        assertEquals(1.0, result.forecast.valueMgL, 1e-6)
        assertEquals(AlertLevel.EMERGENCIA, result.forecast.status.level)
    }

    @Test
    fun `E2 metodo 2 chega a 3 mg por litro as 3 h da manha`() {
        val result = OxygenForecaster.nightMethod(7.0, 19.0, 6.5, 20.0)
        assertEquals(0.5, result.dropPerHourMgL, 1e-6)
        assertEquals(7.0, result.hoursUntilLimit!!, 1e-6)
        assertEquals(3.0, result.limitHour!!, 1e-6)
    }

    @Test
    fun `E3 metodo 1 com a serie da F11`() {
        val forecast = OxygenForecaster.dailyMethod(previousAfternoonMgL = 14.2, morningMgL = 5.5, afternoonMgL = 15.0)
        assertEquals(6.3, forecast.valueMgL, 1e-6)
        assertEquals(AlertLevel.ADEQUADO, forecast.status.level)
    }

    @Test
    fun `previsao negativa vira zero e emergencia`() {
        val forecast = OxygenForecaster.dailyMethod(previousAfternoonMgL = 10.0, morningMgL = 1.0, afternoonMgL = 5.0)
        assertEquals(0.0, forecast.valueMgL, 0.0)
        assertEquals(AlertLevel.EMERGENCIA, forecast.status.level)
    }

    @Test
    fun `oxigenio que nao cai nao tem hora limite`() {
        val result = OxygenForecaster.nightMethod(6.0, 19.0, 6.0, 20.0)
        assertNull(result.hoursUntilLimit)
        assertNull(result.limitHour)
        assertEquals(6.0, result.forecast.valueMgL, 1e-6)
    }

    @Test
    fun `previsao entre 3 e 4 e atencao e entre 2 e 3 e critico`() {
        assertEquals(AlertLevel.ATENCAO, OxygenForecaster.dailyMethod(8.0, 4.0, 7.5).status.level)
        assertEquals(AlertLevel.CRITICO, OxygenForecaster.dailyMethod(8.0, 5.0, 5.5).status.level)
    }
}
