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
