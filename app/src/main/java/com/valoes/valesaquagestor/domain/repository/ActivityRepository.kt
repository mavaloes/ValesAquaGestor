package com.valoes.valesaquagestor.domain.repository

import com.valoes.valesaquagestor.domain.model.Atividade
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Atividades diárias do calendário. Ficam só no aparelho (não são sincronizadas). */
interface ActivityRepository {

    /**
     * Atividades de [from] a [to], inclusive, em ordem de dia e hora. As que não têm hora vêm
     * depois das que têm, no mesmo dia.
     */
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Atividade>>

    /**
     * Salva a atividade (nova se `id == 0`) e devolve o id.
     *
     * @throws IllegalArgumentException se o título estiver em branco.
     */
    suspend fun save(atividade: Atividade): Long

    /** Marca a atividade como feita ou como não feita. */
    suspend fun setDone(id: Long, done: Boolean)

    /** Apaga a atividade de vez. */
    suspend fun delete(id: Long)
}
