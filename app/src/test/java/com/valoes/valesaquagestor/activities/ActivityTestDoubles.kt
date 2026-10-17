package com.valoes.valesaquagestor.activities

import com.valoes.valesaquagestor.domain.model.Atividade
import com.valoes.valesaquagestor.domain.repository.ActivityRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Atividades guardadas só na memória. Imita o repositório real: gera o `id` e ordena por dia e hora. */
class FakeActivityRepository(initial: List<Atividade> = emptyList()) : ActivityRepository {
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1
    private val state = MutableStateFlow(initial.map { if (it.id == 0L) it.copy(id = nextId++) else it })

    val saved: List<Atividade> get() = state.value

    private val order = compareBy<Atividade>({ it.data }, { it.hora == null }, { it.hora ?: LocalTime.MIN }, { it.id })

    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Atividade>> =
        state.map { list -> list.filter { it.data in from..to }.sortedWith(order) }

    override suspend fun save(atividade: Atividade): Long {
        require(atividade.titulo.isNotBlank()) { "Informe o que fazer." }
        val stored = if (atividade.id == 0L) atividade.copy(id = nextId++) else atividade
        state.value = state.value.filterNot { it.id == stored.id } + stored
        return stored.id
    }

    override suspend fun setDone(id: Long, done: Boolean) {
        state.value = state.value.map { if (it.id == id) it.copy(concluida = done) else it }
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }
}
