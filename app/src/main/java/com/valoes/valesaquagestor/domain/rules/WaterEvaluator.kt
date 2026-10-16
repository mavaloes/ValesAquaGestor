package com.valoes.valesaquagestor.domain.rules

import com.valoes.valesaquagestor.domain.calc.AmmoniaCalculator

/**
 * Avalia cada parâmetro da água e devolve o [WaterStatus] (nível, o que significa, o que fazer).
 *
 * Só avalia valores isolados. Alertas compostos (variação do pH, previsão de oxigênio, carga de
 * ração, mortalidade) ficam em [CompositeAlerts], `OxygenForecaster` e `PondLoadChecker`.
 * As faixas vêm de [WaterRanges], que por padrão usa [OxygenThresholds] e os demais objetos de
 * `WaterThresholds.kt`; o ADMIN pode ajustá-las nas Configurações.
 *
 * Valem as mesmas faixas para qualquer espécie (`specs/03`, seção 2); para outras espécies a tela
 * mostra "faixa de referência geral".
 */
object WaterEvaluator {

    /** Avalia o oxigênio dissolvido, em mg/L. */
    fun evaluateOxygen(mgL: Double, ranges: WaterRanges = WaterRanges.DEFAULT): WaterStatus = when {
        mgL >= ranges.oxygenAdequateMinMgL -> status("oxygen", AlertLevel.ADEQUADO)
        mgL >= ranges.oxygenCriticalBelowMgL -> status("oxygen", AlertLevel.ATENCAO)
        mgL >= ranges.oxygenEmergencyBelowMgL -> status("oxygen", AlertLevel.CRITICO)
        else -> status("oxygen", AlertLevel.EMERGENCIA)
    }

    /** Avalia o pH. O nível sai de `ph_low` ou `ph_high` conforme o lado em que o valor fugiu. */
    fun evaluatePh(ph: Double, ranges: WaterRanges = WaterRanges.DEFAULT): WaterStatus {
        val parameter = if (ph < ranges.phAdequateMin) "ph_low" else "ph_high"
        return when {
            ph < ranges.phCriticalMin || ph > ranges.phCriticalMax ->
                status(parameter, AlertLevel.EMERGENCIA)

            ph < ranges.phAttentionMin || ph > ranges.phAttentionMax ->
                status(parameter, AlertLevel.CRITICO)

            ph < ranges.phAdequateMin || ph > ranges.phAdequateMax ->
                status(parameter, AlertLevel.ATENCAO)

            else -> status("ph", AlertLevel.ADEQUADO)
        }
    }

    /** Avalia a temperatura da água, em °C. */
    fun evaluateTemperature(celsius: Double, ranges: WaterRanges = WaterRanges.DEFAULT): WaterStatus = when {
        celsius < ranges.temperatureCriticalBelowC -> status("temperature_low", AlertLevel.CRITICO)
        celsius > ranges.temperatureAdequateMaxC -> status("temperature_high", AlertLevel.CRITICO)
        celsius < ranges.temperatureAdequateMinC -> status("temperature_low", AlertLevel.ATENCAO)
        else -> status("temperature", AlertLevel.ADEQUADO)
    }

    /** Avalia a amônia **tóxica** (NH₃), em mg/L. Para a amônia total, use [evaluateAmmonia]. */
    fun evaluateToxicAmmonia(toxicMgL: Double, ranges: WaterRanges = WaterRanges.DEFAULT): WaterStatus = when {
        toxicMgL < ranges.ammoniaAdequateBelowMgL -> status("ammonia", AlertLevel.ADEQUADO)
        toxicMgL < ranges.ammoniaCriticalFromMgL -> status("ammonia", AlertLevel.ATENCAO)
        toxicMgL < ranges.ammoniaEmergencyFromMgL -> status("ammonia", AlertLevel.CRITICO)
        else -> status("ammonia", AlertLevel.EMERGENCIA)
    }

    /**
     * Calcula a amônia tóxica a partir da total, do pH e da temperatura e avalia o resultado.
     *
     * Devolve `null` quando falta pH ou temperatura ("dado insuficiente"): nunca se compara a
     * amônia total com os limites e nunca se chuta um valor.
     */
    fun evaluateAmmonia(
        totalMgL: Double,
        ph: Double?,
        celsius: Double?,
        ranges: WaterRanges = WaterRanges.DEFAULT,
    ): WaterStatus? {
        if (ph == null || celsius == null) return null
        return evaluateToxicAmmonia(AmmoniaCalculator.toxicMgL(totalMgL, ph, celsius), ranges)
    }

    /** Avalia o nitrito (NO₂), em mg/L. */
    fun evaluateNitrite(mgL: Double, ranges: WaterRanges = WaterRanges.DEFAULT): WaterStatus = when {
        mgL < ranges.nitriteAdequateBelowMgL -> status("nitrite", AlertLevel.ADEQUADO)
        mgL < ranges.nitriteCriticalFromMgL -> status("nitrite", AlertLevel.ATENCAO)
        mgL < ranges.nitriteEmergencyFromMgL -> status("nitrite", AlertLevel.CRITICO)
        else -> status("nitrite", AlertLevel.EMERGENCIA)
    }

    /** Avalia a transparência (disco de Secchi), em cm. */
    fun evaluateTransparency(cm: Double, ranges: WaterRanges = WaterRanges.DEFAULT): WaterStatus = when {
        cm < ranges.transparencyCriticalBelowCm -> status("transparency_low", AlertLevel.CRITICO)
        cm > ranges.transparencyCriticalAboveCm -> status("transparency_high", AlertLevel.CRITICO)
        cm < ranges.transparencyAdequateMinCm -> status("transparency_low", AlertLevel.ATENCAO)
        cm > ranges.transparencyAdequateMaxCm -> status("transparency_high", AlertLevel.ATENCAO)
        else -> status("transparency", AlertLevel.ADEQUADO)
    }

    /** Avalia a alcalinidade, em mg/L de CaCO₃. */
    fun evaluateAlkalinity(mgL: Double, ranges: WaterRanges = WaterRanges.DEFAULT): WaterStatus = when {
        mgL >= ranges.alkalinityAdequateMinMgL -> status("alkalinity", AlertLevel.ADEQUADO)
        mgL >= ranges.alkalinityCriticalBelowMgL -> status("alkalinity", AlertLevel.ATENCAO)
        else -> status("alkalinity", AlertLevel.CRITICO)
    }

    /** Avalia a dureza, em mg/L de CaCO₃. */
    fun evaluateHardness(mgL: Double, ranges: WaterRanges = WaterRanges.DEFAULT): WaterStatus = when {
        mgL >= ranges.hardnessAdequateMinMgL -> status("hardness", AlertLevel.ADEQUADO)
        else -> status("hardness", AlertLevel.ATENCAO)
    }

    private fun status(parameter: String, level: AlertLevel): WaterStatus {
        val suffix = "${parameter}_${level.name.lowercase()}"
        return WaterStatus(level, messageKey = "water_$suffix", actionKey = "action_water_$suffix")
    }
}
