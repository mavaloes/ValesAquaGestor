package com.valoes.valesaquagestor.farm

import com.valoes.valesaquagestor.domain.calc.DailyRoutineBuilder
import com.valoes.valesaquagestor.domain.calc.MeasuredParameter
import com.valoes.valesaquagestor.domain.calc.MonitoringSchedule
import com.valoes.valesaquagestor.domain.calc.PendingTask
import com.valoes.valesaquagestor.domain.model.Arracoamento
import com.valoes.valesaquagestor.domain.model.Biometria
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.LatestWaterReadings
import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.model.QualidadeAgua
import com.valoes.valesaquagestor.domain.model.Reading
import com.valoes.valesaquagestor.domain.model.Tanque
import com.valoes.valesaquagestor.domain.usecase.FarmOverview
import com.valoes.valesaquagestor.domain.usecase.LotOverview
import com.valoes.valesaquagestor.domain.usecase.TankOverview
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Hoje é quarta-feira 14/10/2026; a segunda-feira da semana é 12/10. */
class DailyRoutineBuilderTest {

    private val schedule = MonitoringSchedule()

    private fun routine(
        lote: Lote? = lot(),
        feedingsToday: List<Arracoamento> = emptyList(),
        biometries: List<Biometria> = emptyList(),
        waterToday: List<QualidadeAgua> = emptyList(),
        readings: LatestWaterReadings = LatestWaterReadings(),
        tanque: Tanque = tank(),
    ) = DailyRoutineBuilder.build(
        FarmOverview(listOf(overviewOf(tanque, lote, feedingsToday, biometries, waterToday, readings))),
        schedule,
        FIXED_CLOCK,
    )

    private fun overviewOf(
        tanque: Tanque,
        lote: Lote?,
        feedingsToday: List<Arracoamento>,
        biometries: List<Biometria>,
        waterToday: List<QualidadeAgua>,
        readings: LatestWaterReadings,
    ) = TankOverview(
        tanque = tanque,
        lots = listOfNotNull(lote?.let { LotOverview(it, emptyList(), feedingsToday, biometries) }),
        readings = readings,
        waterToday = waterToday,
    )

    private fun measurements(routine: com.valoes.valesaquagestor.domain.calc.DailyRoutine) =
        routine.tasks.filterIsInstance<PendingTask.Measurement>().map { it.parameter to it.moment }

    @Test
    fun `sem nenhuma medicao hoje faltam todas as diarias e as semanais`() {
        val pending = measurements(routine())

        assertEquals(
            listOf(
                MeasuredParameter.OXYGEN to MomentoMedicao.MANHA,
                MeasuredParameter.PH to MomentoMedicao.MANHA,
                MeasuredParameter.TEMPERATURE to MomentoMedicao.MEIO_DIA,
                MeasuredParameter.TRANSPARENCY to MomentoMedicao.MEIO_DIA,
                MeasuredParameter.OXYGEN to MomentoMedicao.TARDE,
                MeasuredParameter.PH to MomentoMedicao.TARDE,
                MeasuredParameter.TOTAL_AMMONIA to null,
                MeasuredParameter.NITRITE to null,
                MeasuredParameter.ALKALINITY to null,
                MeasuredParameter.HARDNESS to null,
            ),
            pending,
        )
    }

    @Test
    fun `o que ja foi medido hoje no momento certo sai da lista`() {
        val today = listOf(water(1, momento = MomentoMedicao.MANHA, oxigenio = 5.0, ph = 7.2, temperatura = 27.0))

        val pending = measurements(routine(waterToday = today))

        assertTrue((MeasuredParameter.OXYGEN to MomentoMedicao.MANHA) !in pending)
        assertTrue((MeasuredParameter.PH to MomentoMedicao.MANHA) !in pending)
        assertTrue((MeasuredParameter.OXYGEN to MomentoMedicao.TARDE) in pending)
        // A temperatura é do meio-dia: medi-la de manhã não cobre a medição do meio-dia.
        assertTrue((MeasuredParameter.TEMPERATURE to MomentoMedicao.MEIO_DIA) in pending)
    }

    @Test
    fun `medicao semanal vence na segunda e some quando medida desde a segunda`() {
        val sunday = Instant.parse("2026-10-11T10:00:00Z")
        val monday = Instant.parse("2026-10-12T10:00:00Z")
        val readingsOld = LatestWaterReadings(amoniaTotalMgL = Reading(0.4, sunday), nitritoMgL = Reading(0.01, monday))

        val pending = measurements(routine(readings = readingsOld)).filter { it.second == null }.map { it.first }

        assertTrue(MeasuredParameter.TOTAL_AMMONIA in pending)
        assertTrue(MeasuredParameter.NITRITE !in pending)
        assertTrue(MeasuredParameter.ALKALINITY in pending)
    }

    @Test
    fun `tanque sem lote ativo nao tem tarefas mas continua com alertas`() {
        val farm = FarmOverview(
            listOf(
                overviewOf(tank(), lote = null, emptyList(), emptyList(), emptyList(), LatestWaterReadings(oxigenioMgL = Reading(1.5, NOW))),
            ),
        )

        val result = DailyRoutineBuilder.build(farm, schedule, FIXED_CLOCK)

        assertTrue(result.tasks.isEmpty())
        assertEquals(1, result.alerts.size)
    }

    @Test
    fun `faltam os tratos minimos da faixa de peso menos os de hoje`() {
        // 250 g: faixa 250 a 400 g, 2 refeições por dia.
        val none = routine().tasks.filterIsInstance<PendingTask.Feeding>().single()
        assertEquals(2, none.expectedMeals)
        assertEquals(2, none.missingMeals)

        val one = routine(feedingsToday = listOf(feeding(1))).tasks.filterIsInstance<PendingTask.Feeding>().single()
        assertEquals(1, one.missingMeals)

        val done = routine(feedingsToday = listOf(feeding(1), feeding(2))).tasks.filterIsInstance<PendingTask.Feeding>()
        assertTrue(done.isEmpty())
    }

    @Test
    fun `tarefa de racao so existe para tilapia dentro da tabela`() {
        assertTrue(routine(lote = lot(especie = Especie.OUTRA)).tasks.filterIsInstance<PendingTask.Feeding>().isEmpty())
        assertTrue(routine(lote = lot(pesoMedioAtualG = 1800.0)).tasks.filterIsInstance<PendingTask.Feeding>().isEmpty())
        assertTrue(routine(lote = lot(pesoMedioAtualG = 0.5)).tasks.filterIsInstance<PendingTask.Feeding>().isEmpty())
    }

    private fun biometryTasks(lote: Lote, biometries: List<Biometria> = emptyList()) =
        routine(lote = lote, biometries = biometries).tasks.filterIsInstance<PendingTask.Biometry>()

    @Test
    fun `biometria da recria vence com 15 dias desde o povoamento`() {
        assertTrue(biometryTasks(lot(fase = FaseLote.RECRIA, dataPovoamento = daysAgo(14))).isEmpty())

        val today = biometryTasks(lot(fase = FaseLote.RECRIA, dataPovoamento = daysAgo(15))).single()
        assertEquals(0, today.overdueDays)

        val late = biometryTasks(lot(fase = FaseLote.RECRIA, dataPovoamento = daysAgo(20))).single()
        assertEquals(5, late.overdueDays)
    }

    @Test
    fun `a biometria mais recente zera a contagem`() {
        val lote = lot(fase = FaseLote.ENGORDA, dataPovoamento = daysAgo(100))
        val recent = listOf(biometry(1, at = daysAgo(10), kg = 5.0, fish = 20))
        val old = listOf(biometry(2, at = daysAgo(31), kg = 5.0, fish = 20))

        assertTrue(biometryTasks(lote, recent).isEmpty())
        assertEquals(1, biometryTasks(lote, old).single().overdueDays)
    }

    @Test
    fun `engorda usa intervalo mensal e alevinagem nao tem biometria periodica`() {
        assertEquals(0, biometryTasks(lot(fase = FaseLote.ENGORDA, dataPovoamento = daysAgo(30))).single().overdueDays)
        assertTrue(biometryTasks(lot(fase = FaseLote.ALEVINAGEM, dataPovoamento = daysAgo(200))).isEmpty())
    }

    @Test
    fun `tarefas ficam agrupadas por tanque, em ordem de nome`() {
        val farm = FarmOverview(
            listOf(
                overviewOf(tank(1, "Zeta"), lot(1, 1), emptyList(), emptyList(), emptyList(), LatestWaterReadings()),
                overviewOf(tank(2, "Alfa"), lot(2, 2), emptyList(), emptyList(), emptyList(), LatestWaterReadings()),
            ),
        )

        val names = DailyRoutineBuilder.build(farm, schedule, FIXED_CLOCK).tasks.map { it.tankName }

        assertEquals(names.sorted(), names)
        assertEquals("Alfa", names.first())
    }

    @Test
    fun `dentro do tanque a medicao vem antes da racao e da biometria`() {
        val lote = lot(fase = FaseLote.RECRIA, dataPovoamento = daysAgo(20))

        val kinds = routine(lote = lote).tasks.map { it::class.simpleName }

        assertEquals("Measurement", kinds.first())
        assertEquals(listOf("Feeding", "Biometry"), kinds.takeLast(2))
    }

    @Test
    fun `alertas da rotina saem em ordem de prioridade`() {
        val farm = FarmOverview(
            listOf(
                overviewOf(tank(1, "A"), lot(1, 1), emptyList(), emptyList(), emptyList(), LatestWaterReadings(oxigenioMgL = Reading(3.5, NOW))),
                overviewOf(tank(2, "B"), lot(2, 2), emptyList(), emptyList(), emptyList(), LatestWaterReadings(oxigenioMgL = Reading(1.5, NOW))),
            ),
        )

        val result = DailyRoutineBuilder.build(farm, schedule, FIXED_CLOCK)

        assertEquals(listOf("B", "A"), result.alerts.map { it.tankName })
    }
}
