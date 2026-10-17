package com.valoes.valesaquagestor.data.repository

import com.valoes.valesaquagestor.data.local.dao.AtividadeDao
import com.valoes.valesaquagestor.data.local.entity.AtividadeEntity
import com.valoes.valesaquagestor.domain.model.Atividade
import com.valoes.valesaquagestor.domain.repository.ActivityRepository
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ActivityRepositoryImpl @Inject constructor(
    private val dao: AtividadeDao,
) : ActivityRepository {

    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Atividade>> =
        dao.observeBetween(from.toEpochDay(), to.toEpochDay()).map { list -> list.map { it.toDomain() } }

    override suspend fun save(atividade: Atividade): Long {
        require(atividade.titulo.isNotBlank()) { "Informe o que fazer." }
        val entity = atividade.toEntity()
        return if (atividade.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            atividade.id
        }
    }

    override suspend fun setDone(id: Long, done: Boolean) = dao.setDone(id, done)

    override suspend fun delete(id: Long) = dao.deleteById(id)
}

private fun AtividadeEntity.toDomain() = Atividade(
    id = id,
    titulo = titulo,
    data = LocalDate.ofEpochDay(dataEpochDia),
    hora = horaMinutos?.let { LocalTime.of(it / MINUTES_PER_HOUR, it % MINUTES_PER_HOUR) },
    observacao = observacao,
    concluida = concluida,
)

private fun Atividade.toEntity() = AtividadeEntity(
    id = id,
    titulo = titulo.trim(),
    dataEpochDia = data.toEpochDay(),
    horaMinutos = hora?.let { it.hour * MINUTES_PER_HOUR + it.minute },
    observacao = observacao?.trim()?.ifBlank { null },
    concluida = concluida,
)

private const val MINUTES_PER_HOUR = 60
