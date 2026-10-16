package com.valoes.valesaquagestor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.valoes.valesaquagestor.data.local.entity.MovimentacaoLoteEntity
import kotlinx.coroutines.flow.Flow

/** Acesso à tabela `movimentacao_lote`. Consultas observáveis sempre filtram `excluido = 0`. */
@Dao
interface MovimentacaoLoteDao {
    /** Movimentações do lote, da mais antiga à mais nova. */
    @Query("SELECT * FROM movimentacao_lote WHERE loteId = :loteId AND excluido = 0 ORDER BY dataHora, id")
    fun observeByLote(loteId: Long): Flow<List<MovimentacaoLoteEntity>>

    @Insert
    suspend fun insert(entity: MovimentacaoLoteEntity): Long

    @Update
    suspend fun update(entity: MovimentacaoLoteEntity)

    /** Busca pelo id **incluindo excluídos**: uso interno dos repositórios (editar e excluir). */
    @Query("SELECT * FROM movimentacao_lote WHERE id = :id")
    suspend fun getByIdIncludingDeleted(id: Long): MovimentacaoLoteEntity?

    /** Busca pelo `uuid` **incluindo excluídos**: usada na deduplicação da sincronização. */
    @Query("SELECT * FROM movimentacao_lote WHERE uuid = :uuid")
    suspend fun findByUuid(uuid: String): MovimentacaoLoteEntity?

    /** Registros com alteração ainda não enviada. **Inclui excluídos**: a exclusão também precisa ser enviada. */
    @Query("SELECT * FROM movimentacao_lote WHERE sincronizado = 0")
    suspend fun getPending(): List<MovimentacaoLoteEntity>
}
