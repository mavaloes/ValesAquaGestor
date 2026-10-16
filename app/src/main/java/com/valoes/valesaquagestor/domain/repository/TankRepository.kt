package com.valoes.valesaquagestor.domain.repository

import com.valoes.valesaquagestor.domain.model.Tanque
import kotlinx.coroutines.flow.Flow

/**
 * Tanques da fazenda. As listas e consultas nunca trazem registros excluídos (exclusão lógica).
 */
interface TankRepository {

    /** Tanques com status ATIVO, ordenados por nome. */
    fun observeActive(): Flow<List<Tanque>>

    /** Um tanque pelo id; emite `null` se não existe ou foi excluído. */
    fun observe(id: Long): Flow<Tanque?>

    /** Um tanque pelo id, ou `null`. */
    suspend fun get(id: Long): Tanque?

    /**
     * Salva o tanque (novo se `id == 0`) e devolve o id. Preenche `uuid` e `atualizadoEm` e marca
     * como não sincronizado.
     *
     * @throws IllegalArgumentException se ESCAVADO/ACUDE sem `areaM2`, ou outro tipo sem `volumeM3`.
     */
    suspend fun save(tanque: Tanque): Long

    /** Exclusão lógica: o registro some das listas, mas fica no banco até o servidor confirmar. */
    suspend fun delete(id: Long)
}
