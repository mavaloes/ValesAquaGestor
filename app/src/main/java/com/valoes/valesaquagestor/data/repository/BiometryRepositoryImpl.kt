package com.valoes.valesaquagestor.data.repository

import com.valoes.valesaquagestor.data.local.dao.BiometriaDao
import com.valoes.valesaquagestor.data.local.dao.LoteDao
import com.valoes.valesaquagestor.data.local.database.TransactionRunner
import com.valoes.valesaquagestor.data.local.entity.BiometriaEntity
import com.valoes.valesaquagestor.domain.model.Biometria
import com.valoes.valesaquagestor.domain.repository.CurrentUserProvider
import com.valoes.valesaquagestor.domain.repository.BiometryRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BiometryRepositoryImpl @Inject constructor(
    private val dao: BiometriaDao,
    private val loteDao: LoteDao,
    private val transaction: TransactionRunner,
    private val clock: Clock,
    private val currentUser: CurrentUserProvider,
) : BiometryRepository {

    override fun observeLatest(lotId: Long, limit: Int): Flow<List<Biometria>> =
        dao.observeLatest(lotId, limit).map { list -> list.map { it.toDomain() } }

    override suspend fun save(biometria: Biometria): Long {
        require(biometria.numeroPeixesAmostra > 0) { "Informe o número de peixes da amostra." }
        require(biometria.pesoAmostraKg > 0.0) { "Informe o peso da amostra." }
        val createdBy = currentUser.currentUserId()
        val now = clock.millis()
        // Biometria e peso médio do lote na mesma transação: ou os dois são gravados, ou nenhum.
        return transaction.run {
            val id = if (biometria.id == 0L) {
                dao.insert(biometria.toEntity(null, UUID.randomUUID().toString(), now, createdBy))
            } else {
                val existing = requireNotNull(dao.getByIdIncludingDeleted(biometria.id)) { "Biometria ${biometria.id} não existe." }
                dao.update(biometria.toEntity(existing, existing.uuid, now))
                existing.id
            }
            // Só a pesagem mais recente define o peso atual: lançar uma biometria antiga não o sobrescreve.
            if (dao.getLatest(biometria.loteId)?.id == id) {
                val lote = requireNotNull(loteDao.getByIdIncludingDeleted(biometria.loteId)) { "Lote ${biometria.loteId} não existe." }
                loteDao.update(lote.copy(pesoMedioAtualG = biometria.pesoMedioG, atualizadoEm = now, sincronizado = false))
            }
            id
        }
    }

    override suspend fun delete(id: Long) {
        val existing = dao.getByIdIncludingDeleted(id) ?: return
        dao.update(existing.copy(excluido = true, atualizadoEm = clock.millis(), sincronizado = false))
    }

    /** Aplica um registro vindo do servidor (Fase 09): deduplica por `uuid`, vence o maior `atualizadoEm`. */
    suspend fun mergeFromRemote(incoming: BiometriaEntity): MergeResult = transaction.run {
        mergeByUuid(
            incoming,
            find = dao::findByUuid,
            insert = { dao.insert(it.copy(id = 0)) },
            update = { entity, localId -> dao.update(entity.copy(id = localId)) },
        )
    }
}
