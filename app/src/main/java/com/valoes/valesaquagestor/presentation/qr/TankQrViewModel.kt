package com.valoes.valesaquagestor.presentation.qr

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.qr.TankQrCode
import com.valoes.valesaquagestor.domain.repository.TankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado da tela do QR Code de um tanque.
 *
 * @property content texto que vai dentro do QR Code (o `uuid` do tanque); `null` enquanto carrega ou
 *   se o tanque não existe.
 */
data class TankQrUiState(
    val isLoading: Boolean = true,
    val tankName: String = "",
    val content: String? = null,
)

@HiltViewModel
class TankQrViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tanks: TankRepository,
) : ViewModel() {

    private val tankId: Long = checkNotNull(savedStateHandle[TANK_ID_ARG]) { "Falta o id do tanque." }

    private val _state = MutableStateFlow(TankQrUiState())
    val state: StateFlow<TankQrUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val tank = tanks.get(tankId)
            _state.update {
                TankQrUiState(
                    isLoading = false,
                    tankName = tank?.nome.orEmpty(),
                    content = tank?.uuid?.takeIf { uuid -> uuid.isNotBlank() }?.let(TankQrCode::encode),
                )
            }
        }
    }

    companion object {
        /** Nome do argumento da rota `TankQr(tankId)`. */
        const val TANK_ID_ARG = "tankId"
    }
}
