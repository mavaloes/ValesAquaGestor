package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao

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
}
