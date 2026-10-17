package com.valoes.valesaquagestor.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.calc.DailyRoutine
import com.valoes.valesaquagestor.domain.calc.DailyRoutineBuilder
import com.valoes.valesaquagestor.domain.model.Atividade
import com.valoes.valesaquagestor.domain.repository.ActivityRepository
import com.valoes.valesaquagestor.domain.usecase.ObserveFarmOverview
import com.valoes.valesaquagestor.domain.usecase.SetupSuggestion
import com.valoes.valesaquagestor.domain.usecase.resolveSetupSuggestion
import com.valoes.valesaquagestor.presentation.main.RegisterTank
import com.valoes.valesaquagestor.presentation.main.toRegisterTanks
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Estado da Home (rotina diária).
 *
 * @property hasTanks `false` se a fazenda ainda não tem nenhum tanque ativo.
 * @property routine alertas por prioridade (EMERGENCIA primeiro) e o que falta fazer hoje.
 * @property setup atalho para retomar a configuração da fazenda (primeiro tanque, primeiro lote).
 * @property tanks tanques e lotes ativos, para os botões de atalho escolherem onde registrar.
 * @property todayActivities atividades de hoje que ainda não foram feitas (as com hora primeiro); aparecem
 * no quadro de avisos.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val hasTanks: Boolean = false,
    val routine: DailyRoutine = DailyRoutine(alerts = emptyList(), tasks = emptyList()),
    val setup: SetupSuggestion = SetupSuggestion.None,
    val tanks: List<RegisterTank> = emptyList(),
    val todayActivities: List<Atividade> = emptyList(),
)

/** Monta a rotina do dia a partir do retrato da fazenda e da agenda de medições (padrão ou ajustada pelo ADMIN). */
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeFarmOverview: ObserveFarmOverview,
    clock: Clock,
    activities: ActivityRepository,
) : ViewModel() {

    // O relógio do app é UTC; o "hoje" das atividades é o do fuso do aparelho.
    private val today: LocalDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()))

    val state: StateFlow<HomeUiState> = combine(
        observeFarmOverview(),
        activities.observeBetween(today, today),
    ) { farm, todayActivities ->
        HomeUiState(
            isLoading = false,
            hasTanks = farm.tanks.isNotEmpty(),
            routine = DailyRoutineBuilder.build(farm, farm.schedule, clock),
            setup = resolveSetupSuggestion(farm),
            tanks = farm.toRegisterTanks(),
            todayActivities = todayActivities.filterNot { it.concluida },
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
