package com.valoes.valesaquagestor.presentation.tank

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.calc.AlertAggregator
import com.valoes.valesaquagestor.domain.model.QualidadeAgua
import com.valoes.valesaquagestor.domain.model.Tanque
import com.valoes.valesaquagestor.domain.repository.WaterQualityRepository
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.WaterAlert
import com.valoes.valesaquagestor.domain.rules.WaterReadingsEvaluator
import com.valoes.valesaquagestor.domain.usecase.LotOverview
import com.valoes.valesaquagestor.domain.usecase.ObserveFarmOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * O que a tela do tanque mostra.
 *
 * @property waterAlerts última leitura de cada parâmetro, já avaliada (inclusive as ADEQUADO), mais a
 *   variação do pH de hoje quando houver alerta.
 * @property ammoniaNeedsPhAndTemperature `true` se falta pH ou temperatura para calcular a amônia tóxica.
 * @property lots lotes ativos do tanque.
 * @property recentRecords últimas medições registradas, da mais nova à mais antiga.
 */
data class TankDetailData(
    val tanque: Tanque,
    val waterAlerts: List<WaterAlert>,
    val ammoniaNeedsPhAndTemperature: Boolean,
    val lots: List<LotOverview>,
    val recentRecords: List<QualidadeAgua> = emptyList(),
)

/** [tank] fica `null` se o tanque não existe, foi excluído ou está inativo. */
data class TankDetailUiState(
    val isLoading: Boolean = true,
    val tank: TankDetailData? = null,
)

@HiltViewModel
class TankDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeFarmOverview: ObserveFarmOverview,
    private val water: WaterQualityRepository,
) : ViewModel() {

    private val tankId: Long = checkNotNull(savedStateHandle[TANK_ID_ARG]) { "Falta o id do tanque." }

    val state: StateFlow<TankDetailUiState> = combine(observeFarmOverview(), water.observeRecent(tankId, RECENT_RECORDS)) { farm, recent ->
            val overview = farm.tanks.firstOrNull { it.tanque.id == tankId }
            val data = overview?.let {
                val report = WaterReadingsEvaluator.evaluate(it.readings)
                val phVariation = AlertAggregator.alertsFor(it)
                    .firstOrNull { alert -> alert.kind == AlertKind.PH_VARIATION }
                    ?.let { alert ->
                        WaterAlert(alert.kind, checkNotNull(alert.value), alert.status, it.waterToday.last().dataHora)
                    }
                TankDetailData(
                    tanque = it.tanque,
                    waterAlerts = report.alerts + listOfNotNull(phVariation),
                    ammoniaNeedsPhAndTemperature = report.ammoniaNeedsPhAndTemperature,
                    lots = it.lots,
                    recentRecords = recent,
                )
            }
            TankDetailUiState(isLoading = false, tank = data)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TankDetailUiState())

    /** Exclusão lógica de uma medição de água (a tela já pediu a confirmação). */
    fun deleteWaterRecord(id: Long) {
        viewModelScope.launch { water.delete(id) }
    }

    companion object {
        /** Nome do argumento da rota `TankDetail(id)`. */
        const val TANK_ID_ARG = "id"
        private const val RECENT_RECORDS = 5
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
