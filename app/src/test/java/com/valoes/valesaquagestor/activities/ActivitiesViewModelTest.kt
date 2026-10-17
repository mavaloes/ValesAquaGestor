package com.valoes.valesaquagestor.activities

import com.valoes.valesaquagestor.auth.MainDispatcherRule
import com.valoes.valesaquagestor.domain.model.Atividade
import com.valoes.valesaquagestor.farm.FIXED_CLOCK
import com.valoes.valesaquagestor.presentation.activities.ActivitiesMessage
import com.valoes.valesaquagestor.presentation.activities.ActivitiesViewModel
import com.valoes.valesaquagestor.presentation.activities.monthWeeks
import com.valoes.valesaquagestor.presentation.util.FormError
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Calendário de atividades diárias: dia escolhido, cadastro, conclusão e exclusão. */
class ActivitiesViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    // O ViewModel usa o fuso do aparelho; o teste calcula o "hoje" do mesmo jeito.
    private val today: LocalDate = LocalDate.now(FIXED_CLOCK.withZone(ZoneId.systemDefault()))

    private fun viewModel(repository: FakeActivityRepository) = ActivitiesViewModel(repository, FIXED_CLOCK)

    @Test
    fun `abre no mes e no dia de hoje`() = runBlocking {
        val state = viewModel(FakeActivityRepository()).state.first()

        assertEquals(today, state.today)
        assertEquals(today, state.selected)
        assertEquals(YearMonth.from(today), state.month)
    }

    @Test
    fun `a lista mostra so as atividades do dia escolhido, com hora primeiro`() = runBlocking {
        val repository = FakeActivityRepository(
            listOf(
                Atividade(titulo = "Sem hora", data = today),
                Atividade(titulo = "De manhã", data = today, hora = LocalTime.of(7, 0)),
                Atividade(titulo = "Outro dia", data = today.plusDays(1)),
            ),
        )
        val viewModel = viewModel(repository)

        val state = viewModel.state.first { it.monthActivities.isNotEmpty() }

        assertEquals(listOf("De manhã", "Sem hora"), state.selectedActivities.map { it.titulo })
    }

    @Test
    fun `salvar sem titulo mostra o erro e nao grava`() = runBlocking {
        val repository = FakeActivityRepository()
        val viewModel = viewModel(repository)

        viewModel.onNewActivity()
        viewModel.onTitleChange("   ")
        viewModel.onSave()

        assertEquals(FormError.REQUIRED, viewModel.state.first().form?.tituloError)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `salvar cria a atividade no dia escolhido e avisa`() = runBlocking {
        val repository = FakeActivityRepository()
        val viewModel = viewModel(repository)
        val day = today.plusDays(2)

        viewModel.onSelectDay(day)
        viewModel.onNewActivity()
        viewModel.onTitleChange("Limpar os bebedouros")
        viewModel.onTimeChange(LocalTime.of(6, 30))
        viewModel.onNotesChange("Escova nova")
        viewModel.onSave()

        val saved = repository.saved.single()
        assertEquals("Limpar os bebedouros", saved.titulo)
        assertEquals(day, saved.data)
        assertEquals(LocalTime.of(6, 30), saved.hora)
        assertEquals("Escova nova", saved.observacao)
        assertFalse(saved.concluida)
        val state = viewModel.state.first { it.message != null }
        assertEquals(ActivitiesMessage.SAVED, state.message)
        assertNull(state.form)
    }

    @Test
    fun `editar muda a atividade sem criar outra`() = runBlocking {
        val repository = FakeActivityRepository(listOf(Atividade(titulo = "Pedir ração", data = today)))
        val viewModel = viewModel(repository)
        val original = viewModel.state.first { it.monthActivities.isNotEmpty() }.monthActivities.single()

        viewModel.onEditActivity(original)
        viewModel.onTitleChange("Pedir ração 25 kg")
        viewModel.onSave()

        assertEquals("Pedir ração 25 kg", repository.saved.single().titulo)
        assertEquals(original.id, repository.saved.single().id)
    }

    @Test
    fun `marcar como feita alterna a conclusao`() = runBlocking {
        val repository = FakeActivityRepository(listOf(Atividade(titulo = "Conferir o aerador", data = today)))
        val viewModel = viewModel(repository)
        val activity = viewModel.state.first { it.monthActivities.isNotEmpty() }.monthActivities.single()

        viewModel.onToggleDone(activity)
        assertTrue(repository.saved.single().concluida)

        viewModel.onToggleDone(repository.saved.single())
        assertFalse(repository.saved.single().concluida)
    }

    @Test
    fun `apagar pede confirmacao e so apaga depois dela`() = runBlocking {
        val repository = FakeActivityRepository(listOf(Atividade(titulo = "Pedir ração", data = today)))
        val viewModel = viewModel(repository)
        val activity = viewModel.state.first { it.monthActivities.isNotEmpty() }.monthActivities.single()

        viewModel.onDeleteClick(activity)
        assertNotNull(viewModel.state.first().deleting)
        assertEquals(1, repository.saved.size)

        viewModel.onDeleteDismiss()
        assertNull(viewModel.state.first().deleting)
        assertEquals(1, repository.saved.size)

        viewModel.onDeleteClick(activity)
        viewModel.onDeleteConfirm()
        assertTrue(repository.saved.isEmpty())
        assertEquals(ActivitiesMessage.DELETED, viewModel.state.first { it.message != null }.message)
    }

    @Test
    fun `trocar de mes carrega as atividades do outro mes`() = runBlocking {
        val nextMonthDay = YearMonth.from(today).plusMonths(1).atDay(5)
        val repository = FakeActivityRepository(listOf(Atividade(titulo = "Mês que vem", data = nextMonthDay)))
        val viewModel = viewModel(repository)
        assertTrue(viewModel.state.first().monthActivities.isEmpty())

        viewModel.onNextMonth()

        val state = viewModel.state.first { it.monthActivities.isNotEmpty() }
        assertEquals(YearMonth.from(nextMonthDay), state.month)
        assertEquals(setOf(nextMonthDay), state.daysWithActivities)
    }

    @Test
    fun `os dias com atividade por fazer ficam separados dos concluidos`() = runBlocking {
        // Dois dias do mês de hoje, para os dois caírem no mês mostrado.
        val pendingDay = YearMonth.from(today).atDay(1)
        val doneDay = YearMonth.from(today).atDay(2)
        val repository = FakeActivityRepository(
            listOf(
                Atividade(titulo = "Por fazer", data = pendingDay),
                Atividade(titulo = "Feita", data = doneDay, concluida = true),
            ),
        )
        val state = viewModel(repository).state.first { it.monthActivities.size == 2 }

        assertEquals(setOf(pendingDay), state.daysWithPending)
        assertEquals(setOf(pendingDay, doneDay), state.daysWithActivities)
    }

    @Test
    fun `tocar num dia de outro mes leva o calendario para esse mes`() = runBlocking {
        val viewModel = viewModel(FakeActivityRepository())
        val day = LocalDate.of(2027, 1, 20)

        viewModel.onSelectDay(day)

        val state = viewModel.state.first()
        assertEquals(YearMonth.of(2027, 1), state.month)
        assertEquals(day, state.selected)

        viewModel.onGoToToday()
        assertEquals(today, viewModel.state.first().selected)
    }

    @Test
    fun `outubro de 2026 comeca numa quinta e tem cinco semanas`() {
        val weeks = monthWeeks(YearMonth.of(2026, 10))

        assertEquals(5, weeks.size)
        assertTrue(weeks.all { it.size == 7 })
        // Domingo primeiro: quinta-feira é a 5ª coluna, então as quatro primeiras ficam vazias.
        assertEquals(listOf(null, null, null, null, LocalDate.of(2026, 10, 1)), weeks.first().take(5))
        assertEquals(LocalDate.of(2026, 10, 31), weeks.last()[6])
        assertEquals(31, weeks.flatten().filterNotNull().size)
    }

    @Test
    fun `fevereiro de 2026 comeca no domingo e cabe em quatro semanas`() {
        val weeks = monthWeeks(YearMonth.of(2026, 2))

        assertEquals(4, weeks.size)
        assertEquals(LocalDate.of(2026, 2, 1), weeks.first().first())
        assertEquals(LocalDate.of(2026, 2, 28), weeks.last().last())
    }
}
