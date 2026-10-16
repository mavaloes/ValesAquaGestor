package com.valoes.valesaquagestor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.valoes.valesaquagestor.data.local.entity.QualidadeAguaEntity
import kotlinx.coroutines.flow.Flow

/** Acesso à tabela `qualidade_agua`. Consultas observáveis sempre filtram `excluido = 0`. */
@Dao
interface QualidadeAguaDao {
    /** Medições do tanque com `from <= dataHora < to`, da mais antiga à mais nova. */
    @Query("SELECT * FROM qualidade_agua WHERE tanqueId = :tanqueId AND excluido = 0 AND dataHora >= :from AND dataHora < :to ORDER BY dataHora, id")
    fun observeBetween(tanqueId: Long, from: Long, to: Long): Flow<List<QualidadeAguaEntity>>

    /** Medição mais recente do tanque em que `transparenciaCm` foi informado. */
    @Query("SELECT * FROM qualidade_agua WHERE tanqueId = :tanqueId AND excluido = 0 AND transparenciaCm IS NOT NULL ORDER BY dataHora DESC, id DESC LIMIT 1")
    fun observeLatestTransparencia(tanqueId: Long): Flow<QualidadeAguaEntity?>

    /** Medição mais recente do tanque em que `temperaturaC` foi informado. */
    @Query("SELECT * FROM qualidade_agua WHERE tanqueId = :tanqueId AND excluido = 0 AND temperaturaC IS NOT NULL ORDER BY dataHora DESC, id DESC LIMIT 1")
    fun observeLatestTemperatura(tanqueId: Long): Flow<QualidadeAguaEntity?>

    /** Medição mais recente do tanque em que `oxigenioMgL` foi informado. */
    @Query("SELECT * FROM qualidade_agua WHERE tanqueId = :tanqueId AND excluido = 0 AND oxigenioMgL IS NOT NULL ORDER BY dataHora DESC, id DESC LIMIT 1")
    fun observeLatestOxigenio(tanqueId: Long): Flow<QualidadeAguaEntity?>

    /** Medição mais recente do tanque em que `ph` foi informado. */
    @Query("SELECT * FROM qualidade_agua WHERE tanqueId = :tanqueId AND excluido = 0 AND ph IS NOT NULL ORDER BY dataHora DESC, id DESC LIMIT 1")
    fun observeLatestPh(tanqueId: Long): Flow<QualidadeAguaEntity?>

    /** Medição mais recente do tanque em que `amoniaTotalMgL` foi informado. */
    @Query("SELECT * FROM qualidade_agua WHERE tanqueId = :tanqueId AND excluido = 0 AND amoniaTotalMgL IS NOT NULL ORDER BY dataHora DESC, id DESC LIMIT 1")
    fun observeLatestAmoniaTotal(tanqueId: Long): Flow<QualidadeAguaEntity?>

    /** Medição mais recente do tanque em que `nitritoMgL` foi informado. */
    @Query("SELECT * FROM qualidade_agua WHERE tanqueId = :tanqueId AND excluido = 0 AND nitritoMgL IS NOT NULL ORDER BY dataHora DESC, id DESC LIMIT 1")
    fun observeLatestNitrito(tanqueId: Long): Flow<QualidadeAguaEntity?>

    /** Medição mais recente do tanque em que `alcalinidadeMgL` foi informado. */
    @Query("SELECT * FROM qualidade_agua WHERE tanqueId = :tanqueId AND excluido = 0 AND alcalinidadeMgL IS NOT NULL ORDER BY dataHora DESC, id DESC LIMIT 1")
    fun observeLatestAlcalinidade(tanqueId: Long): Flow<QualidadeAguaEntity?>

    /** Medição mais recente do tanque em que `durezaMgL` foi informado. */
    @Query("SELECT * FROM qualidade_agua WHERE tanqueId = :tanqueId AND excluido = 0 AND durezaMgL IS NOT NULL ORDER BY dataHora DESC, id DESC LIMIT 1")
    fun observeLatestDureza(tanqueId: Long): Flow<QualidadeAguaEntity?>

    @Insert
    suspend fun insert(entity: QualidadeAguaEntity): Long

    @Update
    suspend fun update(entity: QualidadeAguaEntity)

    /** Busca pelo id **incluindo excluídos**: uso interno dos repositórios (editar e excluir). */
    @Query("SELECT * FROM qualidade_agua WHERE id = :id")
    suspend fun getByIdIncludingDeleted(id: Long): QualidadeAguaEntity?

    /** Busca pelo `uuid` **incluindo excluídos**: usada na deduplicação da sincronização. */
    @Query("SELECT * FROM qualidade_agua WHERE uuid = :uuid")
    suspend fun findByUuid(uuid: String): QualidadeAguaEntity?

    /** Registros com alteração ainda não enviada. **Inclui excluídos**: a exclusão também precisa ser enviada. */
    @Query("SELECT * FROM qualidade_agua WHERE sincronizado = 0")
    suspend fun getPending(): List<QualidadeAguaEntity>
}
