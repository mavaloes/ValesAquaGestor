package com.valoes.valesaquagestor.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.calc.DailyRoutine
import com.valoes.valesaquagestor.domain.calc.DailyRoutineBuilder
import com.valoes.valesaquagestor.domain.calc.MonitoringSchedule
import com.valoes.valesaquagestor.domain.usecase.ObserveFarmOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Estado da Home (rotina diária).
 *
 * @property hasTanks `false` se a fazenda ainda não tem nenhum tanque ativo.
 * @property routine alertas por prioridade (EMERGENCIA primeiro) e o que falta fazer hoje.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val hasTanks: Boolean = false,
    val routine: DailyRoutine = DailyRoutine(alerts = emptyList(), tasks = emptyList()),
)

/** Monta a rotina do dia a partir do retrato da fazenda e da agenda de medições. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeFarmOverview: ObserveFarmOverview,
    clock: Clock,
) : ViewModel() {

    // A agenda usa os padrões das fontes; a Fase 08 deixa o ADMIN editá-la.
    private val schedule = MonitoringSchedule()

    val state: StateFlow<HomeUiState> = observeFarmOverview()
        .map { farm ->
            HomeUiState(
                isLoading = false,
                hasTanks = farm.tanks.isNotEmpty(),
                routine = DailyRoutineBuilder.build(farm, schedule, clock),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
