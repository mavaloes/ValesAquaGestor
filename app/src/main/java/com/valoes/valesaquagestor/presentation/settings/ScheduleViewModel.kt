package com.valoes.valesaquagestor.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.calc.MonitoringSchedule
import com.valoes.valesaquagestor.domain.calc.ScheduleField
import com.valoes.valesaquagestor.domain.permission.Permission
import com.valoes.valesaquagestor.domain.permission.PermissionChecker
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.SettingsRepository
import com.valoes.valesaquagestor.domain.usecase.ManageSettings
import com.valoes.valesaquagestor.domain.usecase.UseCaseResult
import com.valoes.valesaquagestor.presentation.util.parseWholeNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Erro de um campo da tela da agenda. */
enum class ScheduleFieldError {
    /** Não é um número inteiro. */
    INVALID_NUMBER,

    /** Hora fora de 0 a 23, manhã depois da tarde, início depois do fim ou intervalo menor que 1 dia. */
    INVALID_VALUE,
}

/**
 * Estado da tela da agenda de medições. Os números ficam como texto enquanto o usuário digita.
 *
 * @property allowed `false` quando o perfil não pode editar (a tela mostra o aviso em vez dos campos).
 */
data class ScheduleUiState(
    val isLoading: Boolean = true,
    val allowed: Boolean = false,
    val values: Map<ScheduleField, String> = emptyMap(),
    val weeklyDay: DayOfWeek = MonitoringSchedule.WEEKLY_DAY,
    val errors: Map<ScheduleField, ScheduleFieldError> = emptyMap(),
    val isSaving: Boolean = false,
    val showRestoreConfirm: Boolean = false,
    val message: SettingsMessage? = null,
) {
    /** O botão Salvar só habilita com todos os campos preenchidos. */
    val canSave: Boolean
        get() = allowed && !isSaving && !isLoading && values.values.none { it.isBlank() }
}

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val manageSettings: ManageSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val papel = authRepository.currentSession.first()?.papel
            val schedule = settingsRepository.monitoringSchedule.first()
            _state.update {
                it.copy(
                    isLoading = false,
                    allowed = PermissionChecker.isAllowed(papel, Permission.MANAGE_SETTINGS),
                    values = schedule.toInputs(),
                    weeklyDay = schedule.weeklyDay,
                )
            }
        }
    }

    fun onValueChange(field: ScheduleField, value: String) =
        _state.update { it.copy(values = it.values + (field to value), errors = it.errors - field) }

    fun onWeeklyDayChange(day: DayOfWeek) = _state.update { it.copy(weeklyDay = day) }

    /** Valida e salva. Ignora o toque enquanto outro salvamento está em andamento. */
    fun onSave() {
        val current = _state.value
        if (!current.canSave) return

        val parsed = ScheduleField.entries.associateWith { parseWholeNumber(current.values[it].orEmpty()) }
        val numberErrors = parsed.filterValues { it == null }.mapValues { ScheduleFieldError.INVALID_NUMBER }
        if (numberErrors.isNotEmpty()) {
            _state.update { it.copy(errors = numberErrors) }
            return
        }
        val schedule = parsed.entries.fold(MonitoringSchedule(weeklyDay = current.weeklyDay)) { acc, (field, value) ->
            acc.with(field, checkNotNull(value))
        }
        val invalid = schedule.invalidFields().associateWith { ScheduleFieldError.INVALID_VALUE }
        if (invalid.isNotEmpty()) {
            _state.update { it.copy(errors = invalid) }
            return
        }

        _state.update { it.copy(isSaving = true, errors = emptyMap()) }
        viewModelScope.launch {
            val message = when (val result = manageSettings.saveMonitoringSchedule(schedule)) {
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
            val message = when (val result = manageSettings.resetMonitoringSchedule()) {
                is UseCaseResult.Success -> SettingsMessage.Restored
                is UseCaseResult.Failure -> SettingsMessage.Failed(result.error)
            }
            val schedule = settingsRepository.monitoringSchedule.first()
            _state.update {
                it.copy(
                    isSaving = false,
                    values = schedule.toInputs(),
                    weeklyDay = schedule.weeklyDay,
                    errors = emptyMap(),
                    message = message,
                )
            }
        }
    }

    fun onMessageShown() = _state.update { it.copy(message = null) }

    private fun MonitoringSchedule.toInputs(): Map<ScheduleField, String> =
        ScheduleField.entries.associateWith { valueOf(it).toString() }
}
