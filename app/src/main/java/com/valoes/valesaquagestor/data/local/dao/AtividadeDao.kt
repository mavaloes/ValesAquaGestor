package com.valoes.valesaquagestor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.valoes.valesaquagestor.data.local.entity.AtividadeEntity
import kotlinx.coroutines.flow.Flow

/** Acesso à tabela `atividade`. */
@Dao
interface AtividadeDao {
    /** Atividades de [fromDay] a [toDay] (dias desde 1970, inclusive); as sem hora vêm depois das com hora. */
    @Query(
        "SELECT * FROM atividade WHERE dataEpochDia BETWEEN :fromDay AND :toDay " +
            "ORDER BY dataEpochDia, horaMinutos IS NULL, horaMinutos, id",
    )
    fun observeBetween(fromDay: Long, toDay: Long): Flow<List<AtividadeEntity>>

    @Insert
    suspend fun insert(entity: AtividadeEntity): Long

    @Update
    suspend fun update(entity: AtividadeEntity)

    @Query("UPDATE atividade SET concluida = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM atividade WHERE id = :id")
    suspend fun deleteById(id: Long)
}
