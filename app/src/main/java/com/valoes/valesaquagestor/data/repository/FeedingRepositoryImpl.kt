package com.valoes.valesaquagestor.data.repository

import com.valoes.valesaquagestor.data.local.dao.ArracoamentoDao
import com.valoes.valesaquagestor.data.local.database.TransactionRunner
import com.valoes.valesaquagestor.data.local.entity.ArracoamentoEntity
import com.valoes.valesaquagestor.domain.model.Arracoamento
import com.valoes.valesaquagestor.domain.repository.FeedingRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FeedingRepositoryImpl @Inject constructor(
    private val dao: ArracoamentoDao,
    private val transaction: TransactionRunner,
    private val clock: Clock,
) : FeedingRepository {

    override fun observeByLot(lotId: Long, from: Instant, to: Instant): Flow<List<Arracoamento>> =
        dao.observeByLote(lotId, from.toEpochMilli(), to.toEpochMilli()).map { list -> list.map { it.toDomain() } }

    override fun observeRecent(lotId: Long, limit: Int): Flow<List<Arracoamento>> =
        dao.observeRecent(lotId, limit).map { list -> list.map { it.toDomain() } }

    override suspend fun getLast(lotId: Long): Arracoamento? = dao.getLast(lotId)?.toDomain()

    override suspend fun save(arracoamento: Arracoamento): Long {
        val now = clock.millis()
        if (arracoamento.id == 0L) return dao.insert(arracoamento.toEntity(null, UUID.randomUUID().toString(), now))
        val existing = requireNotNull(dao.getByIdIncludingDeleted(arracoamento.id)) { "Trato ${arracoamento.id} não existe." }
        dao.update(arracoamento.toEntity(existing, existing.uuid, now))
        return existing.id
    }

    override suspend fun delete(id: Long) {
        val existing = dao.getByIdIncludingDeleted(id) ?: return
        dao.update(existing.copy(excluido = true, atualizadoEm = clock.millis(), sincronizado = false))
    }

    /** Aplica um registro vindo do servidor (Fase 09): deduplica por `uuid`, vence o maior `atualizadoEm`. */
    suspend fun mergeFromRemote(incoming: ArracoamentoEntity): MergeResult = transaction.run {
        mergeByUuid(
            incoming,
            find = dao::findByUuid,
            insert = { dao.insert(it.copy(id = 0)) },
            update = { entity, localId -> dao.update(entity.copy(id = localId)) },
        )
    }
}
