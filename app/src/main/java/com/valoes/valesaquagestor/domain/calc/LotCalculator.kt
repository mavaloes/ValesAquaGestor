package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.model.Arracoamento
import com.valoes.valesaquagestor.domain.model.Biometria
import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import java.time.Instant

/**
 * Valores derivados do lote. Nunca são gravados: assim nenhum número fica errado depois de uma
 * edição feita offline (`specs/02-dominio.md`, entidade Lote).
 *
 * Quem chama passa só as movimentações **não excluídas** (os repositórios já entregam assim).
 */
object LotCalculator {

    /**
     * Peixes vivos: `quantidadeInicial − Σ(MORTALIDADE + DESPESCA_PARCIAL + DESPESCA_FINAL + TRANSFERENCIA)`.
     */
    fun currentQuantity(lote: Lote, movements: List<MovimentacaoLote>): Int =
        lote.quantidadeInicial - movements.sumOf { it.quantidade }

    /** Biomassa em kg: `quantidadeAtual × pesoMedioAtualG / 1000`. */
    fun biomassKg(lote: Lote, movements: List<MovimentacaoLote>): Double =
        FeedingCalculator.biomassKg(currentQuantity(lote, movements), lote.pesoMedioAtualG)

    /** Total de peixes mortos no ciclo (só as movimentações `MORTALIDADE`). */
    fun deadCount(movements: List<MovimentacaoLote>): Int =
        movements.filter { it.tipo == TipoMovimentacao.MORTALIDADE }.sumOf { it.quantidade }

    /** Mortalidade acumulada do ciclo, em % da quantidade inicial. `0` se o lote não tem peixes iniciais. */
    fun accumulatedMortalityPct(lote: Lote, movements: List<MovimentacaoLote>): Double =
        if (lote.quantidadeInicial > 0) deadCount(movements) * 100.0 / lote.quantidadeInicial else 0.0

    /**
     * Período em que a TCA é medida: da biometria anterior (ou do povoamento, se só há uma
     * biometria) até a biometria mais recente. `null` se o lote não tem biometria.
     *
     * @param latestBiometries biometrias da mais nova para a mais antiga.
     */
    fun fcrWindow(lote: Lote, latestBiometries: List<Biometria>): Pair<Instant, Instant>? {
        val current = latestBiometries.firstOrNull() ?: return null
        val start = latestBiometries.getOrNull(1)?.dataHora ?: lote.dataPovoamento
        return start to current.dataHora
    }

    /**
     * TCA (taxa de conversão alimentar) estimada no período de [fcrWindow]: ração oferecida
     * (sem os tratos suspensos) dividida pelo ganho de peso. O ganho usa os peixes vivos na data da
     * biometria mais recente; é uma estimativa, porque não pesa os peixes que saíram no período.
     * `null` se não há biometria, ração no período ou ganho de peso positivo.
     *
     * @param latestBiometries biometrias da mais nova para a mais antiga.
     * @param feedings tratos do lote (os de fora do período são ignorados).
     * @see F14 p.48
     */
    fun estimatedFcr(
        lote: Lote,
        movements: List<MovimentacaoLote>,
        latestBiometries: List<Biometria>,
        feedings: List<Arracoamento>,
    ): Double? {
        val current = latestBiometries.firstOrNull() ?: return null
        val previous = latestBiometries.getOrNull(1)
        val (start, end) = fcrWindow(lote, latestBiometries) ?: return null
        val previousWeightG = previous?.pesoMedioG ?: lote.pesoMedioInicialG
        val alive = lote.quantidadeInicial - movements.filter { !it.dataHora.isAfter(end) }.sumOf { it.quantidade }
        val gainKg = FeedingCalculator.weightGainKg(alive, previousWeightG, current.pesoMedioG)
        val feedKg = feedings
            .filter { !it.suspenso && it.dataHora.isAfter(start) && !it.dataHora.isAfter(end) }
            .sumOf { it.quantidadeKg }
        if (feedKg <= 0.0) return null
        return FeedingCalculator.realFcr(feedKg, gainKg)
    }
}
