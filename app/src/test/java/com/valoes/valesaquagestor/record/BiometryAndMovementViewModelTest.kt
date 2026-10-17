package com.valoes.valesaquagestor.record

import androidx.lifecycle.SavedStateHandle
import com.valoes.valesaquagestor.auth.FakeConnectivityChecker
import com.valoes.valesaquagestor.auth.MainDispatcherRule
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import com.valoes.valesaquagestor.domain.rules.AlertLevel
import com.valoes.valesaquagestor.domain.rules.PlausibilityWarning
import com.valoes.valesaquagestor.farm.FIXED_CLOCK
import com.valoes.valesaquagestor.farm.FarmFixture
import com.valoes.valesaquagestor.farm.NOW
import com.valoes.valesaquagestor.farm.lot
import com.valoes.valesaquagestor.farm.movement
import com.valoes.valesaquagestor.farm.tank
import com.valoes.valesaquagestor.presentation.record.BiometryViewModel
import com.valoes.valesaquagestor.presentation.record.MovementViewModel
import com.valoes.valesaquagestor.presentation.util.FormError
import com.valoes.valesaquagestor.presentation.util.SaveFeedback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BiometryAndMovementViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val connectivity = FakeConnectivityChecker(online = true)

    // ---- Biometria: lote de 1.000 peixes vivos de 250 g ----

    private fun biometryFarm(alive: Int = 1000, weightG: Double = 250.0, species: Especie = Especie.TILAPIA) = FarmFixture(
        tanks = listOf(tank(1, "Viveiro 1")),
        lots = listOf(lot(1, especie = species, quantidadeInicial = alive, pesoMedioAtualG = weightG)),
    )

    private fun biometryVm(farm: FarmFixture) = BiometryViewModel(
        SavedStateHandle(mapOf(BiometryViewModel.LOT_ID_ARG to 1L)),
        farm.lots, farm.tanks, farm.movements, farm.water, farm.settings, farm.biometry, connectivity, FIXED_CLOCK,
    )

    @Test
    fun `a biometria abre com uma pesagem vazia e a amostra recomendada do lote`() {
        val vm = biometryVm(biometryFarm())

        val state = vm.state.value
        assertTrue(state.loaded)
        assertEquals(1, state.weighings.size)
        assertEquals(100, state.recommendedSample) // 10% de 1.000
        assertNull(state.summary)
        assertFalse(state.canSave)
    }

    @Test
    fun `o peso medio e calculado na hora, com tara em branco valendo zero`() {
        val vm = biometryVm(biometryFarm())
        val row = vm.state.value.weighings.single().id

        vm.onGrossChange(row, "5")
        vm.onFishCountChange(row, "20")

        val summary = vm.state.value.summary!!
        assertEquals(250.0, summary.averageWeightG, 1e-9) // 5 kg / 20 peixes
        assertEquals(5.0, summary.netWeightKg, 1e-9)

        vm.onTareChange(row, "1")
        assertEquals(200.0, vm.state.value.summary!!.averageWeightG, 1e-9) // (5 - 1) kg / 20
    }

    @Test
    fun `varias pesagens somam peso e peixes`() {
        val vm = biometryVm(biometryFarm())
        val first = vm.state.value.weighings.single().id
        vm.onGrossChange(first, "6"); vm.onTareChange(first, "1"); vm.onFishCountChange(first, "20")
        vm.onAddWeighing()
        val second = vm.state.value.weighings.last().id
        vm.onGrossChange(second, "5,5"); vm.onTareChange(second, "1,5"); vm.onFishCountChange(second, "20")

        val summary = vm.state.value.summary!!

        assertEquals(2, vm.state.value.weighings.size)
        assertEquals(9.0, summary.netWeightKg, 1e-9)
        assertEquals(40, summary.fishCount)
        assertEquals(225.0, summary.averageWeightG, 1e-9)
    }

    @Test
    fun `pesagem remove-se mas sempre sobra uma`() {
        val vm = biometryVm(biometryFarm())
        vm.onAddWeighing()
        val ids = vm.state.value.weighings.map { it.id }

        vm.onRemoveWeighing(ids.first())
        assertEquals(listOf(ids.last()), vm.state.value.weighings.map { it.id })

        vm.onRemoveWeighing(ids.last())
        assertEquals(1, vm.state.value.weighings.size)
    }

    @Test
    fun `amostra menor que a recomendada avisa mas deixa salvar`() {
        val vm = biometryVm(biometryFarm())
        val row = vm.state.value.weighings.single().id
        vm.onGrossChange(row, "5"); vm.onFishCountChange(row, "20")

        assertTrue(vm.state.value.sampleTooSmall)
        assertTrue(vm.state.value.hasWarnings)
        assertTrue(vm.state.value.canSave)

        vm.onFishCountChange(row, "100")
        assertFalse("100 peixes é a amostra recomendada", vm.state.value.sampleTooSmall)
    }

    @Test
    fun `em lote grande a amostra recomendada e de 200 peixes`() {
        assertEquals(200, biometryVm(biometryFarm(alive = 5000)).state.value.recommendedSample)
    }

    @Test
    fun `peso medio 10 vezes maior que o do lote avisa`() {
        val vm = biometryVm(biometryFarm(weightG = 20.0))
        val row = vm.state.value.weighings.single().id

        vm.onGrossChange(row, "4"); vm.onFishCountChange(row, "20") // 200 g = 10 x 20 g
        assertEquals(listOf(PlausibilityWarning.AVERAGE_WEIGHT_JUMP), vm.state.value.warnings)

        vm.onGrossChange(row, "3,9") // 195 g
        assertTrue(vm.state.value.warnings.isEmpty())
    }

    @Test
    fun `peso com peixes igual ou menor que a tara e recusado`() {
        val vm = biometryVm(biometryFarm())
        val row = vm.state.value.weighings.single().id
        vm.onGrossChange(row, "1"); vm.onTareChange(row, "1"); vm.onFishCountChange(row, "20")

        assertTrue("o botão habilita para o erro aparecer ao tocar", vm.state.value.canSave)
        vm.onSave()

        assertEquals(FormError.GROSS_BELOW_TARE, vm.state.value.weighings.single().grossError)
        assertNull(vm.state.value.saved)
    }

    @Test
    fun `pesagem incompleta mostra o erro de cada campo e nao salva`() {
        val farm = biometryFarm()
        val vm = biometryVm(farm)
        val row = vm.state.value.weighings.single().id
        vm.onGrossChange(row, "abc")
        vm.onTareChange(row, "-1")

        vm.onSave()

        val saved = vm.state.value.weighings.single()
        assertEquals(FormError.INVALID_NUMBER, saved.grossError)
        assertEquals(FormError.NEGATIVE, saved.tareError)
        assertEquals(FormError.REQUIRED, saved.fishCountError)
        assertTrue(farm.biometry.biometries.value.isEmpty())
    }

    @Test
    fun `linha em branco e ignorada ao salvar`() {
        val farm = biometryFarm()
        val vm = biometryVm(farm)
        val row = vm.state.value.weighings.single().id
        vm.onGrossChange(row, "5"); vm.onFishCountChange(row, "20")
        vm.onAddWeighing()

        vm.onSave()

        assertEquals(1, farm.biometry.biometries.value.size)
        assertNotNull(vm.state.value.saved)
    }

    @Test
    fun `salva os totais e mostra o peso medio e a racao sugerida`() {
        val farm = biometryFarm()
        val vm = biometryVm(farm)
        val row = vm.state.value.weighings.single().id
        vm.onGrossChange(row, "6"); vm.onTareChange(row, "1"); vm.onFishCountChange(row, "20")
        vm.onNotesChange("  peixes sadios  ")

        vm.onSave()

        val saved = farm.biometry.biometries.value.single()
        assertEquals(5.0, saved.pesoAmostraKg, 1e-9)
        assertEquals(20, saved.numeroPeixesAmostra)
        assertEquals(250.0, saved.pesoMedioG, 1e-9)
        assertEquals("peixes sadios", saved.observacaoSaude)
        assertEquals(NOW, saved.dataHora)
        assertEquals(SaveFeedback.SAVED, vm.state.value.saved)

        val result = vm.state.value.result!!
        assertEquals(250.0, result.averageWeightG, 1e-9)
        // 1.000 peixes x 250 g = 250 kg; faixa 250 a 400 g = 2,2% do peso vivo; sem temperatura medida o fator é 1,0.
        assertEquals(5.5, result.suggestion!!.dailyKg, 1e-9)
        assertEquals(2.75, result.suggestion.perMealKg, 1e-9)
    }

    @Test
    fun `outra especie nao tem sugestao de racao`() {
        val vm = biometryVm(biometryFarm(species = Especie.OUTRA))
        val row = vm.state.value.weighings.single().id
        vm.onGrossChange(row, "5"); vm.onFishCountChange(row, "20")

        vm.onSave()

        assertNull(vm.state.value.result!!.suggestion)
    }

    @Test
    fun `sem internet a biometria avisa que ficou guardada no aparelho`() {
        connectivity.online = false
        val vm = biometryVm(biometryFarm())
        val row = vm.state.value.weighings.single().id
        vm.onGrossChange(row, "5"); vm.onFishCountChange(row, "20")

        vm.onSave()

        assertEquals(SaveFeedback.SAVED_OFFLINE, vm.state.value.saved)
    }

    // ---- Mortalidade e despesca: lote de 1.000 peixes, 40 já mortos (960 vivos) ----

    private fun movementFarm(initial: Int = 1000, dead: Int = 40) = FarmFixture(
        tanks = listOf(tank(1, "Viveiro 1")),
        lots = listOf(lot(1, quantidadeInicial = initial)),
        movements = if (dead > 0) listOf(movement(1, quantity = dead)) else emptyList(),
    )

    private fun movementVm(farm: FarmFixture) = MovementViewModel(
        SavedStateHandle(mapOf(MovementViewModel.LOT_ID_ARG to 1L)),
        farm.lots, farm.tanks, farm.movements, connectivity, FIXED_CLOCK,
    )

    @Test
    fun `a tela abre com os peixes vivos e a mortalidade ate agora`() {
        val state = movementVm(movementFarm()).state.value

        assertTrue(state.loaded)
        assertEquals(960, state.aliveFish)
        assertEquals(TipoMovimentacao.MORTALIDADE, state.type)
        assertEquals(4.0, state.projectedMortalityPct!!, 1e-9)
        assertNull(state.mortalityWarning)
        assertFalse(state.canSave)
    }

    @Test
    fun `a mortalidade acumulada acima de 10 por cento avisa antes de salvar`() {
        val vm = movementVm(movementFarm())

        vm.onQuantityChange("60") // 100 de 1.000 = 10%: no limite, sem aviso
        assertEquals(10.0, vm.state.value.projectedMortalityPct!!, 1e-9)
        assertNull(vm.state.value.mortalityWarning)

        vm.onQuantityChange("61") // 10,1%
        assertEquals(AlertLevel.ATENCAO, vm.state.value.mortalityWarning!!.level)
    }

    @Test
    fun `nao se pode retirar mais peixes do que existem vivos`() {
        val farm = movementFarm()
        val vm = movementVm(farm)

        vm.onQuantityChange("961")
        vm.onSave()
        assertEquals(FormError.EXCEEDS_ALIVE, vm.state.value.quantityError)

        vm.onQuantityChange("0")
        vm.onSave()
        assertEquals(FormError.NOT_POSITIVE, vm.state.value.quantityError)

        vm.onQuantityChange("abc")
        vm.onSave()
        assertEquals(FormError.INVALID_NUMBER, vm.state.value.quantityError)
        assertEquals(1, farm.movements.movements.value.size) // só a morte que já existia
    }

    @Test
    fun `mortalidade salva direto, sem confirmacao`() {
        val farm = movementFarm()
        val vm = movementVm(farm)
        vm.onQuantityChange("12")
        vm.onNotesChange("  boqueando  ")

        vm.onSave()

        val saved = farm.movements.movements.value.last()
        assertEquals(TipoMovimentacao.MORTALIDADE, saved.tipo)
        assertEquals(12, saved.quantidade)
        assertEquals("boqueando", saved.observacao)
        assertEquals(NOW, saved.dataHora)
        assertEquals(SaveFeedback.SAVED, vm.state.value.saved)
        assertFalse(vm.state.value.confirmFinal)
    }

    @Test
    fun `despesca parcial grava o peso e nao mostra mortalidade`() {
        val farm = movementFarm()
        val vm = movementVm(farm)
        vm.onTypeChange(TipoMovimentacao.DESPESCA_PARCIAL)
        assertNull(vm.state.value.projectedMortalityPct)
        vm.onQuantityChange("200")
        vm.onTotalWeightChange("150,5")

        vm.onSave()

        val saved = farm.movements.movements.value.last()
        assertEquals(TipoMovimentacao.DESPESCA_PARCIAL, saved.tipo)
        assertEquals(200, saved.quantidade)
        assertEquals(150.5, saved.pesoTotalKg!!, 0.0)
    }

    @Test
    fun `peso retirado negativo e recusado`() {
        val vm = movementVm(movementFarm())
        vm.onTypeChange(TipoMovimentacao.DESPESCA_PARCIAL)
        vm.onQuantityChange("10")
        vm.onTotalWeightChange("-5")

        vm.onSave()

        assertEquals(FormError.NEGATIVE, vm.state.value.weightError)
        assertNull(vm.state.value.saved)
    }

    @Test
    fun `despesca final retira todos os vivos e pede confirmacao antes de salvar`() {
        val farm = movementFarm()
        val vm = movementVm(farm)
        vm.onTypeChange(TipoMovimentacao.DESPESCA_FINAL)

        assertTrue(vm.state.value.quantityLocked)
        assertEquals("960", vm.state.value.quantity)

        vm.onSave()
        assertTrue("pede confirmação", vm.state.value.confirmFinal)
        assertEquals("ainda não salvou", 1, farm.movements.movements.value.size)

        vm.onDismissFinal()
        assertFalse(vm.state.value.confirmFinal)
        assertEquals(1, farm.movements.movements.value.size)

        vm.onSave()
        vm.onConfirmFinal()
        val saved = farm.movements.movements.value.last()
        assertEquals(TipoMovimentacao.DESPESCA_FINAL, saved.tipo)
        assertEquals(960, saved.quantidade)
        assertNotNull(vm.state.value.saved)
    }

    @Test
    fun `sair da despesca final limpa a quantidade`() {
        val vm = movementVm(movementFarm())
        vm.onTypeChange(TipoMovimentacao.DESPESCA_FINAL)

        vm.onTypeChange(TipoMovimentacao.MORTALIDADE)

        assertEquals("", vm.state.value.quantity)
        assertFalse(vm.state.value.quantityLocked)
    }

    @Test
    fun `lote sem peixes vivos nao deixa registrar`() {
        val vm = movementVm(movementFarm(initial = 100, dead = 100))

        assertEquals(0, vm.state.value.aliveFish)
        vm.onQuantityChange("1")
        assertFalse(vm.state.value.canSave)
    }

    @Test
    fun `sem internet o registro avisa que ficou guardado no aparelho`() {
        connectivity.online = false
        val vm = movementVm(movementFarm())
        vm.onQuantityChange("5")

        vm.onSave()

        assertEquals(SaveFeedback.SAVED_OFFLINE, vm.state.value.saved)
    }
}
