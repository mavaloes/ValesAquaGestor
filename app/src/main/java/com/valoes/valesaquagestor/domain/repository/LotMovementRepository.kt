package com.valoes.valesaquagestor.domain.repository

import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import kotlinx.coroutines.flow.Flow

/**
 * Movimentações do lote (mortalidade, despesca, transferência). É a base de `LotCalculator`
 * (quantidade atual e mortalidade acumulada). As consultas nunca trazem registros excluídos.
 */
interface LotMovementRepository {

    /** Movimentações do lote, da mais antiga à mais nova. */
    fun observeByLot(lotId: Long): Flow<List<MovimentacaoLote>>

    /**
     * Salva a movimentação (nova se `id == 0`) e devolve o id. Na **mesma transação**, uma
     * `DESPESCA_FINAL` marca o lote como ENCERRADO, com a data da movimentação.
     *
     * @throws IllegalArgumentException se a quantidade não for positiva.
     */
    suspend fun save(movimentacao: MovimentacaoLote): Long

    /** Exclusão lógica. Não reabre um lote encerrado. */
    suspend fun delete(id: Long)
}
