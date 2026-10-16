package com.valoes.valesaquagestor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.valoes.valesaquagestor.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

/** Acesso à tabela `usuario`. Consultas observáveis sempre filtram `excluido = 0`. */
@Dao
interface UsuarioDao {
    /** Todos os usuários, ordenados por nome. */
    @Query("SELECT * FROM usuario WHERE excluido = 0 ORDER BY nome COLLATE NOCASE, id")
    fun observeAll(): Flow<List<UsuarioEntity>>

    @Insert
    suspend fun insert(entity: UsuarioEntity): Long

    @Update
    suspend fun update(entity: UsuarioEntity)

    /** Busca pelo e-mail (já em minúsculas) **incluindo excluídos**: o e-mail é único no banco. */
    @Query("SELECT * FROM usuario WHERE email = :email")
    suspend fun findByEmail(email: String): UsuarioEntity?

    /** Busca pelo id **incluindo excluídos**: uso interno dos repositórios. */
    @Query("SELECT * FROM usuario WHERE id = :id")
    suspend fun getByIdIncludingDeleted(id: Long): UsuarioEntity?

    /** Busca pelo `uuid` **incluindo excluídos**: usada na deduplicação da sincronização. */
    @Query("SELECT * FROM usuario WHERE uuid = :uuid")
    suspend fun findByUuid(uuid: String): UsuarioEntity?

    /** Registros com alteração ainda não enviada. **Inclui excluídos**: a exclusão também precisa ser enviada. */
    @Query("SELECT * FROM usuario WHERE sincronizado = 0")
    suspend fun getPending(): List<UsuarioEntity>

    /** Quantos registros têm alteração ainda não enviada (para avisar antes de sair da conta). */
    @Query("SELECT COUNT(*) FROM usuario WHERE sincronizado = 0")
    fun observePendingCount(): Flow<Int>
}
