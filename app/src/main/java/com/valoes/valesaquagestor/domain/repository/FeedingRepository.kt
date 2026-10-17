package com.valoes.valesaquagestor.domain.repository

import com.valoes.valesaquagestor.domain.model.Arracoamento
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Tratos (arraçoamento) dos lotes. As consultas nunca trazem registros excluídos. */
interface FeedingRepository {

    /** Tratos do lote entre [from] (inclusivo) e [to] (exclusivo), do mais antigo ao mais novo. */
    fun observeByLot(lotId: Long, from: Instant, to: Instant): Flow<List<Arracoamento>>

    /** Os [limit] tratos mais recentes do lote, do mais novo ao mais antigo. */
    fun observeRecent(lotId: Long, limit: Int): Flow<List<Arracoamento>>

    /** O trato mais recente do lote (para preencher marca, proteína e pellet), ou `null`. */
    suspend fun getLast(lotId: Long): Arracoamento?

    /** Salva o trato (novo se `id == 0`) e devolve o id. */
    suspend fun save(arracoamento: Arracoamento): Long

    /** Exclusão lógica. */
    suspend fun delete(id: Long)
}
