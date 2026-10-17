package com.valoes.valesaquagestor.data.repository

import com.valoes.valesaquagestor.data.local.dao.LoteDao
import com.valoes.valesaquagestor.data.local.dao.MovimentacaoLoteDao
import com.valoes.valesaquagestor.data.local.database.TransactionRunner
import com.valoes.valesaquagestor.data.local.entity.MovimentacaoLoteEntity
import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import com.valoes.valesaquagestor.domain.model.StatusLote
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import com.valoes.valesaquagestor.domain.repository.CurrentUserProvider
import com.valoes.valesaquagestor.domain.repository.LotMovementRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LotMovementRepositoryImpl @Inject constructor(
    private val dao: MovimentacaoLoteDao,
    private val loteDao: LoteDao,
    private val transaction: TransactionRunner,
    private val clock: Clock,
    private val currentUser: CurrentUserProvider,
) : LotMovementRepository {

    override fun observeByLot(lotId: Long): Flow<List<MovimentacaoLote>> =
        dao.observeByLote(lotId).map { list -> list.map { it.toDomain() } }

    override suspend fun save(movimentacao: MovimentacaoLote): Long {
        require(movimentacao.quantidade > 0) { "Informe a quantidade de peixes." }
        val createdBy = currentUser.currentUserId()
        val now = clock.millis()
        // Movimentação e encerramento do lote na mesma transação.
        return transaction.run {
            val id = if (movimentacao.id == 0L) {
                dao.insert(movimentacao.toEntity(null, UUID.randomUUID().toString(), now, createdBy))
            } else {
                val existing = requireNotNull(dao.getByIdIncludingDeleted(movimentacao.id)) { "Movimentação ${movimentacao.id} não existe." }
                dao.update(movimentacao.toEntity(existing, existing.uuid, now))
                existing.id
            }
            if (movimentacao.tipo == TipoMovimentacao.DESPESCA_FINAL) {
                val lote = requireNotNull(loteDao.getByIdIncludingDeleted(movimentacao.loteId)) { "Lote ${movimentacao.loteId} não existe." }
                loteDao.update(
                    lote.copy(
                        status = StatusLote.ENCERRADO,
                        dataEncerramento = movimentacao.dataHora.toEpochMilli(),
                        atualizadoEm = now,
                        sincronizado = false,
                    ),
                )
            }
            id
        }
    }

    override suspend fun delete(id: Long) {
        val existing = dao.getByIdIncludingDeleted(id) ?: return
        dao.update(existing.copy(excluido = true, atualizadoEm = clock.millis(), sincronizado = false))
    }

    /** Aplica um registro vindo do servidor (Fase 09): deduplica por `uuid`, vence o maior `atualizadoEm`. */
    suspend fun mergeFromRemote(incoming: MovimentacaoLoteEntity): MergeResult = transaction.run {
        mergeByUuid(
            incoming,
            find = dao::findByUuid,
            insert = { dao.insert(it.copy(id = 0)) },
            update = { entity, localId -> dao.update(entity.copy(id = localId)) },
        )
    }
}
