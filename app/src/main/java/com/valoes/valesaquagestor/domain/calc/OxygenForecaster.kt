package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.rules.OxygenThresholds
import com.valoes.valesaquagestor.domain.rules.WaterEvaluator
import com.valoes.valesaquagestor.domain.rules.WaterStatus

/**
 * Previsão de oxigênio da madrugada.
 *
 * @property valueMgL oxigênio previsto, em mg/L; valor negativo vira 0.
 * @property status nível da previsão (< 4 ATENCAO · < 3 CRITICO · < 2 EMERGENCIA).
 */
data class OxygenForecast(val valueMgL: Double, val status: WaterStatus)

/**
 * Previsão pelo método 2 (duas medidas à noite).
 *
 * @property dropPerHourMgL queda de oxigênio, em mg/L por hora.
 * @property forecast previsão para a hora-alvo.
 * @property hoursUntilLimit horas depois da segunda leitura até chegar a 3 mg/L; `null` se o oxigênio não está caindo.
 * @property limitHour hora do dia (0 a 24) em que chega a 3 mg/L, para ligar o aerador antes; `null` se não cai.
 */
data class NightForecast(
    val dropPerHourMgL: Double,
    val forecast: OxygenForecast,
    val hoursUntilLimit: Double?,
    val limitHour: Double?,
)

/** Previsão do oxigênio da madrugada (`specs/03-regras-negocio.md`, seção 4.4). */
object OxygenForecaster {

    /** @see F11 p.16 — a previsão é para as 7 h da manhã. */
    const val TARGET_HOUR = 7.0

    private const val HOURS_PER_DAY = 24.0

    /**
     * Método 2: `queda/h = (O₂ primeira − O₂ segunda) / horas entre elas`;
     * `O₂ previsto = O₂ segunda − queda/h × horas até a hora-alvo`.
     * Horas do dia em formato decimal (20 h = 20,0). Passagem da meia-noite é tratada.
     *
     * O limite de 3 mg/L usa [OxygenThresholds.CRITICAL_BELOW_MG_L] (F13 p.30).
     *
     * @see F11 p.16
     * @see F13 p.30
     */
    fun nightMethod(
        firstMgL: Double,
        firstHour: Double,
        secondMgL: Double,
        secondHour: Double,
        targetHour: Double = TARGET_HOUR,
    ): NightForecast {
        val hoursBetween = wrapHours(secondHour - firstHour)
        val hoursToTarget = wrapHours(targetHour - secondHour)
        val drop = (firstMgL - secondMgL) / hoursBetween
        val forecast = forecast(secondMgL - drop * hoursToTarget)

        val hoursToLimit = if (drop > 0.0) {
            ((secondMgL - OxygenThresholds.CRITICAL_BELOW_MG_L) / drop).coerceAtLeast(0.0)
        } else {
            null
        }
        val limitHour = hoursToLimit?.let { (secondHour + it) % HOURS_PER_DAY }
        return NightForecast(drop, forecast, hoursToLimit, limitHour)
    }

    /**
     * Método 1 (leituras diárias de manhã e de tarde, Kubitza 2003):
     * `diferença(d) = tarde(d−1) − manhã(d)`; `previsão manhã(d+1) = tarde(d) − diferença(d)`.
     *
     * @param previousAfternoonMgL oxigênio da tarde do dia anterior, tarde(d−1).
     * @param morningMgL oxigênio da manhã de hoje, manhã(d).
     * @param afternoonMgL oxigênio da tarde de hoje, tarde(d).
     * @see F11 p.15-16
     */
    fun dailyMethod(previousAfternoonMgL: Double, morningMgL: Double, afternoonMgL: Double): OxygenForecast =
        forecast(afternoonMgL - (previousAfternoonMgL - morningMgL))

    private fun forecast(rawMgL: Double): OxygenForecast {
        val value = rawMgL.coerceAtLeast(0.0)
        return OxygenForecast(value, WaterEvaluator.evaluateOxygen(value))
    }

    /** Intervalo em horas dentro de um dia: diferença negativa ou nula passa pela meia-noite. */
    private fun wrapHours(hours: Double): Double = ((hours % HOURS_PER_DAY) + HOURS_PER_DAY) % HOURS_PER_DAY
}
