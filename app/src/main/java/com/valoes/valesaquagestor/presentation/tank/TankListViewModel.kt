package com.valoes.valesaquagestor.presentation.tank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.calc.AlertAggregator
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.TipoTanque
import com.valoes.valesaquagestor.domain.rules.AlertLevel
import com.valoes.valesaquagestor.domain.usecase.ObserveFarmOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Resumo de um lote ativo na lista de tanques. */
data class LotSummary(val especie: Especie, val quantity: Int)

/**
 * Um tanque na lista.
 *
 * @property worstLevel pior nível de alerta do tanque; `null` se ainda não tem medições.
 */
data class TankListItem(
    val tankId: Long,
    val nome: String,
    val tipo: TipoTanque,
    val lots: List<LotSummary>,
    val worstLevel: AlertLevel?,
)

data class TankListUiState(
    val isLoading: Boolean = true,
    val tanks: List<TankListItem> = emptyList(),
)

/** Lista os tanques ativos com o lote ativo e o pior nível de alerta de cada um. */
@HiltViewModel
class TankListViewModel @Inject constructor(
    observeFarmOverview: ObserveFarmOverview,
) : ViewModel() {

    val state: StateFlow<TankListUiState> = observeFarmOverview()
        .map { farm ->
            TankListUiState(
                isLoading = false,
                tanks = farm.tanks.map { tank ->
                    TankListItem(
                        tankId = tank.tanque.id,
                        nome = tank.tanque.nome,
                        tipo = tank.tanque.tipo,
                        lots = tank.lots.map { LotSummary(it.lote.especie, it.currentQuantity) },
                        worstLevel = AlertAggregator.overallLevel(tank, farm.waterRanges),
                    )
                },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TankListUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
