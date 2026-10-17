package com.valoes.valesaquagestor.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.valoes.valesaquagestor.data.local.entity.BiometriaEntity
import com.valoes.valesaquagestor.data.local.entity.LoteEntity
import com.valoes.valesaquagestor.data.local.entity.MovimentacaoLoteEntity

/**
 * Lote com biometrias e movimentações (Lote 1—N Biometria e Lote 1—N MovimentacaoLote): é o que
 * basta para calcular quantidade atual, biomassa e mortalidade acumulada.
 *
 * Atenção: `@Relation` não filtra a exclusão lógica. [biometrias] e [movimentacoes] trazem também os
 * registros com `excluido = true`, e quem usa esta classe precisa filtrá-los. Os DAOs da Fase 03 usam consultas próprias, já filtradas, e não esta classe.
 */
data class LoteCompleto(
    @Embedded val lote: LoteEntity,
    @Relation(parentColumn = "id", entityColumn = "loteId")
    val biometrias: List<BiometriaEntity>,
    @Relation(parentColumn = "id", entityColumn = "loteId")
    val movimentacoes: List<MovimentacaoLoteEntity>,
)
