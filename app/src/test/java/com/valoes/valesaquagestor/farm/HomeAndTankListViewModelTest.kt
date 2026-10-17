package com.valoes.valesaquagestor.farm

import com.valoes.valesaquagestor.activities.FakeActivityRepository
import com.valoes.valesaquagestor.auth.MainDispatcherRule
import com.valoes.valesaquagestor.domain.calc.MeasuredParameter
import com.valoes.valesaquagestor.domain.calc.PendingTask
import com.valoes.valesaquagestor.domain.model.Atividade
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.AlertLevel
import com.valoes.valesaquagestor.presentation.home.HomeViewModel
import com.valoes.valesaquagestor.presentation.tank.TankListViewModel
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Casos de Etapa 7 da Fase 05: ordem dos alertas e pendências do dia. */
class HomeAndTankListViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    // ---- Home ----

    @Test
    fun `fazenda sem tanques avisa para cadastrar o primeiro`() = runBlocking {
        val viewModel = HomeViewModel(FarmFixture().overview, FIXED_CLOCK, FakeActivityRepository())

        val state = viewModel.state.first { !it.isLoading }

        assertFalse(state.hasTanks)
        assertTrue(state.routine.alerts.isEmpty())
        assertTrue(state.routine.tasks.isEmpty())
    }

    @Test
    fun `o quadro de avisos traz so as atividades de hoje ainda nao feitas`() = runBlocking {
        val today = LocalDate.now(FIXED_CLOCK.withZone(ZoneId.systemDefault()))
        val activities = FakeActivityRepository(
            listOf(
                Atividade(titulo = "Limpar os bebedouros", data = today),
                Atividade(titulo = "Conferir o aerador", data = today, concluida = true),
                Atividade(titulo = "Pedir ração", data = today.plusDays(1)),
            ),
        )

        val state = HomeViewModel(FarmFixture().overview, FIXED_CLOCK, activities).state.first { !it.isLoading }

        assertEquals(listOf("Limpar os bebedouros"), state.todayActivities.map { it.titulo })
    }

    @Test
    fun `os alertas saem com EMERGENCIA primeiro`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1, "Alfa"), tank(2, "Beta"), tank(3, "Gama")),
            lots = listOf(lot(1, 1), lot(2, 2), lot(3, 3)),
            waterRecords = listOf(
                water(1, tankId = 1, oxigenio = 3.5), // ATENCAO
                water(2, tankId = 2, oxigenio = 1.5), // EMERGENCIA
                water(3, tankId = 3, oxigenio = 2.5), // CRITICO
            ),
        )

        val alerts = HomeViewModel(farm.overview, FIXED_CLOCK, FakeActivityRepository()).state.first { !it.isLoading }.routine.alerts

        assertEquals(
            listOf("Beta" to AlertLevel.EMERGENCIA, "Gama" to AlertLevel.CRITICO, "Alfa" to AlertLevel.ATENCAO),
            alerts.map { it.tankName to it.status.level },
        )
    }

    @Test
    fun `mortalidade acima de 10 por cento aparece como alerta do lote`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1)),
            lots = listOf(lot(1)),
            movements = listOf(movement(1, quantity = 150, tipo = TipoMovimentacao.MORTALIDADE)),
        )

        val alert = HomeViewModel(farm.overview, FIXED_CLOCK, FakeActivityRepository()).state.first { !it.isLoading }.routine.alerts.single()

        assertEquals(AlertKind.MORTALITY, alert.kind)
        assertEquals(1L, alert.lotId)
    }

    @Test
    fun `as pendencias do dia mostram medicoes, tratos e biometria vencida`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1)),
            lots = listOf(lot(1, fase = com.valoes.valesaquagestor.domain.model.FaseLote.RECRIA, dataPovoamento = daysAgo(20))),
            waterRecords = listOf(water(1, at = hoursAgo(6), momento = MomentoMedicao.MANHA, oxigenio = 5.0, ph = 7.2)),
            feedings = listOf(feeding(1, at = hoursAgo(5))),
        )

        val tasks = HomeViewModel(farm.overview, FIXED_CLOCK, FakeActivityRepository()).state.first { !it.isLoading }.routine.tasks

        val measurements = tasks.filterIsInstance<PendingTask.Measurement>().map { it.parameter to it.moment }
        assertTrue((MeasuredParameter.OXYGEN to MomentoMedicao.MANHA) !in measurements)
        assertTrue((MeasuredParameter.OXYGEN to MomentoMedicao.TARDE) in measurements)
        assertEquals(1, tasks.filterIsInstance<PendingTask.Feeding>().single().missingMeals)
        assertEquals(5, tasks.filterIsInstance<PendingTask.Biometry>().single().overdueDays)
    }

    @Test
    fun `registrar algo atualiza a rotina sem reabrir a tela`() = runBlocking {
        val farm = FarmFixture(tanks = listOf(tank(1)), lots = listOf(lot(1)))
        val viewModel = HomeViewModel(farm.overview, FIXED_CLOCK, FakeActivityRepository())
        assertEquals(2, viewModel.state.first { !it.isLoading }.routine.tasks.filterIsInstance<PendingTask.Feeding>().single().missingMeals)

        farm.feeding.save(feeding(1, at = hoursAgo(1)))
        farm.feeding.save(feeding(2, at = hoursAgo(2)))

        val updated = viewModel.state.first { state -> state.routine.tasks.none { it is PendingTask.Feeding } }
        assertTrue(updated.routine.tasks.none { it is PendingTask.Feeding })
    }

    // ---- Lista de tanques ----

    @Test
    fun `a lista mostra tipo, lote ativo e pior nivel de cada tanque`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1, "Com lote"), tank(2, "Sem lote"), tank(3, "Sem medicao")),
            lots = listOf(lot(1, 1, especie = Especie.TILAPIA, quantidadeInicial = 1000), lot(2, 3)),
            waterRecords = listOf(
                water(1, tankId = 1, oxigenio = 2.5, ph = 7.0), // CRITICO
                water(2, tankId = 2, oxigenio = 6.0), // ADEQUADO
            ),
            movements = listOf(movement(1, lotId = 1, quantity = 50)),
        )

        val items = TankListViewModel(farm.overview).state.first { !it.isLoading }.tanks.associateBy { it.nome }

        val withLot = items.getValue("Com lote")
        assertEquals(AlertLevel.CRITICO, withLot.worstLevel)
        assertEquals(Especie.TILAPIA, withLot.lots.single().especie)
        assertEquals(950, withLot.lots.single().quantity)
        assertTrue(items.getValue("Sem lote").lots.isEmpty())
        assertEquals(AlertLevel.ADEQUADO, items.getValue("Sem lote").worstLevel)
        assertNull(items.getValue("Sem medicao").worstLevel)
    }

    @Test
    fun `a lista fica vazia sem tanques`() = runBlocking {
        val state = TankListViewModel(FarmFixture().overview).state.first { !it.isLoading }

        assertTrue(state.tanks.isEmpty())
    }
}
