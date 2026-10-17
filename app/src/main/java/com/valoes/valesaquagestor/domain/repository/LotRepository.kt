package com.valoes.valesaquagestor.domain.repository

import com.valoes.valesaquagestor.domain.model.Lote
import kotlinx.coroutines.flow.Flow

/** Lotes de peixes. As consultas nunca trazem registros excluídos. */
interface LotRepository {

    /** Lotes com status ATIVO de um tanque, do mais recente ao mais antigo. */
    fun observeActiveByTank(tankId: Long): Flow<List<Lote>>

    /** Lotes ativos de todos os tanques. */
    fun observeAllActive(): Flow<List<Lote>>

    /** Um lote pelo id; emite `null` se não existe ou foi excluído. */
    fun observe(id: Long): Flow<Lote?>

    /** Um lote pelo id, ou `null`. */
    suspend fun get(id: Long): Lote?

    /** Salva o lote (novo se `id == 0`) e devolve o id. Preenche `uuid` e `atualizadoEm`. */
    suspend fun save(lote: Lote): Long

    /** Exclusão lógica. */
    suspend fun delete(id: Long)
}
