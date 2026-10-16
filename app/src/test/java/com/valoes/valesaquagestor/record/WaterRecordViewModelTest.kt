package com.valoes.valesaquagestor.record

import androidx.lifecycle.SavedStateHandle
import com.valoes.valesaquagestor.auth.FakeConnectivityChecker
import com.valoes.valesaquagestor.auth.MainDispatcherRule
import com.valoes.valesaquagestor.data.sensor.ManualWaterSensorSource
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.AlertLevel
import com.valoes.valesaquagestor.domain.rules.PlausibilityWarning
import com.valoes.valesaquagestor.farm.FIXED_CLOCK
import com.valoes.valesaquagestor.farm.FarmFixture
import com.valoes.valesaquagestor.farm.NOW
import com.valoes.valesaquagestor.farm.hoursAgo
import com.valoes.valesaquagestor.farm.tank
import com.valoes.valesaquagestor.farm.water
import com.valoes.valesaquagestor.presentation.record.WaterField
import com.valoes.valesaquagestor.presentation.record.WaterRecordViewModel
import com.valoes.valesaquagestor.presentation.util.FormError
import com.valoes.valesaquagestor.presentation.util.SaveFeedback
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** O relógio fixo marca 14/10/2026 15:00, então o momento sugerido é "tarde". */
class WaterRecordViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val connectivity = FakeConnectivityChecker(online = true)

    private fun viewModel(farm: FarmFixture = FarmFixture(tanks = listOf(tank(1, "Viveiro 1")))) = Pair(
        WaterRecordViewModel(SavedStateHandle(mapOf(WaterRecordViewModel.TANK_ID_ARG to 1L)), farm.water, farm.tanks, farm.settings, ManualWaterSensorSource(), connectivity, FIXED_CLOCK),
        farm,
    )

    @Test
    fun `abre com o tanque, a data e hora de agora e o momento sugerido`() {
        val (vm, _) = viewModel()

        val state = vm.state.value
        assertTrue(state.tankLoaded)
        assertEquals("Viveiro 1", state.tankName)
        assertEquals(LocalDateTime.of(2026, 10, 14, 15, 0), state.dateTime)
        assertEquals(MomentoMedicao.TARDE, state.moment)
        assertFalse("sem nenhuma medida o botão fica desabilitado", state.canSave)
    }

    @Test
    fun `basta uma medida para habilitar o botao`() {
        val (vm, _) = viewModel()

        vm.onValueChange(WaterField.OXYGEN, "5")

        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun `o momento acompanha a hora ate o usuario escolher`() {
        val (vm, _) = viewModel()

        vm.onDateTimeChange(LocalDateTime.of(2026, 10, 14, 6, 10))
        assertEquals(MomentoMedicao.MANHA, vm.state.value.moment)

        vm.onMomentChange(MomentoMedicao.NOITE)
        vm.onDateTimeChange(LocalDateTime.of(2026, 10, 14, 11, 30))
        assertEquals("depois de escolher, a hora não troca mais o momento", MomentoMedicao.NOITE, vm.state.value.moment)
    }

    @Test
    fun `valores impossiveis ou invalidos sao bloqueados`() {
        val (vm, farm) = viewModel()
        vm.onValueChange(WaterField.OXYGEN, "abc")
        vm.onValueChange(WaterField.TEMPERATURE, "-1")
        vm.onValueChange(WaterField.PH, "15")

        vm.onSave()

        val errors = vm.state.value.errors
        assertEquals(FormError.INVALID_NUMBER, errors[WaterField.OXYGEN])
        assertEquals(FormError.NEGATIVE, errors[WaterField.TEMPERATURE])
        assertEquals(FormError.PH_OUT_OF_SCALE, errors[WaterField.PH])
        assertTrue(farm.water.records.value.isEmpty())
        assertNull(vm.state.value.saved)
    }

    @Test
    fun `digitar apaga o erro do campo`() {
        val (vm, _) = viewModel()
        vm.onValueChange(WaterField.PH, "20")
        vm.onSave()
        assertNotNull(vm.state.value.errors[WaterField.PH])

        vm.onValueChange(WaterField.PH, "7")

        assertNull(vm.state.value.errors[WaterField.PH])
    }

    @Test
    fun `pH acima de 10 e oxigenio acima de 15 avisam mas deixam salvar`() {
        val (vm, farm) = viewModel()
        vm.onValueChange(WaterField.PH, "10,5")
        vm.onValueChange(WaterField.OXYGEN, "15,1")

        assertEquals(listOf(PlausibilityWarning.PH_VERY_HIGH, PlausibilityWarning.OXYGEN_SUPERSATURATED), vm.state.value.warnings)
        assertTrue(vm.state.value.canSave)
        vm.onSave()

        assertEquals(1, farm.water.records.value.size)
    }

    @Test
    fun `valores no limite nao avisam`() {
        val (vm, _) = viewModel()
        vm.onValueChange(WaterField.PH, "10")
        vm.onValueChange(WaterField.OXYGEN, "15")

        assertTrue(vm.state.value.warnings.isEmpty())
    }

    @Test
    fun `salva os valores, o momento, a data e hora e o dia nublado`() {
        val (vm, farm) = viewModel()
        vm.onValueChange(WaterField.OXYGEN, "5,5")
        vm.onValueChange(WaterField.PH, "7,2")
        vm.onValueChange(WaterField.TEMPERATURE, "28")
        vm.onValueChange(WaterField.TRANSPARENCY, "35")
        vm.onValueChange(WaterField.TOTAL_AMMONIA, "0,4")
        vm.onValueChange(WaterField.NITRITE, "0,02")
        vm.onValueChange(WaterField.ALKALINITY, "40")
        vm.onValueChange(WaterField.HARDNESS, "45")
        vm.onCloudyChange(true)
        vm.onMomentChange(MomentoMedicao.MANHA)

        vm.onSave()

        val saved = farm.water.records.value.single()
        assertEquals(5.5, saved.oxigenioMgL!!, 0.0)
        assertEquals(7.2, saved.ph!!, 0.0)
        assertEquals(28.0, saved.temperaturaC!!, 0.0)
        assertEquals(35.0, saved.transparenciaCm!!, 0.0)
        assertEquals(0.4, saved.amoniaTotalMgL!!, 0.0)
        assertEquals(0.02, saved.nitritoMgL!!, 0.0)
        assertEquals(40.0, saved.alcalinidadeMgL!!, 0.0)
        assertEquals(45.0, saved.durezaMgL!!, 0.0)
        assertEquals(MomentoMedicao.MANHA, saved.momento)
        assertTrue(saved.diaNublado)
        assertEquals(NOW, saved.dataHora)
        assertEquals(1L, saved.tanqueId)
    }

    @Test
    fun `campos em branco ficam sem valor, nao zero`() {
        val (vm, farm) = viewModel()
        vm.onValueChange(WaterField.OXYGEN, "5")
        vm.onValueChange(WaterField.PH, "  ")

        vm.onSave()

        assertNull(farm.water.records.value.single().ph)
    }

    @Test
    fun `o aviso de salvo depende da conexao`() {
        val (online, _) = viewModel()
        online.onValueChange(WaterField.OXYGEN, "5")
        online.onSave()
        assertEquals(SaveFeedback.SAVED, online.state.value.saved)

        connectivity.online = false
        val (offline, _) = viewModel()
        offline.onValueChange(WaterField.OXYGEN, "5")
        offline.onSave()
        assertEquals(SaveFeedback.SAVED_OFFLINE, offline.state.value.saved)
    }

    @Test
    fun `depois de salvo nao salva de novo`() {
        val (vm, farm) = viewModel()
        vm.onValueChange(WaterField.OXYGEN, "5")
        vm.onSave()

        assertFalse(vm.state.value.canSave)
        vm.onSave()

        assertEquals(1, farm.water.records.value.size)
    }

    // ---- resultado depois de salvar ----

    @Test
    fun `o resultado traz o nivel de cada valor informado`() {
        val (vm, _) = viewModel()
        vm.onValueChange(WaterField.OXYGEN, "2,5")
        vm.onValueChange(WaterField.TEMPERATURE, "28")

        vm.onSave()

        val levels = vm.state.value.result!!.alerts.associate { it.kind to it.status.level }
        assertEquals(mapOf(AlertKind.TEMPERATURE to AlertLevel.ADEQUADO, AlertKind.OXYGEN to AlertLevel.CRITICO), levels)
    }

    @Test
    fun `a amonia toxica e calculada com o pH e a temperatura do proprio registro`() {
        val (vm, _) = viewModel()
        vm.onValueChange(WaterField.TOTAL_AMMONIA, "1,5")
        vm.onValueChange(WaterField.PH, "9")
        vm.onValueChange(WaterField.TEMPERATURE, "30")

        vm.onSave()

        val toxic = vm.state.value.result!!.alerts.first { it.kind == AlertKind.TOXIC_AMMONIA }
        assertEquals(0.664, toxic.value, 0.005) // caso A2 de specs/09
        assertEquals(AlertLevel.EMERGENCIA, toxic.status.level)
        assertFalse(vm.state.value.result!!.ammoniaNeedsPhAndTemperature)
    }

    @Test
    fun `sem pH no registro usa o pH e a temperatura medidos antes e nao os repete no resultado`() {
        val farm = FarmFixture(
            tanks = listOf(tank(1, "Viveiro 1")),
            waterRecords = listOf(water(1, at = hoursAgo(2), ph = 7.0, temperatura = 30.0)),
        )
        val (vm, _) = viewModel(farm)
        vm.onValueChange(WaterField.TOTAL_AMMONIA, "1,5")

        vm.onSave()

        val result = vm.state.value.result!!
        assertEquals(listOf(AlertKind.TOXIC_AMMONIA), result.alerts.map { it.kind })
        assertEquals(0.012, result.alerts.single().value, 0.001) // caso A1 de specs/09
        assertFalse(result.ammoniaNeedsPhAndTemperature)
    }

    @Test
    fun `amonia sem pH e temperatura avisa em vez de avaliar`() {
        val (vm, _) = viewModel()
        vm.onValueChange(WaterField.TOTAL_AMMONIA, "1,5")

        vm.onSave()

        val result = vm.state.value.result!!
        assertTrue(result.alerts.isEmpty())
        assertTrue(result.ammoniaNeedsPhAndTemperature)
    }
}
