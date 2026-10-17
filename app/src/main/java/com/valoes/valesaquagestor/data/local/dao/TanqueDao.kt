package com.valoes.valesaquagestor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.valoes.valesaquagestor.data.local.entity.TanqueEntity
import kotlinx.coroutines.flow.Flow

/** Acesso à tabela `tanque`. Consultas observáveis sempre filtram `excluido = 0`. */
@Dao
interface TanqueDao {
    /** Tanques com status ATIVO, ordenados por nome. */
    @Query("SELECT * FROM tanque WHERE excluido = 0 AND status = 'ATIVO' ORDER BY nome")
    fun observeActive(): Flow<List<TanqueEntity>>

    @Query("SELECT * FROM tanque WHERE id = :id AND excluido = 0")
    fun observeById(id: Long): Flow<TanqueEntity?>

    @Insert
    suspend fun insert(entity: TanqueEntity): Long

    @Update
    suspend fun update(entity: TanqueEntity)

    /** Busca pelo id **incluindo excluídos**: uso interno dos repositórios (editar e excluir). */
    @Query("SELECT * FROM tanque WHERE id = :id")
    suspend fun getByIdIncludingDeleted(id: Long): TanqueEntity?

    /** Busca pelo `uuid` **incluindo excluídos**: usada na deduplicação da sincronização. */
    @Query("SELECT * FROM tanque WHERE uuid = :uuid")
    suspend fun findByUuid(uuid: String): TanqueEntity?

    /** Registros com alteração ainda não enviada. **Inclui excluídos**: a exclusão também precisa ser enviada. */
    @Query("SELECT * FROM tanque WHERE sincronizado = 0")
    suspend fun getPending(): List<TanqueEntity>

    /** Quantos registros têm alteração ainda não enviada (para avisar antes de sair da conta). */
    @Query("SELECT COUNT(*) FROM tanque WHERE sincronizado = 0")
    fun observePendingCount(): Flow<Int>

    /**
     * Marca como enviado o registro de [uuid] **só se** ele ainda está na versão enviada
     * ([atualizadoEm]): uma edição feita durante o envio continua pendente.
     */
    @Query("UPDATE tanque SET sincronizado = 1 WHERE uuid = :uuid AND atualizadoEm = :atualizadoEm")
    suspend fun markSynced(uuid: String, atualizadoEm: Long)

    /** `uuid` dos registros excluídos cuja exclusão o servidor já confirmou (podem sair do banco). */
    @Query("SELECT uuid FROM tanque WHERE excluido = 1 AND sincronizado = 1")
    suspend fun confirmedDeletedUuids(): List<String>

    /** Remove de vez um registro excluído e confirmado. Falha se outra tabela ainda aponta para ele. */
    @Query("DELETE FROM tanque WHERE uuid = :uuid AND excluido = 1 AND sincronizado = 1")
    suspend fun purge(uuid: String)
}
