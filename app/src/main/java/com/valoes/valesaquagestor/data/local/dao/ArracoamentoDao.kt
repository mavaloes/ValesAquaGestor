package com.valoes.valesaquagestor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.valoes.valesaquagestor.data.local.entity.ArracoamentoEntity
import kotlinx.coroutines.flow.Flow

/** Acesso à tabela `arracoamento`. Consultas observáveis sempre filtram `excluido = 0`. */
@Dao
interface ArracoamentoDao {
    /** Tratos do lote com `from <= dataHora < to`, do mais antigo ao mais novo. */
    @Query("SELECT * FROM arracoamento WHERE loteId = :loteId AND excluido = 0 AND dataHora >= :from AND dataHora < :to ORDER BY dataHora, id")
    fun observeByLote(loteId: Long, from: Long, to: Long): Flow<List<ArracoamentoEntity>>

    /** Os [limit] tratos mais recentes do lote, do mais novo ao mais antigo. */
    @Query("SELECT * FROM arracoamento WHERE loteId = :loteId AND excluido = 0 ORDER BY dataHora DESC, id DESC LIMIT :limit")
    fun observeRecent(loteId: Long, limit: Int): Flow<List<ArracoamentoEntity>>

    /** O trato mais recente do lote, ou `null`. */
    @Query("SELECT * FROM arracoamento WHERE loteId = :loteId AND excluido = 0 ORDER BY dataHora DESC, id DESC LIMIT 1")
    suspend fun getLast(loteId: Long): ArracoamentoEntity?

    @Insert
    suspend fun insert(entity: ArracoamentoEntity): Long

    @Update
    suspend fun update(entity: ArracoamentoEntity)

    /** Busca pelo id **incluindo excluídos**: uso interno dos repositórios (editar e excluir). */
    @Query("SELECT * FROM arracoamento WHERE id = :id")
    suspend fun getByIdIncludingDeleted(id: Long): ArracoamentoEntity?

    /** Busca pelo `uuid` **incluindo excluídos**: usada na deduplicação da sincronização. */
    @Query("SELECT * FROM arracoamento WHERE uuid = :uuid")
    suspend fun findByUuid(uuid: String): ArracoamentoEntity?

    /** Registros com alteração ainda não enviada. **Inclui excluídos**: a exclusão também precisa ser enviada. */
    @Query("SELECT * FROM arracoamento WHERE sincronizado = 0")
    suspend fun getPending(): List<ArracoamentoEntity>

    /** Quantos registros têm alteração ainda não enviada (para avisar antes de sair da conta). */
    @Query("SELECT COUNT(*) FROM arracoamento WHERE sincronizado = 0")
    fun observePendingCount(): Flow<Int>
}
