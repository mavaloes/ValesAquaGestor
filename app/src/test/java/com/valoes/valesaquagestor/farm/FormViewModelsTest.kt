package com.valoes.valesaquagestor.farm

import androidx.lifecycle.SavedStateHandle
import com.valoes.valesaquagestor.auth.MainDispatcherRule
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.TipoTanque
import com.valoes.valesaquagestor.presentation.lot.LotFormViewModel
import com.valoes.valesaquagestor.presentation.tank.TankFormViewModel
import com.valoes.valesaquagestor.presentation.util.FormError
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FormViewModelsTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    // ---- Novo tanque ----

    private val tankRepository = FakeTankRepository()
    private val tankForm = TankFormViewModel(tankRepository)

    private fun TankFormViewModel.fillValidViveiro() {
        onNameChange("Viveiro 1")
        onAreaChange("1000")
        onRenewalChange("5")
    }

    @Test
    fun `o botao salvar so habilita com os obrigatorios preenchidos`() {
        assertFalse(tankForm.state.value.canSave)

        tankForm.onNameChange("Viveiro 1")
        assertFalse(tankForm.state.value.canSave)
        tankForm.onAreaChange("1000")
        assertFalse(tankForm.state.value.canSave)
        tankForm.onRenewalChange("5")
        assertTrue(tankForm.state.value.canSave)
    }

    @Test
    fun `viveiro pede area e os outros tipos pedem volume`() {
        tankForm.onNameChange("Tanque")
        tankForm.onRenewalChange("0")

        tankForm.onTypeChange(TipoTanque.ESCAVADO)
        assertTrue(tankForm.state.value.usesArea)
        assertFalse(tankForm.state.value.canSave)

        tankForm.onTypeChange(TipoTanque.TANQUE_REDE)
        assertFalse(tankForm.state.value.usesArea)
        assertFalse(tankForm.state.value.canSave)
        tankForm.onVolumeChange("20")
        assertTrue(tankForm.state.value.canSave)
    }

    @Test
    fun `salvar com erros mostra a mensagem de cada campo e nao grava`() = runBlocking {
        tankForm.onNameChange("   ")
        tankForm.onAreaChange("abc")
        tankForm.onDepthChange("-1")
        tankForm.onRenewalChange("150")

        tankForm.onSave()

        val state = tankForm.state.value
        assertEquals(FormError.REQUIRED, state.nameError)
        assertEquals(FormError.INVALID_NUMBER, state.areaError)
        assertEquals(FormError.NOT_POSITIVE, state.depthError)
        assertEquals(FormError.OUT_OF_RANGE_PERCENT, state.renewalError)
        assertTrue(tankRepository.tanks.value.isEmpty())
        assertNull(state.savedTankId)
    }

    @Test
    fun `digitar apaga o erro do campo`() {
        tankForm.onAreaChange("abc")
        tankForm.onSave()
        assertEquals(FormError.INVALID_NUMBER, tankForm.state.value.areaError)

        tankForm.onAreaChange("100")

        assertNull(tankForm.state.value.areaError)
    }

    @Test
    fun `salva o viveiro com virgula decimal e calcula o volume pela area e profundidade`() = runBlocking {
        tankForm.fillValidViveiro()
        tankForm.onAreaChange("1000,5")
        tankForm.onDepthChange("1,5")
        tankForm.onHasAeratorChange(true)
        tankForm.onAeratorPowerChange("2")

        tankForm.onSave()

        val saved = tankRepository.tanks.value.single()
        assertEquals(1L, tankForm.state.value.savedTankId)
        assertEquals("Viveiro 1", saved.nome)
        assertEquals(TipoTanque.ESCAVADO, saved.tipo)
        assertEquals(1000.5, saved.areaM2!!, 1e-9)
        assertEquals(1000.5 * 1.5, saved.volumeM3!!, 1e-9)
        assertTrue(saved.usaAerador)
        assertEquals(2.0, saved.aeradorPotenciaHp!!, 0.0)
        assertEquals(5.0, saved.renovacaoAguaPctDia, 0.0)
    }

    @Test
    fun `potencia do aerador some quando o tanque nao tem aerador`() = runBlocking {
        tankForm.fillValidViveiro()
        tankForm.onHasAeratorChange(true)
        tankForm.onAeratorPowerChange("2")
        tankForm.onHasAeratorChange(false)

        tankForm.onSave()

        val saved = tankRepository.tanks.value.single()
        assertFalse(saved.usaAerador)
        assertNull(saved.aeradorPotenciaHp)
    }

    @Test
    fun `tanque redondo salva o volume informado`() = runBlocking {
        tankForm.onNameChange("Caixa")
        tankForm.onTypeChange(TipoTanque.REDONDO)
        tankForm.onVolumeChange("15")
        tankForm.onRenewalChange("10")

        tankForm.onSave()

        val saved = tankRepository.tanks.value.single()
        assertEquals(15.0, saved.volumeM3!!, 0.0)
        assertNull(saved.areaM2)
    }

    // ---- Novo lote ----

    private val lotRepository = FakeLotRepository()

    private fun lotForm(tank: com.valoes.valesaquagestor.domain.model.Tanque = tank(1, areaM2 = 225.0)): LotFormViewModel {
        val tanks = FakeTankRepository(listOf(tank))
        return LotFormViewModel(SavedStateHandle(mapOf(LotFormViewModel.TANK_ID_ARG to tank.id)), lotRepository, tanks, FIXED_CLOCK)
    }

    private fun LotFormViewModel.fillValidLot(quantity: String = "1000", finalWeight: String = "20") {
        onPhaseChange(FaseLote.ALEVINAGEM)
        onQuantityChange(quantity)
        onInitialWeightChange("5")
        onFinalWeightChange(finalWeight)
    }

    @Test
    fun `o formulario do lote carrega o tanque e a data de hoje`() {
        val form = lotForm()

        val state = form.state.value
        assertTrue(state.tankLoaded)
        assertEquals("Viveiro 1", state.tankName)
        assertTrue(state.capacityApplies)
        assertEquals(LocalDate.of(2026, 10, 14), state.stockingDate)
    }

    @Test
    fun `viveiro pede o peso final e mostra a capacidade`() {
        val form = lotForm()
        form.onQuantityChange("1000")
        form.onInitialWeightChange("5")
        assertFalse("falta o peso final", form.state.value.canSave)
        assertNull(form.state.value.capacity)

        form.onFinalWeightChange("20")

        assertTrue(form.state.value.canSave)
        val capacity = form.state.value.capacity!!
        assertEquals(1350, capacity.maxFish) // D1: 225 m2, alevinagem, 20 g
        assertFalse(capacity.exceeds)
    }

    @Test
    fun `passar da capacidade avisa mas deixa salvar mesmo assim`() = runBlocking {
        val form = lotForm()
        form.fillValidLot(quantity = "1351")

        assertTrue(form.state.value.exceedsCapacity)
        assertTrue(form.state.value.canSave)
        form.onSave()

        assertNotNull(form.state.value.savedLotId)
        assertEquals(1351, lotRepository.lots.value.single().quantidadeInicial)
    }

    @Test
    fun `no limite da capacidade nao avisa`() {
        val form = lotForm()
        form.fillValidLot(quantity = "1350")

        assertFalse(form.state.value.exceedsCapacity)
    }

    @Test
    fun `mudar a fase reconfere a capacidade`() {
        val form = lotForm()
        form.fillValidLot(quantity = "1000", finalWeight = "20")
        assertEquals(1350, form.state.value.capacity!!.maxFish)

        form.onPhaseChange(FaseLote.ENGORDA) // 600 kg/1.000 m2 -> 135 kg em 225 m2 -> 6.750 peixes de 20 g

        assertEquals(6750, form.state.value.capacity!!.maxFish)
    }

    @Test
    fun `tanque redondo nao calcula capacidade nem pede peso final`() = runBlocking {
        val form = lotForm(tank(1, tipo = TipoTanque.REDONDO, areaM2 = null))
        form.onQuantityChange("100")
        form.onInitialWeightChange("5")

        assertFalse(form.state.value.capacityApplies)
        assertTrue(form.state.value.canSave)
        assertNull(form.state.value.capacity)
        form.onSave()
        assertNotNull(form.state.value.savedLotId)
    }

    @Test
    fun `o lote salvo comeca com o peso atual igual ao inicial e a data de agora`() = runBlocking {
        val form = lotForm()
        form.fillValidLot()
        form.onSpeciesChange(Especie.TILAPIA)
        form.onOriginChange("  Alevinos Sul  ")

        form.onSave()

        val saved = lotRepository.lots.value.single()
        assertEquals(1L, saved.tanqueId)
        assertEquals(5.0, saved.pesoMedioInicialG, 0.0)
        assertEquals(5.0, saved.pesoMedioAtualG, 0.0)
        assertEquals(NOW, saved.dataPovoamento)
        assertEquals("Alevinos Sul", saved.origemAlevinos)
    }

    @Test
    fun `data de povoamento antiga vira o comeco daquele dia`() = runBlocking {
        val form = lotForm()
        form.fillValidLot()
        form.onStockingDateChange(LocalDate.of(2026, 10, 1))

        form.onSave()

        assertEquals(java.time.Instant.parse("2026-10-01T00:00:00Z"), lotRepository.lots.value.single().dataPovoamento)
    }

    @Test
    fun `quantidade e pesos invalidos sao recusados`() = runBlocking {
        val form = lotForm()
        form.onQuantityChange("0")
        form.onInitialWeightChange("abc")
        form.onFinalWeightChange("-5")

        form.onSave()

        val state = form.state.value
        assertEquals(FormError.NOT_POSITIVE, state.quantityError)
        assertEquals(FormError.INVALID_NUMBER, state.initialWeightError)
        assertEquals(FormError.NOT_POSITIVE, state.finalWeightError)
        assertTrue(lotRepository.lots.value.isEmpty())
    }

    @Test
    fun `quantidade com virgula nao e numero inteiro`() = runBlocking {
        val form = lotForm()
        form.fillValidLot(quantity = "10,5")

        form.onSave()

        assertEquals(FormError.INVALID_NUMBER, form.state.value.quantityError)
    }
}
