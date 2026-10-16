package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.rules.CompositeThresholds
import java.time.DayOfWeek

/** Parâmetro que entra na agenda de medições. */
enum class MeasuredParameter {
    OXYGEN,
    TEMPERATURE,
    TRANSPARENCY,
    PH,
    TOTAL_AMMONIA,
    NITRITE,
    ALKALINITY,
    HARDNESS,
}

/** Campo editável da [MonitoringSchedule]; a tela de Configurações usa para mostrar o erro no campo certo. */
enum class ScheduleField {
    OXYGEN_MORNING_HOUR,
    OXYGEN_AFTERNOON_HOUR,
    TRANSPARENCY_FROM_HOUR,
    TRANSPARENCY_TO_HOUR,
    HIGH_LOAD_AMMONIA_INTERVAL,
    HIGH_LOAD_AMMONIA_HOUR,
    RECRIA_BIOMETRY_INTERVAL,
    ENGORDA_BIOMETRY_INTERVAL,
    ALEVINAGEM_FEED_READJUST_INTERVAL,
    FEED_READJUST_INTERVAL,
}

/**
 * Medição prevista na rotina diária.
 *
 * @property parameter o que medir.
 * @property moments momentos do dia em que medir.
 */
data class ScheduledMeasurement(
    val parameter: MeasuredParameter,
    val moments: List<MomentoMedicao>,
)

/**
 * Agenda de medições, biometria e reajuste da ração. Alimenta a "Rotina diária" (Home).
 * Todos os horários e intervalos são parâmetros editáveis (o ADMIN os ajusta na Fase 08); os
 * padrões vêm das constantes do `companion object`, cada uma com a sua fonte.
 *
 * A agenda usa [MomentoMedicao] (manhã, meio-dia, tarde) em vez de horas, exceto onde a fonte dá a hora.
 *
 * @see specs/03-regras-negocio.md, seção 6
 */
data class MonitoringSchedule(
    val oxygenMorningHour: Int = OXYGEN_MORNING_HOUR,
    val oxygenAfternoonHour: Int = OXYGEN_AFTERNOON_HOUR,
    val transparencyFromHour: Int = TRANSPARENCY_FROM_HOUR,
    val transparencyToHour: Int = TRANSPARENCY_TO_HOUR,
    val weeklyDay: DayOfWeek = WEEKLY_DAY,
    val highLoadAmmoniaIntervalDays: Int = CompositeThresholds.AMMONIA_CHECK_INTERVAL_DAYS,
    val highLoadAmmoniaHour: Int = CompositeThresholds.AMMONIA_CHECK_HOUR,
    val recriaBiometryIntervalDays: Int = RECRIA_BIOMETRY_INTERVAL_DAYS,
    val engordaBiometryIntervalDays: Int = ENGORDA_BIOMETRY_INTERVAL_DAYS,
    val alevinagemFeedReadjustIntervalDays: Int = ALEVINAGEM_FEED_READJUST_INTERVAL_DAYS,
    val feedReadjustIntervalDays: Int = FEED_READJUST_INTERVAL_DAYS,
) {

    /**
     * Campos com valor inválido; vazio quando a agenda pode ser salva. Horas vão de 0 a 23, a manhã vem
     * antes da tarde, a janela da transparência tem começo antes do fim e todo intervalo é de pelo menos
     * 1 dia. Nenhum limite além disso é inventado.
     */
    fun invalidFields(): Set<ScheduleField> = buildSet {
        fun checkHour(field: ScheduleField, hour: Int) {
            if (hour !in HOURS_OF_DAY) add(field)
        }
        fun checkInterval(field: ScheduleField, days: Int) {
            if (days < MIN_INTERVAL_DAYS) add(field)
        }
        checkHour(ScheduleField.OXYGEN_MORNING_HOUR, oxygenMorningHour)
        checkHour(ScheduleField.OXYGEN_AFTERNOON_HOUR, oxygenAfternoonHour)
        checkHour(ScheduleField.TRANSPARENCY_FROM_HOUR, transparencyFromHour)
        checkHour(ScheduleField.TRANSPARENCY_TO_HOUR, transparencyToHour)
        checkHour(ScheduleField.HIGH_LOAD_AMMONIA_HOUR, highLoadAmmoniaHour)
        if (oxygenAfternoonHour <= oxygenMorningHour) add(ScheduleField.OXYGEN_AFTERNOON_HOUR)
        if (transparencyToHour <= transparencyFromHour) add(ScheduleField.TRANSPARENCY_TO_HOUR)
        checkInterval(ScheduleField.HIGH_LOAD_AMMONIA_INTERVAL, highLoadAmmoniaIntervalDays)
        checkInterval(ScheduleField.RECRIA_BIOMETRY_INTERVAL, recriaBiometryIntervalDays)
        checkInterval(ScheduleField.ENGORDA_BIOMETRY_INTERVAL, engordaBiometryIntervalDays)
        checkInterval(ScheduleField.ALEVINAGEM_FEED_READJUST_INTERVAL, alevinagemFeedReadjustIntervalDays)
        checkInterval(ScheduleField.FEED_READJUST_INTERVAL, feedReadjustIntervalDays)
    }

    /** Valor atual do [field]: hora do dia ou intervalo em dias. */
    fun valueOf(field: ScheduleField): Int = when (field) {
        ScheduleField.OXYGEN_MORNING_HOUR -> oxygenMorningHour
        ScheduleField.OXYGEN_AFTERNOON_HOUR -> oxygenAfternoonHour
        ScheduleField.TRANSPARENCY_FROM_HOUR -> transparencyFromHour
        ScheduleField.TRANSPARENCY_TO_HOUR -> transparencyToHour
        ScheduleField.HIGH_LOAD_AMMONIA_INTERVAL -> highLoadAmmoniaIntervalDays
        ScheduleField.HIGH_LOAD_AMMONIA_HOUR -> highLoadAmmoniaHour
        ScheduleField.RECRIA_BIOMETRY_INTERVAL -> recriaBiometryIntervalDays
        ScheduleField.ENGORDA_BIOMETRY_INTERVAL -> engordaBiometryIntervalDays
        ScheduleField.ALEVINAGEM_FEED_READJUST_INTERVAL -> alevinagemFeedReadjustIntervalDays
        ScheduleField.FEED_READJUST_INTERVAL -> feedReadjustIntervalDays
    }

    /** Cópia com o [field] trocado por [value]. */
    fun with(field: ScheduleField, value: Int): MonitoringSchedule = when (field) {
        ScheduleField.OXYGEN_MORNING_HOUR -> copy(oxygenMorningHour = value)
        ScheduleField.OXYGEN_AFTERNOON_HOUR -> copy(oxygenAfternoonHour = value)
        ScheduleField.TRANSPARENCY_FROM_HOUR -> copy(transparencyFromHour = value)
        ScheduleField.TRANSPARENCY_TO_HOUR -> copy(transparencyToHour = value)
        ScheduleField.HIGH_LOAD_AMMONIA_INTERVAL -> copy(highLoadAmmoniaIntervalDays = value)
        ScheduleField.HIGH_LOAD_AMMONIA_HOUR -> copy(highLoadAmmoniaHour = value)
        ScheduleField.RECRIA_BIOMETRY_INTERVAL -> copy(recriaBiometryIntervalDays = value)
        ScheduleField.ENGORDA_BIOMETRY_INTERVAL -> copy(engordaBiometryIntervalDays = value)
        ScheduleField.ALEVINAGEM_FEED_READJUST_INTERVAL -> copy(alevinagemFeedReadjustIntervalDays = value)
        ScheduleField.FEED_READJUST_INTERVAL -> copy(feedReadjustIntervalDays = value)
    }

    /** Medições diárias: oxigênio e pH de manhã e à tarde; temperatura e transparência no meio do dia. */
    fun dailyMeasurements(): List<ScheduledMeasurement> = listOf(
        ScheduledMeasurement(MeasuredParameter.OXYGEN, listOf(MomentoMedicao.MANHA, MomentoMedicao.TARDE)),
        ScheduledMeasurement(MeasuredParameter.TEMPERATURE, listOf(MomentoMedicao.MEIO_DIA)),
        ScheduledMeasurement(MeasuredParameter.TRANSPARENCY, listOf(MomentoMedicao.MEIO_DIA)),
        ScheduledMeasurement(MeasuredParameter.PH, listOf(MomentoMedicao.MANHA, MomentoMedicao.TARDE)),
    )

    /** Medições semanais, feitas em [weeklyDay]. */
    fun weeklyMeasurements(): List<MeasuredParameter> = listOf(
        MeasuredParameter.TOTAL_AMMONIA,
        MeasuredParameter.NITRITE,
        MeasuredParameter.ALKALINITY,
        MeasuredParameter.HARDNESS,
    )

    /**
     * Intervalo da biometria, em dias. Recria: quinzenal. Engorda: mensal.
     * Alevinagem: `null`, porque nenhuma fonte do spec traz o intervalo (a ração do alevino é
     * reajustada por [feedReadjustIntervalDays]; ver `specs/08`, conflito 10).
     */
    fun biometryIntervalDays(phase: FaseLote): Int? = when (phase) {
        FaseLote.ALEVINAGEM -> null
        FaseLote.RECRIA -> recriaBiometryIntervalDays
        FaseLote.ENGORDA -> engordaBiometryIntervalDays
    }

    /** Intervalo, em dias, para reajustar a ração: alevinos a cada 3 dias (3 a 4); demais fases, semanal. */
    fun feedReadjustIntervalDays(phase: FaseLote): Int =
        if (phase == FaseLote.ALEVINAGEM) alevinagemFeedReadjustIntervalDays else feedReadjustIntervalDays

    companion object {
        /** Horas de um dia no relógio de 24 h. */
        private val HOURS_OF_DAY = 0..23

        /** Menor intervalo possível entre duas tarefas: todo dia. */
        private const val MIN_INTERVAL_DAYS = 1

        /** @see F13 p.27 — oxigênio às 6 h, antes da primeira ração (janela de 6 a 8 h). */
        const val OXYGEN_MORNING_HOUR = 6

        /** @see F13 p.27 — oxigênio às 17 h. */
        const val OXYGEN_AFTERNOON_HOUR = 17

        /** @see F01 p.65 — transparência a partir das 11 h. */
        const val TRANSPARENCY_FROM_HOUR = 11

        /** @see F01 p.65 — transparência até as 12 h. */
        const val TRANSPARENCY_TO_HOUR = 12

        /** @see F01 p.152 — medições semanais; "ex.: toda segunda". */
        val WEEKLY_DAY: DayOfWeek = DayOfWeek.MONDAY

        /** @see F01 p.77 — recria: biometria quinzenal (15 dias). */
        const val RECRIA_BIOMETRY_INTERVAL_DAYS = 15

        /** @see F01 p.77 — engorda: biometria mensal (30 dias). */
        const val ENGORDA_BIOMETRY_INTERVAL_DAYS = 30

        /** @see F14 p.32 — alevinos: reajustar a ração a cada 3 ou 4 dias (usa o menor). */
        const val ALEVINAGEM_FEED_READJUST_INTERVAL_DAYS = 3

        /** @see F14 p.38 — demais fases: reajuste a cada semana. */
        const val FEED_READJUST_INTERVAL_DAYS = 7
    }
}
