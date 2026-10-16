package com.valoes.valesaquagestor.domain.rules

import com.valoes.valesaquagestor.domain.calc.AmmoniaCalculator

/**
 * Avalia cada parâmetro da água e devolve o [WaterStatus] (nível, o que significa, o que fazer).
 *
 * Só avalia valores isolados. Alertas compostos (variação do pH, previsão de oxigênio, carga de
 * ração, mortalidade) ficam em [CompositeAlerts], `OxygenForecaster` e `PondLoadChecker`.
 * As faixas vêm de [OxygenThresholds] e dos demais objetos de `WaterThresholds.kt`.
 *
 * Valem as mesmas faixas para qualquer espécie (`specs/03`, seção 2); para outras espécies a tela
 * mostra "faixa de referência geral".
 */
object WaterEvaluator {

    /** Avalia o oxigênio dissolvido, em mg/L. */
    fun evaluateOxygen(mgL: Double): WaterStatus = when {
        mgL >= OxygenThresholds.ADEQUATE_MIN_MG_L -> status("oxygen", AlertLevel.ADEQUADO)
        mgL >= OxygenThresholds.CRITICAL_BELOW_MG_L -> status("oxygen", AlertLevel.ATENCAO)
        mgL >= OxygenThresholds.EMERGENCY_BELOW_MG_L -> status("oxygen", AlertLevel.CRITICO)
        else -> status("oxygen", AlertLevel.EMERGENCIA)
    }

    /** Avalia o pH. O nível sai de `ph_low` ou `ph_high` conforme o lado em que o valor fugiu. */
    fun evaluatePh(ph: Double): WaterStatus {
        val parameter = if (ph < PhThresholds.ADEQUATE_MIN) "ph_low" else "ph_high"
        return when {
            ph < PhThresholds.CRITICAL_MIN || ph > PhThresholds.CRITICAL_MAX ->
                status(parameter, AlertLevel.EMERGENCIA)

            ph < PhThresholds.ATTENTION_MIN || ph > PhThresholds.ATTENTION_MAX ->
                status(parameter, AlertLevel.CRITICO)

            ph < PhThresholds.ADEQUATE_MIN || ph > PhThresholds.ADEQUATE_MAX ->
                status(parameter, AlertLevel.ATENCAO)

            else -> status("ph", AlertLevel.ADEQUADO)
        }
    }

    /** Avalia a temperatura da água, em °C. */
    fun evaluateTemperature(celsius: Double): WaterStatus = when {
        celsius < TemperatureThresholds.CRITICAL_BELOW_C -> status("temperature_low", AlertLevel.CRITICO)
        celsius > TemperatureThresholds.ADEQUATE_MAX_C -> status("temperature_high", AlertLevel.CRITICO)
        celsius < TemperatureThresholds.ADEQUATE_MIN_C -> status("temperature_low", AlertLevel.ATENCAO)
        else -> status("temperature", AlertLevel.ADEQUADO)
    }

    /** Avalia a amônia **tóxica** (NH₃), em mg/L. Para a amônia total, use [evaluateAmmonia]. */
    fun evaluateToxicAmmonia(toxicMgL: Double): WaterStatus = when {
        toxicMgL < AmmoniaThresholds.ADEQUATE_BELOW_MG_L -> status("ammonia", AlertLevel.ADEQUADO)
        toxicMgL < AmmoniaThresholds.CRITICAL_FROM_MG_L -> status("ammonia", AlertLevel.ATENCAO)
        toxicMgL < AmmoniaThresholds.EMERGENCY_FROM_MG_L -> status("ammonia", AlertLevel.CRITICO)
        else -> status("ammonia", AlertLevel.EMERGENCIA)
    }

    /**
     * Calcula a amônia tóxica a partir da total, do pH e da temperatura e avalia o resultado.
     *
     * Devolve `null` quando falta pH ou temperatura ("dado insuficiente"): nunca se compara a
     * amônia total com os limites e nunca se chuta um valor.
     */
    fun evaluateAmmonia(totalMgL: Double, ph: Double?, celsius: Double?): WaterStatus? {
        if (ph == null || celsius == null) return null
        return evaluateToxicAmmonia(AmmoniaCalculator.toxicMgL(totalMgL, ph, celsius))
    }

    /** Avalia o nitrito (NO₂), em mg/L. */
    fun evaluateNitrite(mgL: Double): WaterStatus = when {
        mgL < NitriteThresholds.ADEQUATE_BELOW_MG_L -> status("nitrite", AlertLevel.ADEQUADO)
        mgL < NitriteThresholds.CRITICAL_FROM_MG_L -> status("nitrite", AlertLevel.ATENCAO)
        mgL < NitriteThresholds.EMERGENCY_FROM_MG_L -> status("nitrite", AlertLevel.CRITICO)
        else -> status("nitrite", AlertLevel.EMERGENCIA)
    }

    /** Avalia a transparência (disco de Secchi), em cm. */
    fun evaluateTransparency(cm: Double): WaterStatus = when {
        cm < TransparencyThresholds.CRITICAL_BELOW_CM -> status("transparency_low", AlertLevel.CRITICO)
        cm > TransparencyThresholds.CRITICAL_ABOVE_CM -> status("transparency_high", AlertLevel.CRITICO)
        cm < TransparencyThresholds.ADEQUATE_MIN_CM -> status("transparency_low", AlertLevel.ATENCAO)
        cm > TransparencyThresholds.ADEQUATE_MAX_CM -> status("transparency_high", AlertLevel.ATENCAO)
        else -> status("transparency", AlertLevel.ADEQUADO)
    }

    /** Avalia a alcalinidade, em mg/L de CaCO₃. */
    fun evaluateAlkalinity(mgL: Double): WaterStatus = when {
        mgL >= AlkalinityThresholds.ADEQUATE_MIN_MG_L -> status("alkalinity", AlertLevel.ADEQUADO)
        mgL >= AlkalinityThresholds.CRITICAL_BELOW_MG_L -> status("alkalinity", AlertLevel.ATENCAO)
        else -> status("alkalinity", AlertLevel.CRITICO)
    }

    /** Avalia a dureza, em mg/L de CaCO₃. */
    fun evaluateHardness(mgL: Double): WaterStatus = when {
        mgL >= HardnessThresholds.ADEQUATE_MIN_MG_L -> status("hardness", AlertLevel.ADEQUADO)
        else -> status("hardness", AlertLevel.ATENCAO)
    }

    private fun status(parameter: String, level: AlertLevel): WaterStatus {
        val suffix = "${parameter}_${level.name.lowercase()}"
        return WaterStatus(level, messageKey = "water_$suffix", actionKey = "action_water_$suffix")
    }
}
