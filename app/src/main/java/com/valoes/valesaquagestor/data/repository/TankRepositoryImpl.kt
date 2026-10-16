package com.valoes.valesaquagestor.data.repository

import com.valoes.valesaquagestor.data.local.dao.TanqueDao
import com.valoes.valesaquagestor.data.local.database.TransactionRunner
import com.valoes.valesaquagestor.data.local.entity.TanqueEntity
import com.valoes.valesaquagestor.domain.model.Tanque
import com.valoes.valesaquagestor.domain.model.TipoTanque
import com.valoes.valesaquagestor.domain.repository.TankRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TankRepositoryImpl @Inject constructor(
    private val dao: TanqueDao,
    private val transaction: TransactionRunner,
    private val clock: Clock,
) : TankRepository {

    override fun observeActive(): Flow<List<Tanque>> = dao.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observe(id: Long): Flow<Tanque?> = dao.observeById(id).map { it?.toDomain() }

    override suspend fun get(id: Long): Tanque? = dao.getByIdIncludingDeleted(id)?.takeUnless { it.excluido }?.toDomain()

    override suspend fun getByUuid(uuid: String): Tanque? = dao.findByUuid(uuid)?.takeUnless { it.excluido }?.toDomain()

    override suspend fun save(tanque: Tanque): Long {
        // Viveiro e açude têm capacidade por área; os demais tipos, por volume (F03 p.15; F12 p.14).
        when (tanque.tipo) {
            TipoTanque.ESCAVADO, TipoTanque.ACUDE ->
                require(tanque.areaM2 != null) { "Informe a área do tanque." }

            else -> require(tanque.volumeM3 != null) { "Informe o volume do tanque." }
        }
        val now = clock.millis()
        if (tanque.id == 0L) return dao.insert(tanque.toEntity(null, UUID.randomUUID().toString(), now))
        val existing = requireNotNull(dao.getByIdIncludingDeleted(tanque.id)) { "Tanque ${tanque.id} não existe." }
        dao.update(tanque.toEntity(existing, existing.uuid, now))
        return existing.id
    }

    override suspend fun delete(id: Long) {
        val existing = dao.getByIdIncludingDeleted(id) ?: return
        dao.update(existing.copy(excluido = true, atualizadoEm = clock.millis(), sincronizado = false))
    }

    /** Aplica um registro vindo do servidor (Fase 09): deduplica por `uuid`, vence o maior `atualizadoEm`. */
    suspend fun mergeFromRemote(incoming: TanqueEntity): MergeResult = transaction.run {
        mergeByUuid(
            incoming,
            find = dao::findByUuid,
            insert = { dao.insert(it.copy(id = 0)) },
            update = { entity, localId -> dao.update(entity.copy(id = localId)) },
        )
    }
}
