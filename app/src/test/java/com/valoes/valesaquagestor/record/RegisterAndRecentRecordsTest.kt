package com.valoes.valesaquagestor.record

import androidx.lifecycle.SavedStateHandle
import com.valoes.valesaquagestor.auth.MainDispatcherRule
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import com.valoes.valesaquagestor.farm.FarmFixture
import com.valoes.valesaquagestor.farm.biometry
import com.valoes.valesaquagestor.farm.feeding
import com.valoes.valesaquagestor.farm.hoursAgo
import com.valoes.valesaquagestor.farm.lot
import com.valoes.valesaquagestor.farm.movement
import com.valoes.valesaquagestor.farm.tank
import com.valoes.valesaquagestor.farm.water
import com.valoes.valesaquagestor.presentation.lot.LotDetailViewModel
import com.valoes.valesaquagestor.presentation.main.RegisterViewModel
import com.valoes.valesaquagestor.presentation.tank.TankDetailViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegisterAndRecentRecordsTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    // ---- Aba Registrar ----

    @Test
    fun `a aba Registrar lista os tanques por nome com os lotes ativos`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1, "Zeta"), tank(2, "Alfa")),
            lots = listOf(lot(1, tankId = 1, fase = FaseLote.ENGORDA), lot(2, tankId = 1, especie = Especie.TAMBAQUI)),
        )

        val state = RegisterViewModel(farm.overview).state.first { !it.isLoading }

        assertEquals(listOf("Alfa", "Zeta"), state.tanks.map { it.nome })
        assertTrue(state.tanks.first().lots.isEmpty())
        assertEquals(listOf(1L, 2L), state.tanks.last().lots.map { it.lotId }.sorted())
        assertEquals(FaseLote.ENGORDA, state.tanks.last().lots.first { it.lotId == 1L }.fase)
    }

    @Test
    fun `sem tanques a aba Registrar fica vazia`() = runBlocking {
        assertTrue(RegisterViewModel(FarmFixture().overview).state.first { !it.isLoading }.tanks.isEmpty())
    }

    // ---- Últimos registros do lote ----

    private fun lotDetail(farm: FarmFixture) = LotDetailViewModel(
        SavedStateHandle(mapOf(LotDetailViewModel.LOT_ID_ARG to 1L)),
        farm.lots, farm.tanks, farm.water, farm.settings, farm.movements, farm.biometry, farm.feeding,
    )

    @Test
    fun `o lote mostra so os 5 registros mais recentes de cada tipo, do mais novo ao mais antigo`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1)),
            lots = listOf(lot(1)),
            feedings = (1L..7L).map { feeding(it, at = hoursAgo(10 - it), kg = it.toDouble()) },
            biometries = (1L..6L).map { biometry(it, at = hoursAgo(20 - it), kg = 1.0, fish = 10) },
            movements = (1L..6L).map { movement(it, quantity = it.toInt(), at = hoursAgo(30 - it)) },
        )

        val data = lotDetail(farm).state.first { !it.isLoading }.lot!!

        assertEquals(listOf(7L, 6L, 5L, 4L, 3L), data.recentFeedings.map { it.id })
        assertEquals(listOf(6L, 5L, 4L, 3L, 2L), data.recentBiometries.map { it.id })
        assertEquals(listOf(6L, 5L, 4L, 3L, 2L), data.recentMovements.map { it.id })
    }

    @Test
    fun `excluir um trato tira da lista e do calculo`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1)),
            lots = listOf(lot(1)),
            feedings = listOf(feeding(1, at = hoursAgo(3)), feeding(2, at = hoursAgo(2))),
        )
        val vm = lotDetail(farm)
        assertEquals(2, vm.state.first { it.lot != null }.lot!!.recentFeedings.size)

        vm.deleteFeeding(1)

        assertEquals(listOf(2L), vm.state.first { it.lot?.recentFeedings?.size == 1 }.lot!!.recentFeedings.map { it.id })
    }

    @Test
    fun `excluir uma mortalidade devolve os peixes ao lote`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1)),
            lots = listOf(lot(1, quantidadeInicial = 1000)),
            movements = listOf(movement(1, quantity = 150, tipo = TipoMovimentacao.MORTALIDADE)),
        )
        val vm = lotDetail(farm)
        assertEquals(850, vm.state.first { it.lot != null }.lot!!.currentQuantity)

        vm.deleteMovement(1)

        val data = vm.state.first { it.lot?.recentMovements?.isEmpty() == true }.lot!!
        assertEquals(1000, data.currentQuantity)
        assertTrue("sem a mortalidade, o alerta de 10% some", data.alerts.isEmpty())
    }

    @Test
    fun `excluir uma biometria tira da lista`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1)),
            lots = listOf(lot(1)),
            biometries = listOf(biometry(1, at = hoursAgo(5), kg = 5.0, fish = 20)),
        )
        val vm = lotDetail(farm)
        assertTrue(vm.state.first { it.lot != null }.lot!!.hasBiometry)

        vm.deleteBiometry(1)

        assertTrue(vm.state.first { it.lot?.hasBiometry == false }.lot!!.recentBiometries.isEmpty())
    }

    // ---- Últimas medições do tanque ----

    private fun tankDetail(farm: FarmFixture) =
        TankDetailViewModel(SavedStateHandle(mapOf(TankDetailViewModel.TANK_ID_ARG to 1L)), farm.overview, farm.water)

    @Test
    fun `o tanque mostra as 5 medicoes mais recentes e permite excluir`() = runBlocking {
        val farm = FarmFixture(
            tanks = listOf(tank(1)),
            waterRecords = (1L..6L).map { water(it, at = hoursAgo(10 - it), oxigenio = 5.0) },
        )
        val vm = tankDetail(farm)

        val before = vm.state.first { !it.isLoading }.tank!!.recentRecords
        assertEquals(listOf(6L, 5L, 4L, 3L, 2L), before.map { it.id })

        vm.deleteWaterRecord(6)

        val after = vm.state.first { state -> state.tank?.recentRecords?.firstOrNull()?.id == 5L }.tank!!.recentRecords
        assertEquals(listOf(5L, 4L, 3L, 2L, 1L), after.map { it.id })
    }
}
