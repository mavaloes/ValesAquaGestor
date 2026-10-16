package com.valoes.valesaquagestor.presentation.tank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.model.Tanque
import com.valoes.valesaquagestor.domain.model.TipoTanque
import com.valoes.valesaquagestor.domain.repository.TankRepository
import com.valoes.valesaquagestor.presentation.util.FormError
import com.valoes.valesaquagestor.presentation.util.FormValidation
import com.valoes.valesaquagestor.presentation.util.parseDecimal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado do formulário de novo tanque. Os números ficam como texto enquanto o usuário digita.
 * Viveiro escavado e açude pedem a **área** (a capacidade é por área); os outros tipos pedem o **volume**.
 */
data class TankFormUiState(
    val name: String = "",
    val type: TipoTanque = TipoTanque.ESCAVADO,
    val area: String = "",
    val depth: String = "",
    val volume: String = "",
    val hasAerator: Boolean = false,
    val aeratorPower: String = "",
    val renewal: String = "",
    val nameError: FormError? = null,
    val areaError: FormError? = null,
    val depthError: FormError? = null,
    val volumeError: FormError? = null,
    val aeratorPowerError: FormError? = null,
    val renewalError: FormError? = null,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val savedTankId: Long? = null,
) {
    /** Viveiro e açude: a capacidade é calculada pela área. */
    val usesArea: Boolean get() = type == TipoTanque.ESCAVADO || type == TipoTanque.ACUDE

    /** O botão Salvar só habilita com todos os obrigatórios preenchidos. */
    val canSave: Boolean
        get() = !isSaving && name.isNotBlank() && renewal.isNotBlank() &&
            (if (usesArea) area.isNotBlank() else volume.isNotBlank())
}

@HiltViewModel
class TankFormViewModel @Inject constructor(
    private val tankRepository: TankRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TankFormUiState())
    val state: StateFlow<TankFormUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = null) }

    fun onTypeChange(value: TipoTanque) =
        _state.update { it.copy(type = value, areaError = null, volumeError = null) }

    fun onAreaChange(value: String) = _state.update { it.copy(area = value, areaError = null) }

    fun onDepthChange(value: String) = _state.update { it.copy(depth = value, depthError = null) }

    fun onVolumeChange(value: String) = _state.update { it.copy(volume = value, volumeError = null) }

    fun onHasAeratorChange(value: Boolean) = _state.update { it.copy(hasAerator = value, aeratorPowerError = null) }

    fun onAeratorPowerChange(value: String) = _state.update { it.copy(aeratorPower = value, aeratorPowerError = null) }

    fun onRenewalChange(value: String) = _state.update { it.copy(renewal = value, renewalError = null) }

    fun onSaveFailedShown() = _state.update { it.copy(saveFailed = false) }

    /** Valida e salva. Ignora o toque enquanto outro salvamento está em andamento. */
    fun onSave() {
        val current = _state.value
        if (current.isSaving) return

        val nameError = FormValidation.requiredText(current.name)
        val areaError = FormValidation.positiveDecimal(current.area, required = current.usesArea)
        val depthError = FormValidation.positiveDecimal(current.depth, required = false)
        val volumeError = FormValidation.positiveDecimal(current.volume, required = !current.usesArea)
        val powerError = if (current.hasAerator) FormValidation.positiveDecimal(current.aeratorPower, required = false) else null
        val renewalError = FormValidation.percent(current.renewal)
        if (listOf(nameError, areaError, depthError, volumeError, powerError, renewalError).any { it != null }) {
            _state.update {
                it.copy(
                    nameError = nameError, areaError = areaError, depthError = depthError,
                    volumeError = volumeError, aeratorPowerError = powerError, renewalError = renewalError,
                )
            }
            return
        }

        val area = parseDecimal(current.area)
        val depth = parseDecimal(current.depth)
        // Com área e profundidade, o volume é a conta das duas (specs/02-dominio.md).
        val volume = if (area != null && depth != null) area * depth else parseDecimal(current.volume)
        val tank = Tanque(
            nome = current.name.trim(),
            tipo = current.type,
            areaM2 = area,
            profundidadeMediaM = depth,
            volumeM3 = volume,
            usaAerador = current.hasAerator,
            aeradorPotenciaHp = if (current.hasAerator) parseDecimal(current.aeratorPower) else null,
            renovacaoAguaPctDia = checkNotNull(parseDecimal(current.renewal)),
        )

        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching { tankRepository.save(tank) }
                .onSuccess { id -> _state.update { it.copy(isSaving = false, savedTankId = id) } }
                .onFailure { _state.update { it.copy(isSaving = false, saveFailed = true) } }
        }
    }
}
