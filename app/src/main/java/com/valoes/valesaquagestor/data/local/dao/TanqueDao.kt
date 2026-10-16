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
}
