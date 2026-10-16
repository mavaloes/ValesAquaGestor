package com.valoes.valesaquagestor.data.repository

import com.valoes.valesaquagestor.data.local.dao.QualidadeAguaDao
import com.valoes.valesaquagestor.data.local.database.TransactionRunner
import com.valoes.valesaquagestor.data.local.entity.QualidadeAguaEntity
import com.valoes.valesaquagestor.domain.model.LatestWaterReadings
import com.valoes.valesaquagestor.domain.model.QualidadeAgua
import com.valoes.valesaquagestor.domain.model.Reading
import com.valoes.valesaquagestor.domain.repository.CurrentUserProvider
import com.valoes.valesaquagestor.domain.repository.WaterQualityRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class WaterQualityRepositoryImpl @Inject constructor(
    private val dao: QualidadeAguaDao,
    private val transaction: TransactionRunner,
    private val clock: Clock,
    private val currentUser: CurrentUserProvider,
) : WaterQualityRepository {

    override fun observeLatestReadings(tankId: Long): Flow<LatestWaterReadings> = combine(
        listOf(
            dao.observeLatestTransparencia(tankId),
            dao.observeLatestTemperatura(tankId),
            dao.observeLatestOxigenio(tankId),
            dao.observeLatestPh(tankId),
            dao.observeLatestAmoniaTotal(tankId),
            dao.observeLatestNitrito(tankId),
            dao.observeLatestAlcalinidade(tankId),
            dao.observeLatestDureza(tankId),
        ),
    ) { latest ->
        LatestWaterReadings(
            transparenciaCm = latest[0].reading { it.transparenciaCm },
            temperaturaC = latest[1].reading { it.temperaturaC },
            oxigenioMgL = latest[2].reading { it.oxigenioMgL },
            ph = latest[3].reading { it.ph },
            amoniaTotalMgL = latest[4].reading { it.amoniaTotalMgL },
            nitritoMgL = latest[5].reading { it.nitritoMgL },
            alcalinidadeMgL = latest[6].reading { it.alcalinidadeMgL },
            durezaMgL = latest[7].reading { it.durezaMgL },
        )
    }

    override fun observeBetween(tankId: Long, from: Instant, to: Instant): Flow<List<QualidadeAgua>> =
        dao.observeBetween(tankId, from.toEpochMilli(), to.toEpochMilli()).map { list -> list.map { it.toDomain() } }

    override fun observeRecent(tankId: Long, limit: Int): Flow<List<QualidadeAgua>> =
        dao.observeRecent(tankId, limit).map { list -> list.map { it.toDomain() } }

    override suspend fun save(medicao: QualidadeAgua): Long {
        require(medicao.temAlgumaMedida) { "Informe pelo menos uma medida." }
        val createdBy = currentUser.currentUserId()
        val now = clock.millis()
        if (medicao.id == 0L) return dao.insert(medicao.toEntity(null, UUID.randomUUID().toString(), now, createdBy))
        val existing = requireNotNull(dao.getByIdIncludingDeleted(medicao.id)) { "Medição ${medicao.id} não existe." }
        dao.update(medicao.toEntity(existing, existing.uuid, now))
        return existing.id
    }

    override suspend fun delete(id: Long) {
        val existing = dao.getByIdIncludingDeleted(id) ?: return
        dao.update(existing.copy(excluido = true, atualizadoEm = clock.millis(), sincronizado = false))
    }

    /** Aplica um registro vindo do servidor (Fase 09): deduplica por `uuid`, vence o maior `atualizadoEm`. */
    suspend fun mergeFromRemote(incoming: QualidadeAguaEntity): MergeResult = transaction.run {
        mergeByUuid(
            incoming,
            find = dao::findByUuid,
            insert = { dao.insert(it.copy(id = 0)) },
            update = { entity, localId -> dao.update(entity.copy(id = localId)) },
        )
    }

    private fun QualidadeAguaEntity?.reading(value: (QualidadeAguaEntity) -> Double?): Reading? {
        if (this == null) return null
        return value(this)?.let { Reading(it, Instant.ofEpochMilli(dataHora)) }
    }
}
