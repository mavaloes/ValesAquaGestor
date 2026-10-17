package com.valoes.valesaquagestor.automation

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.valoes.valesaquagestor.auth.MainDispatcherRule
import com.valoes.valesaquagestor.data.qr.QrEncoder
import com.valoes.valesaquagestor.domain.qr.TankQrCode
import com.valoes.valesaquagestor.domain.usecase.QrResolution
import com.valoes.valesaquagestor.domain.usecase.ResolveTankQr
import com.valoes.valesaquagestor.farm.FakeTankRepository
import com.valoes.valesaquagestor.farm.tank
import com.valoes.valesaquagestor.presentation.qr.ScanProblem
import com.valoes.valesaquagestor.presentation.qr.ScanQrViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** QR Code dos tanques (Fase 10): conteúdo, desenho, leitura válida e inválida, câmera negada. */
class QrCodeTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val uuid = "2c1f3a44-7b8e-4d27-9a31-5c0e8d6b1f90"

    // ---- Conteúdo ----

    @Test
    fun `o QR Code contem exatamente o uuid do tanque`() {
        assertEquals(uuid, TankQrCode.encode(uuid))
        assertEquals(uuid, TankQrCode.encode("  ${uuid.uppercase()} "))
    }

    @Test
    fun `um uuid lido volta como o uuid do tanque, mesmo com espacos e maiusculas`() {
        assertEquals(uuid, TankQrCode.parse(uuid))
        assertEquals(uuid, TankQrCode.parse(" ${uuid.uppercase()}\n"))
    }

    @Test
    fun `texto que nao e uuid e invalido`() {
        listOf(null, "", "   ", "tanque-1", "https://exemplo.com/2c1f3a44", uuid + "x", uuid.replace("-", ""), "1234567890128")
            .forEach { assertNull("deveria ser inválido: $it", TankQrCode.parse(it)) }
    }

    // ---- Desenho ----

    @Test
    fun `o QR Code desenhado pode ser lido de volta e traz o uuid`() {
        val size = 400
        val matrix = QrEncoder.encode(TankQrCode.encode(uuid), size)
        val pixels = IntArray(size * size) { index ->
            if (matrix[index % size, index / size]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }

        val decoded = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(size, size, pixels))))

        assertEquals(uuid, decoded.text)
    }

    // ---- Resolver o código lido ----

    private fun resolver() = ResolveTankQr(FakeTankRepository(listOf(tank(id = 7).copy(uuid = uuid))))

    @Test
    fun `codigo de um tanque do aparelho abre esse tanque`() = runBlocking {
        assertEquals(QrResolution.Found(7), resolver()(uuid))
    }

    @Test
    fun `codigo que nao e de tanque e invalido`() = runBlocking {
        assertEquals(QrResolution.InvalidCode, resolver()("https://exemplo.com"))
        assertEquals(QrResolution.InvalidCode, resolver()(null))
    }

    @Test
    fun `codigo de tanque que nao existe neste aparelho e tanque desconhecido`() = runBlocking {
        assertEquals(QrResolution.UnknownTank, resolver()("11111111-2222-3333-4444-555555555555"))
    }

    // ---- Tela de leitura ----

    private fun scanViewModel() = ScanQrViewModel(resolver())

    @Test
    fun `ler um QR Code valido manda abrir o tanque`() {
        val vm = scanViewModel()

        vm.onScanResult(uuid)

        assertEquals(7L, vm.state.value.openTankId)
        assertNull(vm.state.value.problem)
        vm.onTankOpened()
        assertNull(vm.state.value.openTankId)
    }

    @Test
    fun `QR Code invalido mostra o problema e nao abre nada`() {
        val vm = scanViewModel()

        vm.onScanResult("qualquer coisa")

        assertEquals(ScanProblem.INVALID_CODE, vm.state.value.problem)
        assertNull(vm.state.value.openTankId)
    }

    @Test
    fun `QR Code de tanque desconhecido mostra o problema`() {
        val vm = scanViewModel()

        vm.onScanResult("11111111-2222-3333-4444-555555555555")

        assertEquals(ScanProblem.UNKNOWN_TANK, vm.state.value.problem)
    }

    @Test
    fun `camera negada e aparelho sem camera mostram o motivo`() {
        val denied = scanViewModel()
        denied.onCameraPermissionResult(granted = false)
        assertEquals(ScanProblem.CAMERA_DENIED, denied.state.value.problem)

        val none = scanViewModel()
        none.onNoCamera()
        assertEquals(ScanProblem.NO_CAMERA, none.state.value.problem)
    }

    @Test
    fun `permissao concedida nao mostra problema e fechar a camera limpa o aviso`() {
        val vm = scanViewModel()
        vm.onScanResult("lixo")
        assertTrue(vm.state.value.problem != null)

        vm.onCameraPermissionResult(granted = true)
        assertTrue(vm.state.value.problem != null)

        vm.onScanResult(null) // usuário fechou a câmera
        assertNull(vm.state.value.problem)
    }
}
