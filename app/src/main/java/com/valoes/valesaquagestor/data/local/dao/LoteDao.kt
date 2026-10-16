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
}
