package com.valoes.valesaquagestor.domain.repository

import com.valoes.valesaquagestor.domain.model.LatestWaterReadings
import com.valoes.valesaquagestor.domain.model.QualidadeAgua
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Medições da qualidade da água. As consultas nunca trazem registros excluídos. */
interface WaterQualityRepository {

    /** Última leitura de cada parâmetro do tanque (cada um pode vir de uma medição diferente). */
    fun observeLatestReadings(tankId: Long): Flow<LatestWaterReadings>

    /** Medições do tanque entre [from] (inclusivo) e [to] (exclusivo), da mais antiga à mais nova. */
    fun observeBetween(tankId: Long, from: Instant, to: Instant): Flow<List<QualidadeAgua>>

    /** As [limit] medições mais recentes do tanque, da mais nova à mais antiga. */
    fun observeRecent(tankId: Long, limit: Int): Flow<List<QualidadeAgua>>

    /**
     * Salva a medição (nova se `id == 0`) e devolve o id.
     *
     * @throws IllegalArgumentException se nenhuma medida foi informada.
     */
    suspend fun save(medicao: QualidadeAgua): Long

    /** Exclusão lógica. */
    suspend fun delete(id: Long)
}
