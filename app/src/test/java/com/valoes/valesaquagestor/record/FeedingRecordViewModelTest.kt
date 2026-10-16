package com.valoes.valesaquagestor.record

import androidx.lifecycle.SavedStateHandle
import com.valoes.valesaquagestor.auth.FakeConnectivityChecker
import com.valoes.valesaquagestor.auth.MainDispatcherRule
import com.valoes.valesaquagestor.domain.calc.FeedBlockReason
import com.valoes.valesaquagestor.domain.calc.FeedWarning
import com.valoes.valesaquagestor.domain.model.ComportamentoPeixe
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.model.MotivoSuspensao
import com.valoes.valesaquagestor.farm.FIXED_CLOCK
import com.valoes.valesaquagestor.farm.FarmFixture
import com.valoes.valesaquagestor.farm.NOW
import com.valoes.valesaquagestor.farm.daysAgo
import com.valoes.valesaquagestor.farm.feeding
import com.valoes.valesaquagestor.farm.hoursAgo
import com.valoes.valesaquagestor.farm.lot
import com.valoes.valesaquagestor.farm.movement
import com.valoes.valesaquagestor.farm.tank
import com.valoes.valesaquagestor.farm.water
import com.valoes.valesaquagestor.presentation.record.FeedingRecordViewModel
import com.valoes.valesaquagestor.presentation.util.FormError
import com.valoes.valesaquagestor.presentation.util.SaveFeedback
import com.valoes.valesaquagestor.presentation.util.parseDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Caso B1 de `specs/09`: 500 peixes de 43 g dão 0,3225 kg por refeição (3 refeições). */
class FeedingRecordViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val connectivity = FakeConnectivityChecker(online = true)

    private fun fixture(
        lote: Lote = lot(1, quantidadeInicial = 500, pesoMedioAtualG = 43.0),
        water: List<com.valoes.valesaquagestor.domain.model.QualidadeAgua> = listOf(water(1, at = hoursAgo(2), temperatura = 27.0, oxigenio = 5.0)),
        feedings: List<com.valoes.valesaquagestor.domain.model.Arracoamento> = emptyList(),
        movements: List<com.valoes.valesaquagestor.domain.model.MovimentacaoLote> = emptyList(),
    ) = FarmFixture(tanks = listOf(tank(1, "Viveiro 1")), lots = listOf(lote), waterRecords = water, feedings = feedings, movements = movements)

    private fun viewModel(farm: FarmFixture) = FeedingRecordViewModel(
        SavedStateHandle(mapOf(FeedingRecordViewModel.LOT_ID_ARG to 1L)),
        farm.lots, farm.tanks, farm.movements, farm.water, farm.feeding, connectivity, FIXED_CLOCK,
    )

    @Test
    fun `abre com a quantidade sugerida pela tabela e o horario de agora`() {
        val vm = viewModel(fixture())

        val state = vm.state.value
        assertTrue(state.loaded)
        assertEquals(0.3225, state.suggestion!!.perMealKg, 1e-9)
        assertEquals(3, state.suggestion.band.meals)
        assertEquals(0.3225, parseDecimal(state.quantity)!!, 0.001)
        assertFalse(state.suspended)
        assertEquals(ComportamentoPeixe.NORMAL, state.behavior)
        assertEquals(NOW.atZone(FIXED_CLOCK.zone).toLocalDateTime(), state.dateTime)
        assertTrue("já pode salvar sem digitar nada", state.canSave)
    }

    @Test
    fun `a sugestao usa os peixes vivos, nao os iniciais`() {
        val vm = viewModel(fixture(movements = listOf(movement(1, quantity = 100))))

        // 400 peixes x 43 g = 17,2 kg x 4,5% = 0,774 kg/dia; 3 refeições.
        assertEquals(0.258, vm.state.value.suggestion!!.perMealKg, 1e-9)
    }

    @Test
    fun `marca, proteina e pellet vem da ultima racao do lote`() {
        val last = feeding(1, at = hoursAgo(5)).copy(marcaRacao = "Marca X", proteinaBrutaPct = 36.0, tamanhoPelletMm = 3.5)
        val vm = viewModel(fixture(feedings = listOf(feeding(2, at = daysAgo(3)).copy(marcaRacao = "Antiga"), last)))

        val state = vm.state.value
        assertEquals("Marca X", state.brand)
        assertEquals(36.0, parseDecimal(state.protein)!!, 0.0)
        assertEquals(3.5, parseDecimal(state.pellet)!!, 0.0)
    }

    @Test
    fun `sobra na ultima refeicao avisa para reduzir esta`() {
        val vm = viewModel(fixture(feedings = listOf(feeding(1, at = hoursAgo(5)).copy(sobraRacao = true))))

        assertTrue(vm.state.value.lastHadLeftover)
        assertFalse(viewModel(fixture()).state.value.lastHadLeftover)
    }

    // ---- trava só com oxigênio, temperatura, amônia tóxica e nitrito (specs/08, conflito 13) ----

    @Test
    fun `oxigenio abaixo de 3 sugere 0 kg e ja marca a racao como suspensa com o motivo`() {
        val vm = viewModel(fixture(water = listOf(water(1, at = hoursAgo(1), oxigenio = 2.5, temperatura = 27.0))))

        val state = vm.state.value
        assertEquals(0.0, state.suggestion!!.dailyKg, 0.0)
        assertEquals(listOf(FeedBlockReason.LOW_OXYGEN), state.suggestion.blockReasons)
        assertTrue(state.suspended)
        assertEquals(MotivoSuspensao.OXIGENIO_BAIXO, state.reason)
        assertEquals("", state.quantity)
        assertTrue(state.canSave)
    }

    @Test
    fun `a trava nunca bloqueia, o usuario pode desmarcar a suspensao`() {
        val vm = viewModel(fixture(water = listOf(water(1, at = hoursAgo(1), oxigenio = 2.5, temperatura = 27.0))))

        vm.onSuspendedChange(false)
        assertFalse("sem quantidade o botão fica desabilitado", vm.state.value.canSave)
        vm.onQuantityChange("0,2")
        vm.onSave()

        val saved = vm.state.value.saved
        assertNotNull(saved)
    }

    @Test
    fun `amonia toxica alta, nitrito alto e temperatura fria tambem travam`() {
        val ammonia = viewModel(fixture(water = listOf(water(1, at = hoursAgo(1), amoniaTotal = 1.5, ph = 9.0, temperatura = 27.0))))
        assertEquals(MotivoSuspensao.AMONIA_ALTA, ammonia.state.value.reason)

        val nitrite = viewModel(fixture(water = listOf(water(1, at = hoursAgo(1), nitrito = 0.3, temperatura = 27.0))))
        assertEquals(MotivoSuspensao.NITRITO_ALTO, nitrite.state.value.reason)

        val cold = viewModel(fixture(water = listOf(water(1, at = hoursAgo(1), temperatura = 15.9))))
        assertEquals(MotivoSuspensao.TEMPERATURA, cold.state.value.reason)
    }

    @Test
    fun `pH, transparencia e alcalinidade em nivel critico nao zeram a racao`() {
        val vm = viewModel(
            fixture(water = listOf(water(1, at = hoursAgo(1), ph = 9.5, transparencia = 10.0, alcalinidade = 5.0, temperatura = 27.0, oxigenio = 5.0))),
        )

        assertTrue(vm.state.value.suggestion!!.blockReasons.isEmpty())
        assertFalse(vm.state.value.suspended)
        assertEquals(0.3225, parseDecimal(vm.state.value.quantity)!!, 0.001)
    }

    @Test
    fun `oxigenio exatamente em 3 e permitido com aviso`() {
        val vm = viewModel(fixture(water = listOf(water(1, at = hoursAgo(1), oxigenio = 3.0, temperatura = 27.0))))

        val suggestion = vm.state.value.suggestion!!
        assertTrue(suggestion.blockReasons.isEmpty())
        assertEquals(listOf(FeedWarning.OXYGEN_ATTENTION), suggestion.warnings)
        assertFalse(vm.state.value.suspended)
    }

    @Test
    fun `sem temperatura medida avisa e usa o valor base da tabela`() {
        val vm = viewModel(fixture(water = emptyList()))

        assertEquals(listOf(FeedWarning.TEMPERATURE_MISSING), vm.state.value.suggestion!!.warnings)
        assertEquals(0.3225, vm.state.value.suggestion!!.perMealKg, 1e-9)
    }

    @Test
    fun `lote sem tabela de racao pede a quantidade`() {
        val farm = fixture(lote = lot(1, especie = Especie.OUTRA, quantidadeInicial = 500, pesoMedioAtualG = 43.0))
        val other = viewModel(farm)
        assertTrue(other.state.value.noTable)
        assertEquals("", other.state.value.quantity)
        assertFalse(other.state.value.canSave)

        val heavy = viewModel(fixture(lote = lot(1, quantidadeInicial = 500, pesoMedioAtualG = 1800.0)))
        assertTrue(heavy.state.value.noTable)

        other.onQuantityChange("2")
        other.onSave()
        assertEquals(2.0, farm.feeding.feedings.value.single().quantidadeKg, 0.0)
    }

    // ---- validação e salvamento ----

    @Test
    fun `quantidade invalida ou zero e recusada quando a racao nao esta suspensa`() {
        val farm = fixture()
        val vm = viewModel(farm)

        vm.onQuantityChange("0")
        vm.onSave()
        assertEquals(FormError.NOT_POSITIVE, vm.state.value.quantityError)

        vm.onQuantityChange("abc")
        vm.onSave()
        assertEquals(FormError.INVALID_NUMBER, vm.state.value.quantityError)
        assertTrue(farm.feeding.feedings.value.isEmpty())
    }

    @Test
    fun `racao suspensa exige o motivo e proteina nao passa de 100`() {
        val farm = fixture()
        val vm = viewModel(farm)

        vm.onSuspendedChange(true)
        vm.onProteinChange("150")
        vm.onSave()

        assertEquals(FormError.REQUIRED, vm.state.value.reasonError)
        assertEquals(FormError.OUT_OF_RANGE_PERCENT, vm.state.value.proteinError)
        assertTrue(farm.feeding.feedings.value.isEmpty())
    }

    @Test
    fun `salva o trato com tudo o que foi preenchido`() {
        val farm = fixture()
        val vm = viewModel(farm)
        vm.onQuantityChange("0,4")
        vm.onBrandChange("  Marca Y  ")
        vm.onProteinChange("32")
        vm.onPelletChange("4,5")
        vm.onBehaviorChange(ComportamentoPeixe.VORAZ)
        vm.onLeftoverChange(true)

        vm.onSave()

        val saved = farm.feeding.feedings.value.single()
        assertEquals(1L, saved.loteId)
        assertEquals(0.4, saved.quantidadeKg, 0.0)
        assertFalse(saved.suspenso)
        assertNull(saved.motivoSuspensao)
        assertEquals("Marca Y", saved.marcaRacao)
        assertEquals(32.0, saved.proteinaBrutaPct!!, 0.0)
        assertEquals(4.5, saved.tamanhoPelletMm!!, 0.0)
        assertEquals(ComportamentoPeixe.VORAZ, saved.comportamento)
        assertTrue(saved.sobraRacao)
        assertEquals(NOW, saved.dataHora)
        assertEquals(SaveFeedback.SAVED, vm.state.value.saved)
    }

    @Test
    fun `racao suspensa grava 0 kg com o motivo`() {
        val farm = fixture()
        val vm = viewModel(farm)
        vm.onSuspendedChange(true)
        vm.onReasonChange(MotivoSuspensao.DOENCA)
        vm.onQuantityChange("5") // some da tela, mas não pode ser gravado

        vm.onSave()

        val saved = farm.feeding.feedings.value.single()
        assertTrue(saved.suspenso)
        assertEquals(0.0, saved.quantidadeKg, 0.0)
        assertEquals(MotivoSuspensao.DOENCA, saved.motivoSuspensao)
    }

    @Test
    fun `sem internet o aviso diz que ficou guardado no aparelho`() {
        connectivity.online = false
        val vm = viewModel(fixture())

        vm.onSave()

        assertEquals(SaveFeedback.SAVED_OFFLINE, vm.state.value.saved)
    }
}
