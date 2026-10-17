package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.model.QualidadeAgua
import com.valoes.valesaquagestor.domain.rules.AlertItem
import com.valoes.valesaquagestor.domain.usecase.FarmOverview
import com.valoes.valesaquagestor.domain.usecase.LotOverview
import com.valoes.valesaquagestor.domain.usecase.TankOverview
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Algo que falta fazer hoje. */
sealed interface PendingTask {
    val tankId: Long
    val tankName: String

    /**
     * Medição da agenda ainda não registrada. [moment] é `null` nas medições semanais.
     */
    data class Measurement(
        override val tankId: Long,
        override val tankName: String,
        val parameter: MeasuredParameter,
        val moment: MomentoMedicao?,
    ) : PendingTask

    /**
     * Tratos que faltam hoje num lote: [missingMeals] de [expectedMeals] (o mínimo de refeições da
     * faixa de peso, F14 p.34).
     */
    data class Feeding(
        override val tankId: Long,
        override val tankName: String,
        val lotId: Long,
        val especie: Especie,
        val missingMeals: Int,
        val expectedMeals: Int,
    ) : PendingTask

    /** Biometria vencida há [overdueDays] dias (0 = vence hoje). */
    data class Biometry(
        override val tankId: Long,
        override val tankName: String,
        val lotId: Long,
        val especie: Especie,
        val overdueDays: Int,
    ) : PendingTask
}

/**
 * A rotina do dia da Home.
 *
 * @property alerts alertas por prioridade (EMERGENCIA primeiro).
 * @property tasks o que falta fazer hoje, agrupado por tanque.
 */
data class DailyRoutine(
    val alerts: List<AlertItem>,
    val tasks: List<PendingTask>,
)

/**
 * Monta a rotina diária a partir do retrato da fazenda e da [MonitoringSchedule]
 * (`specs/03-regras-negocio.md`, seção 6). Só tanques com lote ativo têm tarefas.
 *
 * - **Medições:** as da agenda diária que ainda não têm registro hoje no momento certo (ex.: oxigênio
 *   de manhã); as semanais, quando não há leitura desde o último dia da semana marcado.
 * - **Tratos:** refeições mínimas da faixa de peso menos os tratos já registrados hoje (só tilápia,
 *   a única espécie com tabela de ração).
 * - **Biometria:** vencida quando passou o intervalo da fase desde a última biometria (ou desde o povoamento).
 */
object DailyRoutineBuilder {

    fun build(farm: FarmOverview, schedule: MonitoringSchedule, clock: Clock): DailyRoutine {
        val today = LocalDate.now(clock)
        val tasks = farm.tanks
            .filter { it.lots.isNotEmpty() }
            .flatMap { tank -> measurementTasks(tank, schedule, today, clock) + tank.lots.flatMap { lotTasks(tank, it, schedule, today, clock) } }
            .sortedWith(compareBy<PendingTask> { it.tankName }.thenBy { taskOrder(it) })
        return DailyRoutine(AlertAggregator.prioritized(farm), tasks)
    }

    private fun measurementTasks(
        tank: TankOverview,
        schedule: MonitoringSchedule,
        today: LocalDate,
        clock: Clock,
    ): List<PendingTask> {
        val tanque = tank.tanque
        val daily = schedule.dailyMeasurements().flatMap { scheduled ->
            scheduled.moments
                .filter { moment -> tank.waterToday.none { it.momento == moment && it.hasValue(scheduled.parameter) } }
                .map { PendingTask.Measurement(tanque.id, tanque.nome, scheduled.parameter, it) }
        }
        val weekStart = lastWeeklyDay(today, schedule.weeklyDay).atStartOfDay(clock.zone).toInstant()
        val weekly = schedule.weeklyMeasurements()
            .filter { lastReading(tank, it)?.isBefore(weekStart) ?: true }
            .map { PendingTask.Measurement(tanque.id, tanque.nome, it, null) }
        return daily + weekly
    }

    private fun lotTasks(
        tank: TankOverview,
        lot: LotOverview,
        schedule: MonitoringSchedule,
        today: LocalDate,
        clock: Clock,
    ): List<PendingTask> {
        val lote = lot.lote
        val tanque = tank.tanque
        val feeding = TilapiaFeedTable.bandFor(lote.pesoMedioAtualG)
            ?.takeIf { lote.especie == Especie.TILAPIA }
            ?.let { band ->
                val missing = band.meals - lot.feedingsToday.size
                if (missing > 0) PendingTask.Feeding(tanque.id, tanque.nome, lote.id, lote.especie, missing, band.meals) else null
            }

        val interval = schedule.biometryIntervalDays(lote.fase)
        val reference = lot.latestBiometries.firstOrNull()?.dataHora ?: lote.dataPovoamento
        val daysSince = ChronoUnit.DAYS.between(reference.atZone(clock.zone).toLocalDate(), today).toInt()
        val biometry = if (interval != null && daysSince >= interval) {
            PendingTask.Biometry(tanque.id, tanque.nome, lote.id, lote.especie, daysSince - interval)
        } else {
            null
        }
        return listOfNotNull(feeding, biometry)
    }

    /** Último dia da semana marcado em [weeklyDay] que já passou (hoje inclusive). */
    private fun lastWeeklyDay(today: LocalDate, weeklyDay: DayOfWeek): LocalDate =
        today.with(TemporalAdjusters.previousOrSame(weeklyDay))

    private fun lastReading(tank: TankOverview, parameter: MeasuredParameter): Instant? = when (parameter) {
        MeasuredParameter.TOTAL_AMMONIA -> tank.readings.amoniaTotalMgL
        MeasuredParameter.NITRITE -> tank.readings.nitritoMgL
        MeasuredParameter.ALKALINITY -> tank.readings.alcalinidadeMgL
        MeasuredParameter.HARDNESS -> tank.readings.durezaMgL
        MeasuredParameter.OXYGEN -> tank.readings.oxigenioMgL
        MeasuredParameter.TEMPERATURE -> tank.readings.temperaturaC
        MeasuredParameter.TRANSPARENCY -> tank.readings.transparenciaCm
        MeasuredParameter.PH -> tank.readings.ph
    }?.dataHora

    private fun QualidadeAgua.hasValue(parameter: MeasuredParameter): Boolean = when (parameter) {
        MeasuredParameter.OXYGEN -> oxigenioMgL != null
        MeasuredParameter.TEMPERATURE -> temperaturaC != null
        MeasuredParameter.TRANSPARENCY -> transparenciaCm != null
        MeasuredParameter.PH -> ph != null
        MeasuredParameter.TOTAL_AMMONIA -> amoniaTotalMgL != null
        MeasuredParameter.NITRITE -> nitritoMgL != null
        MeasuredParameter.ALKALINITY -> alcalinidadeMgL != null
        MeasuredParameter.HARDNESS -> durezaMgL != null
    }

    /** Dentro do tanque: medições (por momento do dia), depois tratos, depois biometria. */
    private fun taskOrder(task: PendingTask): Int = when (task) {
        is PendingTask.Measurement -> (task.moment?.ordinal ?: MomentoMedicao.entries.size) * ORDER_STEP + task.parameter.ordinal
        is PendingTask.Feeding -> FEEDING_ORDER
        is PendingTask.Biometry -> BIOMETRY_ORDER
    }

    private const val ORDER_STEP = 10
    private const val FEEDING_ORDER = 1_000
    private const val BIOMETRY_ORDER = 1_001
}
