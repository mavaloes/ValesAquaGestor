package com.valoes.valesaquagestor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.valoes.valesaquagestor.data.local.entity.LoteEntity
import kotlinx.coroutines.flow.Flow

/** Acesso à tabela `lote`. Consultas observáveis sempre filtram `excluido = 0`. */
@Dao
interface LoteDao {
    /** Lotes ATIVOS de um tanque, do mais recente ao mais antigo. */
    @Query("SELECT * FROM lote WHERE tanqueId = :tanqueId AND excluido = 0 AND status = 'ATIVO' ORDER BY dataPovoamento DESC, id DESC")
    fun observeActiveByTanque(tanqueId: Long): Flow<List<LoteEntity>>

    @Query("SELECT * FROM lote WHERE excluido = 0 AND status = 'ATIVO' ORDER BY dataPovoamento DESC, id DESC")
    fun observeAllActive(): Flow<List<LoteEntity>>

    @Query("SELECT * FROM lote WHERE id = :id AND excluido = 0")
    fun observeById(id: Long): Flow<LoteEntity?>

    @Insert
    suspend fun insert(entity: LoteEntity): Long

    @Update
    suspend fun update(entity: LoteEntity)

    /** Busca pelo id **incluindo excluídos**: uso interno dos repositórios (editar e excluir). */
    @Query("SELECT * FROM lote WHERE id = :id")
    suspend fun getByIdIncludingDeleted(id: Long): LoteEntity?

    /** Busca pelo `uuid` **incluindo excluídos**: usada na deduplicação da sincronização. */
    @Query("SELECT * FROM lote WHERE uuid = :uuid")
    suspend fun findByUuid(uuid: String): LoteEntity?

    /** Registros com alteração ainda não enviada. **Inclui excluídos**: a exclusão também precisa ser enviada. */
    @Query("SELECT * FROM lote WHERE sincronizado = 0")
    suspend fun getPending(): List<LoteEntity>

    /** Quantos registros têm alteração ainda não enviada (para avisar antes de sair da conta). */
    @Query("SELECT COUNT(*) FROM lote WHERE sincronizado = 0")
    fun observePendingCount(): Flow<Int>

    /**
     * Marca como enviado o registro de [uuid] **só se** ele ainda está na versão enviada
     * ([atualizadoEm]): uma edição feita durante o envio continua pendente.
     */
    @Query("UPDATE lote SET sincronizado = 1 WHERE uuid = :uuid AND atualizadoEm = :atualizadoEm")
    suspend fun markSynced(uuid: String, atualizadoEm: Long)

    /** `uuid` dos registros excluídos cuja exclusão o servidor já confirmou (podem sair do banco). */
    @Query("SELECT uuid FROM lote WHERE excluido = 1 AND sincronizado = 1")
    suspend fun confirmedDeletedUuids(): List<String>

    /** Remove de vez um registro excluído e confirmado. Falha se outra tabela ainda aponta para ele. */
    @Query("DELETE FROM lote WHERE uuid = :uuid AND excluido = 1 AND sincronizado = 1")
    suspend fun purge(uuid: String)
}
