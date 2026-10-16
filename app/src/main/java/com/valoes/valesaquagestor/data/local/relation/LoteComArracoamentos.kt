package com.valoes.valesaquagestor.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.valoes.valesaquagestor.data.local.entity.Arracoamento
import com.valoes.valesaquagestor.data.local.entity.Lote

/**
 * Lote com todos os seus tratos (Lote 1—N Arracoamento).
 *
 * Atenção: `@Relation` não filtra a exclusão lógica. [arracoamentos] traz também os registros com
 * `excluido = true`, e quem usa esta classe precisa filtrá-los (Fase 03).
 */
data class LoteComArracoamentos(
    @Embedded val lote: Lote,
    @Relation(parentColumn = "id", entityColumn = "loteId")
    val arracoamentos: List<Arracoamento>,
)
