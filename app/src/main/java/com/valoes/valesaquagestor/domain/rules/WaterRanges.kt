package com.valoes.valesaquagestor.domain.rules

/** Campo editável de [WaterRanges]; a tela de Configurações usa para mostrar o erro no campo certo. */
enum class WaterRangeField {
    OXYGEN_ADEQUATE_MIN,
    OXYGEN_CRITICAL_BELOW,
    OXYGEN_EMERGENCY_BELOW,
    PH_ADEQUATE_MIN,
    PH_ADEQUATE_MAX,
    PH_ATTENTION_MIN,
    PH_ATTENTION_MAX,
    PH_CRITICAL_MIN,
    PH_CRITICAL_MAX,
    TEMPERATURE_ADEQUATE_MIN,
    TEMPERATURE_ADEQUATE_MAX,
    TEMPERATURE_CRITICAL_BELOW,
    AMMONIA_ADEQUATE_BELOW,
    AMMONIA_CRITICAL_FROM,
    AMMONIA_EMERGENCY_FROM,
    NITRITE_ADEQUATE_BELOW,
    NITRITE_CRITICAL_FROM,
    NITRITE_EMERGENCY_FROM,
    TRANSPARENCY_ADEQUATE_MIN,
    TRANSPARENCY_ADEQUATE_MAX,
    TRANSPARENCY_CRITICAL_BELOW,
    TRANSPARENCY_CRITICAL_ABOVE,
    ALKALINITY_ADEQUATE_MIN,
    ALKALINITY_CRITICAL_BELOW,
    HARDNESS_ADEQUATE_MIN,
}

/**
 * Faixas da água usadas pelo [WaterEvaluator]. Os padrões são as constantes com fonte de
 * `WaterThresholds.kt` (`specs/03-regras-negocio.md`, seção 2); o ADMIN pode ajustar cada limite nas
 * Configurações, e o ajuste fica guardado à parte, sem mexer nas constantes.
 *
 * A amônia é a **tóxica** (NH₃). Os limites dos alertas compostos (variação do pH, carga de ração,
 * mortalidade) não são editáveis: os textos deles citam o número.
 */
data class WaterRanges(
    val oxygenAdequateMinMgL: Double = OxygenThresholds.ADEQUATE_MIN_MG_L,
    val oxygenCriticalBelowMgL: Double = OxygenThresholds.CRITICAL_BELOW_MG_L,
    val oxygenEmergencyBelowMgL: Double = OxygenThresholds.EMERGENCY_BELOW_MG_L,
    val phAdequateMin: Double = PhThresholds.ADEQUATE_MIN,
    val phAdequateMax: Double = PhThresholds.ADEQUATE_MAX,
    val phAttentionMin: Double = PhThresholds.ATTENTION_MIN,
    val phAttentionMax: Double = PhThresholds.ATTENTION_MAX,
    val phCriticalMin: Double = PhThresholds.CRITICAL_MIN,
    val phCriticalMax: Double = PhThresholds.CRITICAL_MAX,
    val temperatureAdequateMinC: Double = TemperatureThresholds.ADEQUATE_MIN_C,
    val temperatureAdequateMaxC: Double = TemperatureThresholds.ADEQUATE_MAX_C,
    val temperatureCriticalBelowC: Double = TemperatureThresholds.CRITICAL_BELOW_C,
    val ammoniaAdequateBelowMgL: Double = AmmoniaThresholds.ADEQUATE_BELOW_MG_L,
    val ammoniaCriticalFromMgL: Double = AmmoniaThresholds.CRITICAL_FROM_MG_L,
    val ammoniaEmergencyFromMgL: Double = AmmoniaThresholds.EMERGENCY_FROM_MG_L,
    val nitriteAdequateBelowMgL: Double = NitriteThresholds.ADEQUATE_BELOW_MG_L,
    val nitriteCriticalFromMgL: Double = NitriteThresholds.CRITICAL_FROM_MG_L,
    val nitriteEmergencyFromMgL: Double = NitriteThresholds.EMERGENCY_FROM_MG_L,
    val transparencyAdequateMinCm: Double = TransparencyThresholds.ADEQUATE_MIN_CM,
    val transparencyAdequateMaxCm: Double = TransparencyThresholds.ADEQUATE_MAX_CM,
    val transparencyCriticalBelowCm: Double = TransparencyThresholds.CRITICAL_BELOW_CM,
    val transparencyCriticalAboveCm: Double = TransparencyThresholds.CRITICAL_ABOVE_CM,
    val alkalinityAdequateMinMgL: Double = AlkalinityThresholds.ADEQUATE_MIN_MG_L,
    val alkalinityCriticalBelowMgL: Double = AlkalinityThresholds.CRITICAL_BELOW_MG_L,
    val hardnessAdequateMinMgL: Double = HardnessThresholds.ADEQUATE_MIN_MG_L,
) {

    /**
     * Campos com valor inválido; vazio quando as faixas podem ser salvas. Não há limite mínimo ou
     * máximo inventado: só se exige que cada grupo siga a ordem (ex.: emergência < crítico < adequado),
     * que todo valor seja positivo e que o pH fique na escala de 0 a 14.
     */
    fun invalidFields(): Set<WaterRangeField> {
        val invalid = mutableSetOf<WaterRangeField>()
        ascending(
            invalid,
            WaterRangeField.OXYGEN_EMERGENCY_BELOW to oxygenEmergencyBelowMgL,
            WaterRangeField.OXYGEN_CRITICAL_BELOW to oxygenCriticalBelowMgL,
            WaterRangeField.OXYGEN_ADEQUATE_MIN to oxygenAdequateMinMgL,
        )
        ascending(
            invalid,
            WaterRangeField.PH_CRITICAL_MIN to phCriticalMin,
            WaterRangeField.PH_ATTENTION_MIN to phAttentionMin,
            WaterRangeField.PH_ADEQUATE_MIN to phAdequateMin,
            WaterRangeField.PH_ADEQUATE_MAX to phAdequateMax,
            WaterRangeField.PH_ATTENTION_MAX to phAttentionMax,
            WaterRangeField.PH_CRITICAL_MAX to phCriticalMax,
        )
        if (!Plausibility.isPhOnScale(phCriticalMax)) invalid += WaterRangeField.PH_CRITICAL_MAX
        ascending(
            invalid,
            WaterRangeField.TEMPERATURE_CRITICAL_BELOW to temperatureCriticalBelowC,
            WaterRangeField.TEMPERATURE_ADEQUATE_MIN to temperatureAdequateMinC,
            WaterRangeField.TEMPERATURE_ADEQUATE_MAX to temperatureAdequateMaxC,
        )
        ascending(
            invalid,
            WaterRangeField.AMMONIA_ADEQUATE_BELOW to ammoniaAdequateBelowMgL,
            WaterRangeField.AMMONIA_CRITICAL_FROM to ammoniaCriticalFromMgL,
            WaterRangeField.AMMONIA_EMERGENCY_FROM to ammoniaEmergencyFromMgL,
        )
        ascending(
            invalid,
            WaterRangeField.NITRITE_ADEQUATE_BELOW to nitriteAdequateBelowMgL,
            WaterRangeField.NITRITE_CRITICAL_FROM to nitriteCriticalFromMgL,
            WaterRangeField.NITRITE_EMERGENCY_FROM to nitriteEmergencyFromMgL,
        )
        ascending(
            invalid,
            WaterRangeField.TRANSPARENCY_CRITICAL_BELOW to transparencyCriticalBelowCm,
            WaterRangeField.TRANSPARENCY_ADEQUATE_MIN to transparencyAdequateMinCm,
            WaterRangeField.TRANSPARENCY_ADEQUATE_MAX to transparencyAdequateMaxCm,
            WaterRangeField.TRANSPARENCY_CRITICAL_ABOVE to transparencyCriticalAboveCm,
        )
        ascending(
            invalid,
            WaterRangeField.ALKALINITY_CRITICAL_BELOW to alkalinityCriticalBelowMgL,
            WaterRangeField.ALKALINITY_ADEQUATE_MIN to alkalinityAdequateMinMgL,
        )
        ascending(invalid, WaterRangeField.HARDNESS_ADEQUATE_MIN to hardnessAdequateMinMgL)
        return invalid
    }

    /** Valor atual do [field]. */
    fun valueOf(field: WaterRangeField): Double = when (field) {
        WaterRangeField.OXYGEN_ADEQUATE_MIN -> oxygenAdequateMinMgL
        WaterRangeField.OXYGEN_CRITICAL_BELOW -> oxygenCriticalBelowMgL
        WaterRangeField.OXYGEN_EMERGENCY_BELOW -> oxygenEmergencyBelowMgL
        WaterRangeField.PH_ADEQUATE_MIN -> phAdequateMin
        WaterRangeField.PH_ADEQUATE_MAX -> phAdequateMax
        WaterRangeField.PH_ATTENTION_MIN -> phAttentionMin
        WaterRangeField.PH_ATTENTION_MAX -> phAttentionMax
        WaterRangeField.PH_CRITICAL_MIN -> phCriticalMin
        WaterRangeField.PH_CRITICAL_MAX -> phCriticalMax
        WaterRangeField.TEMPERATURE_ADEQUATE_MIN -> temperatureAdequateMinC
        WaterRangeField.TEMPERATURE_ADEQUATE_MAX -> temperatureAdequateMaxC
        WaterRangeField.TEMPERATURE_CRITICAL_BELOW -> temperatureCriticalBelowC
        WaterRangeField.AMMONIA_ADEQUATE_BELOW -> ammoniaAdequateBelowMgL
        WaterRangeField.AMMONIA_CRITICAL_FROM -> ammoniaCriticalFromMgL
        WaterRangeField.AMMONIA_EMERGENCY_FROM -> ammoniaEmergencyFromMgL
        WaterRangeField.NITRITE_ADEQUATE_BELOW -> nitriteAdequateBelowMgL
        WaterRangeField.NITRITE_CRITICAL_FROM -> nitriteCriticalFromMgL
        WaterRangeField.NITRITE_EMERGENCY_FROM -> nitriteEmergencyFromMgL
        WaterRangeField.TRANSPARENCY_ADEQUATE_MIN -> transparencyAdequateMinCm
        WaterRangeField.TRANSPARENCY_ADEQUATE_MAX -> transparencyAdequateMaxCm
        WaterRangeField.TRANSPARENCY_CRITICAL_BELOW -> transparencyCriticalBelowCm
        WaterRangeField.TRANSPARENCY_CRITICAL_ABOVE -> transparencyCriticalAboveCm
        WaterRangeField.ALKALINITY_ADEQUATE_MIN -> alkalinityAdequateMinMgL
        WaterRangeField.ALKALINITY_CRITICAL_BELOW -> alkalinityCriticalBelowMgL
        WaterRangeField.HARDNESS_ADEQUATE_MIN -> hardnessAdequateMinMgL
    }

    /** Cópia com o [field] trocado por [value]. */
    fun with(field: WaterRangeField, value: Double): WaterRanges = when (field) {
        WaterRangeField.OXYGEN_ADEQUATE_MIN -> copy(oxygenAdequateMinMgL = value)
        WaterRangeField.OXYGEN_CRITICAL_BELOW -> copy(oxygenCriticalBelowMgL = value)
        WaterRangeField.OXYGEN_EMERGENCY_BELOW -> copy(oxygenEmergencyBelowMgL = value)
        WaterRangeField.PH_ADEQUATE_MIN -> copy(phAdequateMin = value)
        WaterRangeField.PH_ADEQUATE_MAX -> copy(phAdequateMax = value)
        WaterRangeField.PH_ATTENTION_MIN -> copy(phAttentionMin = value)
        WaterRangeField.PH_ATTENTION_MAX -> copy(phAttentionMax = value)
        WaterRangeField.PH_CRITICAL_MIN -> copy(phCriticalMin = value)
        WaterRangeField.PH_CRITICAL_MAX -> copy(phCriticalMax = value)
        WaterRangeField.TEMPERATURE_ADEQUATE_MIN -> copy(temperatureAdequateMinC = value)
        WaterRangeField.TEMPERATURE_ADEQUATE_MAX -> copy(temperatureAdequateMaxC = value)
        WaterRangeField.TEMPERATURE_CRITICAL_BELOW -> copy(temperatureCriticalBelowC = value)
        WaterRangeField.AMMONIA_ADEQUATE_BELOW -> copy(ammoniaAdequateBelowMgL = value)
        WaterRangeField.AMMONIA_CRITICAL_FROM -> copy(ammoniaCriticalFromMgL = value)
        WaterRangeField.AMMONIA_EMERGENCY_FROM -> copy(ammoniaEmergencyFromMgL = value)
        WaterRangeField.NITRITE_ADEQUATE_BELOW -> copy(nitriteAdequateBelowMgL = value)
        WaterRangeField.NITRITE_CRITICAL_FROM -> copy(nitriteCriticalFromMgL = value)
        WaterRangeField.NITRITE_EMERGENCY_FROM -> copy(nitriteEmergencyFromMgL = value)
        WaterRangeField.TRANSPARENCY_ADEQUATE_MIN -> copy(transparencyAdequateMinCm = value)
        WaterRangeField.TRANSPARENCY_ADEQUATE_MAX -> copy(transparencyAdequateMaxCm = value)
        WaterRangeField.TRANSPARENCY_CRITICAL_BELOW -> copy(transparencyCriticalBelowCm = value)
        WaterRangeField.TRANSPARENCY_CRITICAL_ABOVE -> copy(transparencyCriticalAboveCm = value)
        WaterRangeField.ALKALINITY_ADEQUATE_MIN -> copy(alkalinityAdequateMinMgL = value)
        WaterRangeField.ALKALINITY_CRITICAL_BELOW -> copy(alkalinityCriticalBelowMgL = value)
        WaterRangeField.HARDNESS_ADEQUATE_MIN -> copy(hardnessAdequateMinMgL = value)
    }

    /** Marca o campo que não é positivo (o primeiro da ordem) ou que não é maior que o anterior. */
    private fun ascending(invalid: MutableSet<WaterRangeField>, vararg chain: Pair<WaterRangeField, Double>) {
        var previous = 0.0
        for ((field, value) in chain) {
            if (!value.isFinite() || value <= previous) invalid += field
            previous = value
        }
    }

    companion object {
        /** Faixas de fábrica: as constantes com fonte. */
        val DEFAULT = WaterRanges()
    }
}
