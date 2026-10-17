package com.valoes.valesaquagestor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.valoes.valesaquagestor.data.local.entity.BiometriaEntity
import kotlinx.coroutines.flow.Flow

/** Acesso à tabela `biometria`. Consultas observáveis sempre filtram `excluido = 0`. */
@Dao
interface BiometriaDao {
    /** As [limit] biometrias mais recentes do lote, da mais nova à mais antiga. */
    @Query("SELECT * FROM biometria WHERE loteId = :loteId AND excluido = 0 ORDER BY dataHora DESC, id DESC LIMIT :limit")
    fun observeLatest(loteId: Long, limit: Int): Flow<List<BiometriaEntity>>

    /** A biometria mais recente do lote, ou `null`. */
    @Query("SELECT * FROM biometria WHERE loteId = :loteId AND excluido = 0 ORDER BY dataHora DESC, id DESC LIMIT 1")
    suspend fun getLatest(loteId: Long): BiometriaEntity?

    @Insert
    suspend fun insert(entity: BiometriaEntity): Long

    @Update
    suspend fun update(entity: BiometriaEntity)

    /** Busca pelo id **incluindo excluídos**: uso interno dos repositórios (editar e excluir). */
    @Query("SELECT * FROM biometria WHERE id = :id")
    suspend fun getByIdIncludingDeleted(id: Long): BiometriaEntity?

    /** Busca pelo `uuid` **incluindo excluídos**: usada na deduplicação da sincronização. */
    @Query("SELECT * FROM biometria WHERE uuid = :uuid")
    suspend fun findByUuid(uuid: String): BiometriaEntity?

    /** Registros com alteração ainda não enviada. **Inclui excluídos**: a exclusão também precisa ser enviada. */
    @Query("SELECT * FROM biometria WHERE sincronizado = 0")
    suspend fun getPending(): List<BiometriaEntity>

    /** Quantos registros têm alteração ainda não enviada (para avisar antes de sair da conta). */
    @Query("SELECT COUNT(*) FROM biometria WHERE sincronizado = 0")
    fun observePendingCount(): Flow<Int>

    /**
     * Marca como enviado o registro de [uuid] **só se** ele ainda está na versão enviada
     * ([atualizadoEm]): uma edição feita durante o envio continua pendente.
     */
    @Query("UPDATE biometria SET sincronizado = 1 WHERE uuid = :uuid AND atualizadoEm = :atualizadoEm")
    suspend fun markSynced(uuid: String, atualizadoEm: Long)

    /** `uuid` dos registros excluídos cuja exclusão o servidor já confirmou (podem sair do banco). */
    @Query("SELECT uuid FROM biometria WHERE excluido = 1 AND sincronizado = 1")
    suspend fun confirmedDeletedUuids(): List<String>

    /** Remove de vez um registro excluído e confirmado. Falha se outra tabela ainda aponta para ele. */
    @Query("DELETE FROM biometria WHERE uuid = :uuid AND excluido = 1 AND sincronizado = 1")
    suspend fun purge(uuid: String)
}
