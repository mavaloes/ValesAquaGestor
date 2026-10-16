package com.valoes.valesaquagestor.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.permission.Permission
import com.valoes.valesaquagestor.domain.permission.PermissionChecker
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.SettingsRepository
import com.valoes.valesaquagestor.domain.rules.WaterRangeField
import com.valoes.valesaquagestor.domain.rules.WaterRanges
import com.valoes.valesaquagestor.domain.usecase.ManageSettings
import com.valoes.valesaquagestor.domain.usecase.UseCaseError
import com.valoes.valesaquagestor.domain.usecase.UseCaseResult
import com.valoes.valesaquagestor.presentation.util.formatInput
import com.valoes.valesaquagestor.presentation.util.parseDecimal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Erro de um campo da tela de faixas. */
enum class RangeFieldError {
    /** Não é um número. */
    INVALID_NUMBER,

    /** Fora de ordem, não positivo ou, no pH, fora da escala de 0 a 14. */
    ORDER,
}

/** Aviso que a tela de configurações mostra depois de uma ação. */
sealed interface SettingsMessage {
    data object Saved : SettingsMessage
    data object Restored : SettingsMessage
    data class Failed(val error: UseCaseError) : SettingsMessage
}

/**
 * Estado da tela de faixas da água. Os números ficam como texto enquanto o usuário digita.
 *
 * @property allowed `false` quando o perfil não pode editar (a tela mostra o aviso em vez dos campos).
 */
data class WaterRangesUiState(
    val isLoading: Boolean = true,
    val allowed: Boolean = false,
    val values: Map<WaterRangeField, String> = emptyMap(),
    val errors: Map<WaterRangeField, RangeFieldError> = emptyMap(),
    val isSaving: Boolean = false,
    val showRestoreConfirm: Boolean = false,
    val message: SettingsMessage? = null,
) {
    /** O botão Salvar só habilita com todos os campos preenchidos. */
    val canSave: Boolean
        get() = allowed && !isSaving && !isLoading && values.values.none { it.isBlank() }
}

@HiltViewModel
class WaterRangesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val manageSettings: ManageSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(WaterRangesUiState())
    val state: StateFlow<WaterRangesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val papel = authRepository.currentSession.first()?.papel
            val ranges = settingsRepository.waterRanges.first()
            _state.update {
                it.copy(
                    isLoading = false,
                    allowed = PermissionChecker.isAllowed(papel, Permission.MANAGE_SETTINGS),
                    values = ranges.toInputs(),
                )
            }
        }
    }

    fun onValueChange(field: WaterRangeField, value: String) =
        _state.update { it.copy(values = it.values + (field to value), errors = it.errors - field) }

    /** Valida e salva. Ignora o toque enquanto outro salvamento está em andamento. */
    fun onSave() {
        val current = _state.value
        if (!current.canSave) return

        val parsed = WaterRangeField.entries.associateWith { parseDecimal(current.values[it].orEmpty()) }
        val numberErrors = parsed.filterValues { it == null }.mapValues { RangeFieldError.INVALID_NUMBER }
        if (numberErrors.isNotEmpty()) {
            _state.update { it.copy(errors = numberErrors) }
            return
        }
        val ranges = parsed.entries.fold(WaterRanges.DEFAULT) { acc, (field, value) -> acc.with(field, checkNotNull(value)) }
        val orderErrors = ranges.invalidFields().associateWith { RangeFieldError.ORDER }
        if (orderErrors.isNotEmpty()) {
            _state.update { it.copy(errors = orderErrors) }
            return
        }

        _state.update { it.copy(isSaving = true, errors = emptyMap()) }
        viewModelScope.launch {
            val message = when (val result = manageSettings.saveWaterRanges(ranges)) {
                is UseCaseResult.Success -> SettingsMessage.Saved
                is UseCaseResult.Failure -> SettingsMessage.Failed(result.error)
            }
            _state.update { it.copy(isSaving = false, message = message) }
        }
    }

    fun onRestoreClick() = _state.update { it.copy(showRestoreConfirm = true) }

    fun onRestoreDismiss() = _state.update { it.copy(showRestoreConfirm = false) }

    /** Apaga os ajustes e volta os campos aos valores de fábrica. */
    fun onRestoreConfirm() {
        _state.update { it.copy(showRestoreConfirm = false, isSaving = true) }
        viewModelScope.launch {
            val message = when (val result = manageSettings.resetWaterRanges()) {
                is UseCaseResult.Success -> SettingsMessage.Restored
                is UseCaseResult.Failure -> SettingsMessage.Failed(result.error)
            }
            val ranges = settingsRepository.waterRanges.first()
            _state.update {
                it.copy(isSaving = false, values = ranges.toInputs(), errors = emptyMap(), message = message)
            }
        }
    }

    fun onMessageShown() = _state.update { it.copy(message = null) }

    private fun WaterRanges.toInputs(): Map<WaterRangeField, String> =
        WaterRangeField.entries.associateWith { formatInput(valueOf(it), INPUT_DECIMALS) }

    private companion object {
        /** Casas decimais ao mostrar um limite (os menores, como o nitrito, têm duas casas). */
        const val INPUT_DECIMALS = 4
    }
}
