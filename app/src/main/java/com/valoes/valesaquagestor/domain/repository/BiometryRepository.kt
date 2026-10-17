package com.valoes.valesaquagestor.domain.repository

import com.valoes.valesaquagestor.domain.model.Biometria
import kotlinx.coroutines.flow.Flow

/** Biometrias dos lotes. As consultas nunca trazem registros excluídos. */
interface BiometryRepository {

    /** As [limit] biometrias mais recentes do lote, da mais nova à mais antiga. */
    fun observeLatest(lotId: Long, limit: Int): Flow<List<Biometria>>

    /**
     * Salva a biometria (nova se `id == 0`) e devolve o id. Na **mesma transação**, se esta for a
     * biometria mais recente do lote, atualiza `Lote.pesoMedioAtualG` com o peso médio dela.
     * Uma biometria lançada com data antiga não sobrescreve o peso de uma pesagem mais nova.
     *
     * @throws IllegalArgumentException se o nº de peixes ou o peso da amostra não for positivo.
     */
    suspend fun save(biometria: Biometria): Long

    /** Exclusão lógica. Não altera o peso médio do lote. */
    suspend fun delete(id: Long)
}
