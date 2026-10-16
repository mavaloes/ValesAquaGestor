package com.valoes.valesaquagestor.domain.rules

import kotlin.math.abs

/**
 * Alertas compostos que dependem de mais de uma medida ou do histórico do lote.
 * A previsão de oxigênio fica em `OxygenForecaster` e a carga de ração em `PondLoadChecker`.
 *
 * @see specs/03-regras-negocio.md, seção 3
 */
object CompositeAlerts {

    /**
     * Variação do pH no dia (manhã × fim da tarde). Acima de 2 unidades é ATENCAO: sinal de
     * alcalinidade baixa ou excesso de plâncton. Devolve `null` quando não há alerta (2,0 exato não alerta).
     *
     * @see F01 p.66
     * @see F04 p.4
     * @see F11 p.20
     */
    fun phDailyVariation(phMorning: Double, phAfternoon: Double): WaterStatus? {
        if (abs(phAfternoon - phMorning) <= CompositeThresholds.PH_DAILY_VARIATION_ABOVE) return null
        return attention("composite_ph_variation")
    }

    /**
     * Mortalidade acumulada do ciclo: acima de 10% é ATENCAO ("chame um técnico"). Devolve `null`
     * quando não há alerta (10% exato não alerta) ou quando o lote não tem peixes iniciais.
     *
     * @param initialCount quantidade inicial de peixes do lote.
     * @param deadCount total de peixes mortos no ciclo (soma das movimentações `MORTALIDADE`).
     * @see F02 p.69
     */
    fun accumulatedMortality(initialCount: Int, deadCount: Int): WaterStatus? {
        if (initialCount <= 0) return null
        val percent = deadCount * 100.0 / initialCount
        if (percent <= CompositeThresholds.MORTALITY_ATTENTION_ABOVE_PCT) return null
        return attention("composite_mortality")
    }

    private fun attention(parameter: String): WaterStatus {
        val suffix = "${parameter}_${AlertLevel.ATENCAO.name.lowercase()}"
        return WaterStatus(AlertLevel.ATENCAO, "water_$suffix", "action_water_$suffix")
    }
}
