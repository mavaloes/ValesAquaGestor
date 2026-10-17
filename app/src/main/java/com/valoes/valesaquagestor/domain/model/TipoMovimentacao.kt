package com.valoes.valesaquagestor.domain.model

/**
 * Tipo de saída de peixes do lote (Planilha 7). Toda movimentação diminui a quantidade atual do lote.
 *
 * @see F01 p.156
 */
enum class TipoMovimentacao {
    /** Peixes mortos retirados e contados. */
    MORTALIDADE,

    /** Retirada para vender, classificar ou ajustar a densidade. */
    DESPESCA_PARCIAL,

    /** Retirada de todos os peixes; encerra o lote. */
    DESPESCA_FINAL,

    /** Peixes levados para outro tanque. No MVP só se registra a baixa (🔶 `specs/08`, seção 5, item 4). */
    TRANSFERENCIA,
}
