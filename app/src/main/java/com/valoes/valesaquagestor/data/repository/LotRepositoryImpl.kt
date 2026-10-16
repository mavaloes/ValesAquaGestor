package com.valoes.valesaquagestor.data.repository

import com.valoes.valesaquagestor.data.local.dao.LoteDao
import com.valoes.valesaquagestor.data.local.database.TransactionRunner
import com.valoes.valesaquagestor.data.local.entity.LoteEntity
import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.repository.LotRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LotRepositoryImpl @Inject constructor(
    private val dao: LoteDao,
    private val transaction: TransactionRunner,
    private val clock: Clock,
) : LotRepository {

    override fun observeActiveByTank(tankId: Long): Flow<List<Lote>> =
        dao.observeActiveByTanque(tankId).map { list -> list.map { it.toDomain() } }

    override fun observeAllActive(): Flow<List<Lote>> = dao.observeAllActive().map { list -> list.map { it.toDomain() } }

    override fun observe(id: Long): Flow<Lote?> = dao.observeById(id).map { it?.toDomain() }

    override suspend fun get(id: Long): Lote? = dao.getByIdIncludingDeleted(id)?.takeUnless { it.excluido }?.toDomain()

    override suspend fun save(lote: Lote): Long {
        val now = clock.millis()
        if (lote.id == 0L) return dao.insert(lote.toEntity(null, UUID.randomUUID().toString(), now))
        val existing = requireNotNull(dao.getByIdIncludingDeleted(lote.id)) { "Lote ${lote.id} não existe." }
        dao.update(lote.toEntity(existing, existing.uuid, now))
        return existing.id
    }

    override suspend fun delete(id: Long) {
        val existing = dao.getByIdIncludingDeleted(id) ?: return
        dao.update(existing.copy(excluido = true, atualizadoEm = clock.millis(), sincronizado = false))
    }

    /** Aplica um registro vindo do servidor (Fase 09): deduplica por `uuid`, vence o maior `atualizadoEm`. */
    suspend fun mergeFromRemote(incoming: LoteEntity): MergeResult = transaction.run {
        mergeByUuid(
            incoming,
            find = dao::findByUuid,
            insert = { dao.insert(it.copy(id = 0)) },
            update = { entity, localId -> dao.update(entity.copy(id = localId)) },
        )
    }
}
