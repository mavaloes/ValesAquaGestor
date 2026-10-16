package com.valoes.valesaquagestor.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.valoes.valesaquagestor.data.local.entity.LoteEntity
import com.valoes.valesaquagestor.data.local.entity.TanqueEntity

/**
 * Tanque com todos os seus lotes (Tanque 1—N Lote).
 *
 * Atenção: `@Relation` não filtra a exclusão lógica. [lotes] traz também os registros com
 * `excluido = true`, e quem usa esta classe precisa filtrá-los. Os DAOs da Fase 03 usam consultas próprias, já filtradas, e não esta classe.
 */
data class TanqueComLotes(
    @Embedded val tanque: TanqueEntity,
    @Relation(parentColumn = "id", entityColumn = "tanqueId")
    val lotes: List<LoteEntity>,
)
