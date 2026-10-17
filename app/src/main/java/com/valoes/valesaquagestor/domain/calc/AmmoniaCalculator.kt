package com.valoes.valesaquagestor.domain.calc

import kotlin.math.pow

/**
 * Amônia tóxica (NH₃) a partir da amônia total, do pH e da temperatura.
 *
 * O kit de campo mede a amônia **total** (NH₃ + NH₄⁺); a tóxica é a fração NH₃, que depende do pH
 * e da temperatura (F13 p.36-38). Usa a fórmula de equilíbrio químico, conferida contra a tabela da
 * F13 p.37 (erro máximo de 0,3 ponto percentual; 🔶 `specs/03-regras-negocio.md`, seção 4.1).
 */
object AmmoniaCalculator {

    /** @see F13 p.37 — constante da fórmula de pKa, conferida contra a tabela (🔶 `03`, seção 4.1). */
    private const val PKA_BASE = 0.09018

    /** @see F13 p.37 — constante da fórmula de pKa, conferida contra a tabela (🔶 `03`, seção 4.1). */
    private const val PKA_TEMPERATURE_TERM = 2729.92

    /** Zero Celsius em kelvin (constante física). */
    private const val KELVIN_OFFSET = 273.15

    /** Fração de NH₃ na amônia total, em % (0 a 100). */
    fun nh3FractionPct(ph: Double, celsius: Double): Double {
        val pKa = PKA_BASE + PKA_TEMPERATURE_TERM / (celsius + KELVIN_OFFSET)
        return 100.0 / (1.0 + 10.0.pow(pKa - ph))
    }

    /** Amônia tóxica, em mg/L: `total × fração(pH, °C) / 100`. */
    fun toxicMgL(totalMgL: Double, ph: Double, celsius: Double): Double =
        totalMgL * nh3FractionPct(ph, celsius) / 100.0
}
