package com.valoes.valesaquagestor.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.valoes.valesaquagestor.data.local.entity.Lote
import com.valoes.valesaquagestor.data.local.entity.Tanque

/**
 * Tanque com todos os seus lotes (Tanque 1—N Lote).
 *
 * Atenção: `@Relation` não filtra a exclusão lógica. [lotes] traz também os registros com
 * `excluido = true`, e quem usa esta classe precisa filtrá-los (Fase 03).
 */
data class TanqueComLotes(
    @Embedded val tanque: Tanque,
    @Relation(parentColumn = "id", entityColumn = "tanqueId")
    val lotes: List<Lote>,
)
