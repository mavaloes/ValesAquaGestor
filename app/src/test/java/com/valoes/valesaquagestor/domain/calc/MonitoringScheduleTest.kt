package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonitoringScheduleTest {

    private val schedule = MonitoringSchedule()

    @Test
    fun `oxigenio e pH sao medidos de manha e a tarde`() {
        val daily = schedule.dailyMeasurements().associate { it.parameter to it.moments }
        assertEquals(listOf(MomentoMedicao.MANHA, MomentoMedicao.TARDE), daily[MeasuredParameter.OXYGEN])
        assertEquals(listOf(MomentoMedicao.MANHA, MomentoMedicao.TARDE), daily[MeasuredParameter.PH])
        assertEquals(listOf(MomentoMedicao.MEIO_DIA), daily[MeasuredParameter.TEMPERATURE])
        assertEquals(listOf(MomentoMedicao.MEIO_DIA), daily[MeasuredParameter.TRANSPARENCY])
    }

    @Test
    fun `horas padrao do oxigenio e da transparencia`() {
        assertEquals(6, schedule.oxygenMorningHour)
        assertEquals(17, schedule.oxygenAfternoonHour)
        assertEquals(11, schedule.transparencyFromHour)
        assertEquals(12, schedule.transparencyToHour)
    }

    @Test
    fun `medicoes semanais`() {
        assertEquals(
            listOf(MeasuredParameter.TOTAL_AMMONIA, MeasuredParameter.NITRITE, MeasuredParameter.ALKALINITY, MeasuredParameter.HARDNESS),
            schedule.weeklyMeasurements(),
        )
    }

    @Test
    fun `intervalo de biometria por fase`() {
        assertNull(schedule.biometryIntervalDays(FaseLote.ALEVINAGEM))
        assertEquals(15, schedule.biometryIntervalDays(FaseLote.RECRIA))
        assertEquals(30, schedule.biometryIntervalDays(FaseLote.ENGORDA))
    }

    @Test
    fun `reajuste de racao e a cada 3 dias no alevino e semanal nas demais fases`() {
        assertEquals(3, schedule.feedReadjustIntervalDays(FaseLote.ALEVINAGEM))
        assertEquals(7, schedule.feedReadjustIntervalDays(FaseLote.ENGORDA))
    }

    @Test
    fun `agenda e editavel`() {
        val custom = MonitoringSchedule(engordaBiometryIntervalDays = 20)
        assertEquals(20, custom.biometryIntervalDays(FaseLote.ENGORDA))
    }
}
