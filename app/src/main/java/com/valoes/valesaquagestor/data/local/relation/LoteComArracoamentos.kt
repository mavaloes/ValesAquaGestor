package com.valoes.valesaquagestor.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.valoes.valesaquagestor.data.local.entity.ArracoamentoEntity
import com.valoes.valesaquagestor.data.local.entity.LoteEntity

/**
 * Lote com todos os seus tratos (Lote 1—N Arracoamento).
 *
 * Atenção: `@Relation` não filtra a exclusão lógica. [arracoamentos] traz também os registros com
 * `excluido = true`, e quem usa esta classe precisa filtrá-los. Os DAOs da Fase 03 usam consultas próprias, já filtradas, e não esta classe.
 */
data class LoteComArracoamentos(
    @Embedded val lote: LoteEntity,
    @Relation(parentColumn = "id", entityColumn = "loteId")
    val arracoamentos: List<ArracoamentoEntity>,
)
