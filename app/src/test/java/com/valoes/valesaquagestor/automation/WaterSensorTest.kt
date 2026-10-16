package com.valoes.valesaquagestor.automation

import androidx.lifecycle.SavedStateHandle
import com.valoes.valesaquagestor.auth.FakeConnectivityChecker
import com.valoes.valesaquagestor.auth.MainDispatcherRule
import com.valoes.valesaquagestor.data.sensor.ManualWaterSensorSource
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.sensor.SensorReading
import com.valoes.valesaquagestor.farm.FIXED_CLOCK
import com.valoes.valesaquagestor.farm.FarmFixture
import com.valoes.valesaquagestor.farm.tank
import com.valoes.valesaquagestor.presentation.qr.TankQrViewModel
import com.valoes.valesaquagestor.presentation.record.WaterField
import com.valoes.valesaquagestor.presentation.record.WaterRecordViewModel
import java.time.Instant
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Interface de sensor (Fase 10): a leitura vira um rascunho editável, marcado como "vinda do sensor". */
class WaterSensorTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    /** 14/10/2026 06:30 (UTC, o fuso do relógio fixo de teste): bem antes do "agora" do relógio. */
    private val readAt = Instant.parse("2026-10-14T06:30:40Z")

    private val reading = SensorReading(readAt = readAt, oxigenioMgL = 5.5, ph = 7.25, temperaturaC = 27.0)

    private val farm = FarmFixture(tanks = listOf(tank(1, "Viveiro 1")))
    private val connectivity = FakeConnectivityChecker(online = true)

    private fun viewModel(sensor: ManualWaterSensorSource) = WaterRecordViewModel(
        SavedStateHandle(mapOf(WaterRecordViewModel.TANK_ID_ARG to 1L)),
        farm.water, farm.tanks, farm.settings, sensor, connectivity, FIXED_CLOCK,
    )

    // ---- A fonte manual ----

    @Test
    fun `a leitura entregue espera o formulario e sai da fila quando ele a pega`() = runBlocking {
        val source = ManualWaterSensorSource()
        source.submit(1, reading)

        assertEquals(reading, source.takePendingReading(1))
        assertNull(source.takePendingReading(1))
    }

    @Test
    fun `leitura de um tanque nao aparece em outro e leitura vazia e ignorada`() = runBlocking {
        val source = ManualWaterSensorSource()
        source.submit(1, reading)
        source.submit(2, SensorReading(readAt = readAt))

        assertNull(source.takePendingReading(2))
        assertNotNull(source.takePendingReading(1))
    }

    // ---- O formulário de água ----

    @Test
    fun `sem sensor o formulario abre vazio como sempre`() {
        val state = viewModel(ManualWaterSensorSource()).state.value

        assertTrue(state.values.isEmpty())
        assertTrue(state.fromSensor.isEmpty())
        assertFalse(state.canSave)
    }

    @Test
    fun `a leitura do sensor preenche o formulario e marca os campos`() {
        val source = ManualWaterSensorSource().apply { submit(1, reading) }

        val state = viewModel(source).state.value

        assertEquals("5,5", state.values[WaterField.OXYGEN])
        assertEquals("7,25", state.values[WaterField.PH])
        assertEquals("27", state.values[WaterField.TEMPERATURE])
        assertEquals(setOf(WaterField.OXYGEN, WaterField.PH, WaterField.TEMPERATURE), state.fromSensor)
        assertNull(state.values[WaterField.HARDNESS])
        // Data, hora e momento são os da medição, e continuam editáveis.
        assertEquals(LocalDateTime.of(2026, 10, 14, 6, 30), state.dateTime)
        assertEquals(MomentoMedicao.MANHA, state.moment)
        assertTrue(state.canSave)
    }

    @Test
    fun `o valor do sensor continua editavel e perde a marca quando o usuario muda`() {
        val vm = viewModel(ManualWaterSensorSource().apply { submit(1, reading) })

        vm.onValueChange(WaterField.OXYGEN, "4,8")

        val state = vm.state.value
        assertEquals("4,8", state.values[WaterField.OXYGEN])
        assertEquals(setOf(WaterField.PH, WaterField.TEMPERATURE), state.fromSensor)
    }

    @Test
    fun `salvar usa o valor que esta no formulario, inclusive o corrigido pelo usuario`() = runBlocking {
        val vm = viewModel(ManualWaterSensorSource().apply { submit(1, reading) })
        vm.onValueChange(WaterField.OXYGEN, "4,8")

        vm.onSave()

        val saved = farm.water.records.value.single()
        assertEquals(4.8, saved.oxigenioMgL!!, 0.0)
        assertEquals(7.25, saved.ph!!, 0.0)
        assertEquals(readAt.toEpochMilli() / 60_000 * 60_000, saved.dataHora.toEpochMilli())
    }

    @Test
    fun `a leitura preenche o formulario uma vez so`() {
        val source = ManualWaterSensorSource().apply { submit(1, reading) }

        viewModel(source)
        val second = viewModel(source).state.value

        assertTrue(second.values.isEmpty())
    }

    @Test
    fun `leitura de sensor com pH improvavel avisa como qualquer valor digitado`() {
        val vm = viewModel(ManualWaterSensorSource().apply { submit(1, SensorReading(readAt = readAt, ph = 11.0)) })

        assertEquals(1, vm.state.value.warnings.size)
    }

    // ---- QR Code do tanque ----

    @Test
    fun `a tela do QR Code traz o uuid do tanque`() {
        val vm = TankQrViewModel(SavedStateHandle(mapOf(TankQrViewModel.TANK_ID_ARG to 1L)), farm.tanks)

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals("Viveiro 1", state.tankName)
        assertEquals("t1", state.content)
    }

    @Test
    fun `tanque que nao existe nao tem QR Code`() {
        val vm = TankQrViewModel(SavedStateHandle(mapOf(TankQrViewModel.TANK_ID_ARG to 99L)), farm.tanks)

        assertNull(vm.state.value.content)
        assertFalse(vm.state.value.isLoading)
    }
}
