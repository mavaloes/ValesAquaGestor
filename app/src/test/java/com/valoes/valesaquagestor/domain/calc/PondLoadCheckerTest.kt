package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.rules.AlertLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Casos F1 e F2 de `specs/09-casos-de-teste.md`. */
class PondLoadCheckerTest {

    @Test
    fun `F1 40 kg por hectare por dia esta no limite e nao alerta`() {
        val load = PondLoadChecker.check(dailyFeedKg = 4.0, areaM2 = 1000.0)
        assertEquals(40.0, load.kgPerHaDay, 0.0)
        assertNull(load.status)
        assertFalse(load.requiresAmmoniaCheck)
    }

    @Test
    fun `F2 61 kg por hectare exige medir a amonia`() {
        val load = PondLoadChecker.check(dailyFeedKg = 6.1, areaM2 = 1000.0)
        assertEquals(61.0, load.kgPerHaDay, 1e-6)
        assertEquals(AlertLevel.ATENCAO, load.status?.level)
        assertTrue(load.requiresAmmoniaCheck)
    }

    @Test
    fun `entre 40 e 60 alerta sem exigir amonia`() {
        val load = PondLoadChecker.check(dailyFeedKg = 5.0, areaM2 = 1000.0)
        assertEquals(AlertLevel.ATENCAO, load.status?.level)
        assertFalse(load.requiresAmmoniaCheck)
    }
}
