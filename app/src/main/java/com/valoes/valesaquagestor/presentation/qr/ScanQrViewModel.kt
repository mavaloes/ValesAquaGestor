package com.valoes.valesaquagestor.presentation.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.usecase.QrResolution
import com.valoes.valesaquagestor.domain.usecase.ResolveTankQr
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Por que a leitura do QR Code não abriu um tanque. A tela converte em texto de `strings.xml`. */
enum class ScanProblem {
    /** O usuário não deu permissão à câmera. */
    CAMERA_DENIED,

    /** O aparelho não tem câmera. */
    NO_CAMERA,

    /** A câmera leu algo que não é QR Code de tanque. */
    INVALID_CODE,

    /** É QR Code de tanque, mas este aparelho não tem esse tanque. */
    UNKNOWN_TANK,
}

/**
 * Estado da leitura do QR Code.
 *
 * @property problem o que deu errado, ou `null` (nada lido ainda, ou o usuário fechou a câmera).
 * @property openTankId preenchido quando o código é de um tanque deste aparelho: a tela abre o `TankDetail`.
 */
data class ScanQrUiState(
    val problem: ScanProblem? = null,
    val openTankId: Long? = null,
)

/**
 * Recebe o resultado da permissão e da câmera e decide o que mostrar. Toda saída ruim tem mensagem
 * clara e a opção de escolher o tanque na lista (nunca um erro técnico nem uma tela travada).
 */
@HiltViewModel
class ScanQrViewModel @Inject constructor(
    private val resolveTankQr: ResolveTankQr,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanQrUiState())
    val state: StateFlow<ScanQrUiState> = _state.asStateFlow()

    fun onCameraPermissionResult(granted: Boolean) {
        if (!granted) _state.update { it.copy(problem = ScanProblem.CAMERA_DENIED) }
    }

    fun onNoCamera() = _state.update { it.copy(problem = ScanProblem.NO_CAMERA) }

    /** [text] é o que a câmera leu; `null` quando o usuário fechou a câmera sem ler nada. */
    fun onScanResult(text: String?) {
        if (text == null) {
            _state.update { it.copy(problem = null) }
            return
        }
        viewModelScope.launch {
            _state.update {
                when (val resolution = resolveTankQr(text)) {
                    is QrResolution.Found -> it.copy(problem = null, openTankId = resolution.tankId)
                    QrResolution.InvalidCode -> it.copy(problem = ScanProblem.INVALID_CODE)
                    QrResolution.UnknownTank -> it.copy(problem = ScanProblem.UNKNOWN_TANK)
                }
            }
        }
    }

    /** A tela já abriu o tanque. */
    fun onTankOpened() = _state.update { it.copy(openTankId = null) }

    /** Vai tentar de novo: limpa o aviso anterior. */
    fun onRetry() = _state.update { it.copy(problem = null) }
}
