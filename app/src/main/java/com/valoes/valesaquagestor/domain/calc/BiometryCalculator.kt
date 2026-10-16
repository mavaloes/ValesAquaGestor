package com.valoes.valesaquagestor.domain.calc

/**
 * Uma pesagem da biometria (Planilha 6): o balde ou puçá com os peixes, o peso do recipiente
 * sozinho (tara) e quantos peixes foram pesados.
 *
 * @property grossKg peso com os peixes, em kg.
 * @property tareKg peso do recipiente com água (balde) ou do puçá molhado, em kg.
 * @property fishCount número de peixes da pesagem.
 */
data class Weighing(val grossKg: Double, val tareKg: Double, val fishCount: Int) {
    /** Peso só dos peixes, em kg. */
    val netKg: Double get() = grossKg - tareKg
}

/**
 * Totais de uma biometria.
 *
 * @property netWeightKg peso total dos peixes pesados, sem a tara, em kg.
 * @property fishCount total de peixes pesados.
 * @property averageWeightG peso médio, em g.
 */
data class BiometrySummary(val netWeightKg: Double, val fishCount: Int, val averageWeightG: Double)

/** Cálculos da biometria (`specs/02-dominio.md`, entidade Biometria). */
object BiometryCalculator {

    /** @see F03 p.106 — lote de até cerca de 2.000 peixes: amostra de cerca de 10%. */
    const val SMALL_LOT_MAX_FISH = 2000

    /** @see F03 p.106 — em lote pequeno, a amostra é de cerca de 10% dos peixes. */
    const val SMALL_LOT_SAMPLE_PCT = 10

    /** @see F03 p.106 — em lote maior que 2.000 peixes, a amostra é de cerca de 200 peixes. */
    const val LARGE_LOT_SAMPLE_FISH = 200

    /**
     * Soma as pesagens e calcula o peso médio: `Σ(bruto − tara) / Σ peixes × 1000`.
     * Devolve `null` se não há peixes ou se o peso dos peixes não é positivo.
     */
    fun summarize(weighings: List<Weighing>): BiometrySummary? {
        val fish = weighings.sumOf { it.fishCount }
        val netKg = weighings.sumOf { it.netKg }
        if (fish <= 0 || netKg <= 0.0) return null
        return BiometrySummary(netKg, fish, netKg / fish * 1000.0)
    }

    /**
     * Tamanho de amostra recomendado: cerca de 10% dos peixes (arredondado para cima) em lote de até
     * 2.000 peixes; cerca de 200 peixes em lote maior.
     *
     * @param fishInLot peixes vivos do lote.
     */
    fun recommendedSample(fishInLot: Int): Int =
        if (fishInLot <= SMALL_LOT_MAX_FISH) {
            (fishInLot * SMALL_LOT_SAMPLE_PCT + PERCENT_ROUNDING) / PERCENT
        } else {
            LARGE_LOT_SAMPLE_FISH
        }

    private const val PERCENT = 100
    private const val PERCENT_ROUNDING = PERCENT - 1
}
