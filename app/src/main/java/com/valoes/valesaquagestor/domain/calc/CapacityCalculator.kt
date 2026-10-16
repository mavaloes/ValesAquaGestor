package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.model.FaseLote
import kotlin.math.floor

/**
 * Condição de criação do viveiro (1 a 4 da tabela F03 p.15), derivada de aeração e troca de água.
 */
enum class RearingCondition {
    /** Sem aerador, sem troca de água. */
    NO_AERATOR_NO_EXCHANGE,

    /** Com aerador, sem troca de água. */
    AERATOR_NO_EXCHANGE,

    /** Sem aerador, com troca de 5 a 10% ao dia. */
    NO_AERATOR_WITH_EXCHANGE,

    /** Com aerador e troca de 5 a 10% ao dia. */
    AERATOR_WITH_EXCHANGE,
}

/**
 * Produtividade em kg de peixe por 1.000 m².
 *
 * @property minKg limite inferior (mais conservador).
 * @property maxKg limite superior (padrão do app, como nos exemplos do manual; 🔶 `08`, conflito 9).
 */
data class CapacityRange(val minKg: Double, val maxKg: Double)

/**
 * Capacidade do viveiro e número máximo de peixes (`specs/03-regras-negocio.md`, seção 4.5).
 * A capacidade é por **área**; vale para viveiro escavado e açude.
 */
object CapacityCalculator {

    /** @see F03 p.15 — troca de água a partir de 5% ao dia conta como "com troca". */
    const val WATER_EXCHANGE_MIN_PCT_DAY = 5.0

    /** Tabela F03 p.15 (colunas conferidas olhando a imagem), em kg por 1.000 m². */
    private val table: Map<RearingCondition, Map<FaseLote, CapacityRange>> = mapOf(
        RearingCondition.NO_AERATOR_NO_EXCHANGE to mapOf(
            FaseLote.ALEVINAGEM to CapacityRange(100.0, 120.0),
            FaseLote.RECRIA to CapacityRange(200.0, 250.0),
            FaseLote.ENGORDA to CapacityRange(500.0, 600.0),
        ),
        RearingCondition.AERATOR_NO_EXCHANGE to mapOf(
            FaseLote.ALEVINAGEM to CapacityRange(140.0, 180.0),
            FaseLote.RECRIA to CapacityRange(280.0, 330.0),
            FaseLote.ENGORDA to CapacityRange(800.0, 1000.0),
        ),
        RearingCondition.NO_AERATOR_WITH_EXCHANGE to mapOf(
            FaseLote.ALEVINAGEM to CapacityRange(140.0, 180.0),
            FaseLote.RECRIA to CapacityRange(280.0, 330.0),
            FaseLote.ENGORDA to CapacityRange(800.0, 1000.0),
        ),
        RearingCondition.AERATOR_WITH_EXCHANGE to mapOf(
            FaseLote.ALEVINAGEM to CapacityRange(200.0, 350.0),
            FaseLote.RECRIA to CapacityRange(350.0, 600.0),
            FaseLote.ENGORDA to CapacityRange(1000.0, 2000.0),
        ),
    )

    /** Condição de criação: `usaAerador` × (`renovacaoAguaPctDia` ≥ 5). */
    fun condition(usesAerator: Boolean, waterExchangePctDay: Double): RearingCondition {
        val exchange = waterExchangePctDay >= WATER_EXCHANGE_MIN_PCT_DAY
        return when {
            usesAerator && exchange -> RearingCondition.AERATOR_WITH_EXCHANGE
            usesAerator -> RearingCondition.AERATOR_NO_EXCHANGE
            exchange -> RearingCondition.NO_AERATOR_WITH_EXCHANGE
            else -> RearingCondition.NO_AERATOR_NO_EXCHANGE
        }
    }

    /** Faixa de produtividade (kg por 1.000 m²) da condição e da fase. */
    fun productivityRange(condition: RearingCondition, phase: FaseLote): CapacityRange =
        table.getValue(condition).getValue(phase)

    /**
     * Capacidade em kg: `produtividade (kg/1.000 m²) × areaM2 / 1000`. Com [useUpperLimit] `true`
     * (padrão) usa o limite superior da faixa; com `false`, o inferior (mais conservador).
     */
    fun capacityKg(areaM2: Double, condition: RearingCondition, phase: FaseLote, useUpperLimit: Boolean = true): Double {
        val range = productivityRange(condition, phase)
        val productivity = if (useUpperLimit) range.maxKg else range.minKg
        return productivity * areaM2 / 1000.0
    }

    /**
     * Número máximo de peixes: `capacidadeKg / pesoFinal`, arredondado para baixo. O peso final
     * entra em gramas para a divisão ser exata (ex.: 27 kg e 20 g dão exatamente 1.350).
     */
    fun maxFish(capacityKg: Double, finalWeightG: Double): Int =
        floor(capacityKg * 1000.0 / finalWeightG + ROUNDING_GUARD).toInt()

    /** Tolerância para o erro de ponto flutuante não derrubar um resultado exato para o inteiro abaixo. */
    private const val ROUNDING_GUARD = 1e-9
}
