package com.valoes.valesaquagestor.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.valoes.valesaquagestor.data.local.entity.Biometria
import com.valoes.valesaquagestor.data.local.entity.Lote
import com.valoes.valesaquagestor.data.local.entity.MovimentacaoLote

/**
 * Lote com biometrias e movimentações (Lote 1—N Biometria e Lote 1—N MovimentacaoLote): é o que
 * basta para calcular quantidade atual, biomassa e mortalidade acumulada.
 *
 * Atenção: `@Relation` não filtra a exclusão lógica. [biometrias] e [movimentacoes] trazem também os
 * registros com `excluido = true`, e quem usa esta classe precisa filtrá-los (Fase 03).
 */
data class LoteCompleto(
    @Embedded val lote: Lote,
    @Relation(parentColumn = "id", entityColumn = "loteId")
    val biometrias: List<Biometria>,
    @Relation(parentColumn = "id", entityColumn = "loteId")
    val movimentacoes: List<MovimentacaoLote>,
)
