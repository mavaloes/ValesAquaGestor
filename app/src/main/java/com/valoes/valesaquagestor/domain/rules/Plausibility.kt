package com.valoes.valesaquagestor.domain.rules

/**
 * Valor improvável: a tela avisa e oferece "Salvar mesmo assim". Não confundir com valor
 * **impossível** (negativo, pH fora de 0 a 14), que é bloqueado.
 */
enum class PlausibilityWarning {
    /** pH acima de 10. */
    PH_VERY_HIGH,

    /** Oxigênio acima de 15 mg/L: supersaturação, risco de "trauma da bolha de gás". */
    OXYGEN_SUPERSATURATED,

    /** Peso médio 10 vezes maior que o do lote: provável erro de digitação ou de pesagem. */
    AVERAGE_WEIGHT_JUMP,
}

/** Regras de valor improvável (`specs/08-fontes-e-conflitos.md`, conflito 15). */
object Plausibility {

    /** @see specs/04-requisitos-ux.md, seção 5 — exemplo "pH > 10". */
    const val PH_IMPROBABLE_ABOVE = 10.0

    /** @see F11 p.14 — oxigênio acima de 15 mg/L é supersaturação. */
    const val OXYGEN_IMPROBABLE_ABOVE_MG_L = 15.0

    /** @see .claude/skills/fish-lot-records/SKILL.md — "peso médio 10 vezes maior que o da última biometria". */
    const val WEIGHT_JUMP_FACTOR = 10.0

    /** Início da escala de pH (definição química). */
    const val PH_SCALE_MIN = 0.0

    /** Fim da escala de pH (definição química). */
    const val PH_SCALE_MAX = 14.0

    /** `true` se o pH existe na escala (de 0 a 14, inclusive). Fora dela o valor é impossível. */
    fun isPhOnScale(ph: Double): Boolean = ph in PH_SCALE_MIN..PH_SCALE_MAX

    /** Avisos de uma medição de água. Valores `null` (não medidos) não geram aviso. */
    fun checkWater(ph: Double?, oxygenMgL: Double?): List<PlausibilityWarning> = listOfNotNull(
        PlausibilityWarning.PH_VERY_HIGH.takeIf { ph != null && ph > PH_IMPROBABLE_ABOVE },
        PlausibilityWarning.OXYGEN_SUPERSATURATED.takeIf { oxygenMgL != null && oxygenMgL > OXYGEN_IMPROBABLE_ABOVE_MG_L },
    )

    /**
     * Aviso se o peso médio medido é [WEIGHT_JUMP_FACTOR] vezes (ou mais) o peso médio atual do lote.
     * Sem peso de referência positivo, não avisa.
     */
    fun checkAverageWeight(newAverageG: Double, currentAverageG: Double): PlausibilityWarning? =
        if (currentAverageG > 0.0 && newAverageG >= currentAverageG * WEIGHT_JUMP_FACTOR) {
            PlausibilityWarning.AVERAGE_WEIGHT_JUMP
        } else {
            null
        }
}
