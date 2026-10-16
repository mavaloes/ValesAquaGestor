package com.valoes.valesaquagestor.presentation.lot

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.calc.LotCalculator
import com.valoes.valesaquagestor.domain.model.Arracoamento
import com.valoes.valesaquagestor.domain.model.Biometria
import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import com.valoes.valesaquagestor.domain.repository.BiometryRepository
import com.valoes.valesaquagestor.domain.repository.FeedingRepository
import com.valoes.valesaquagestor.domain.repository.LotMovementRepository
import com.valoes.valesaquagestor.domain.repository.LotRepository
import com.valoes.valesaquagestor.domain.repository.TankRepository
import com.valoes.valesaquagestor.domain.repository.WaterQualityRepository
import com.valoes.valesaquagestor.domain.rules.AlertItem
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.AlertLevel
import com.valoes.valesaquagestor.domain.rules.CompositeAlerts
import com.valoes.valesaquagestor.domain.rules.WaterReadingsEvaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * O que a tela do lote mostra. Tudo é calculado na hora, nada é gravado.
 *
 * @property fcr TCA estimada entre as duas últimas biometrias (ou o povoamento e a biometria);
 *   `null` se o lote não tem biometria ou ração no período.
 * @property alerts mortalidade acima de 10% e alertas da água do tanque, do mais grave ao menos grave.
 * @property recentFeedings últimos tratos, do mais novo ao mais antigo.
 * @property recentBiometries últimas biometrias, da mais nova à mais antiga.
 * @property recentMovements últimas mortalidades, despescas e transferências, da mais nova à mais antiga.
 */
data class LotDetailData(
    val lote: Lote,
    val tankName: String,
    val currentQuantity: Int,
    val biomassKg: Double,
    val mortalityPct: Double,
    val fcr: Double?,
    val hasBiometry: Boolean,
    val alerts: List<AlertItem>,
    val recentFeedings: List<Arracoamento> = emptyList(),
    val recentBiometries: List<Biometria> = emptyList(),
    val recentMovements: List<MovimentacaoLote> = emptyList(),
)

/** [lot] fica `null` se o lote não existe ou foi excluído. */
data class LotDetailUiState(
    val isLoading: Boolean = true,
    val lot: LotDetailData? = null,
)

@HiltViewModel
class LotDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    lots: LotRepository,
    tanks: TankRepository,
    water: WaterQualityRepository,
    private val movements: LotMovementRepository,
    private val biometry: BiometryRepository,
    private val feeding: FeedingRepository,
) : ViewModel() {

    private val lotId: Long = checkNotNull(savedStateHandle[LOT_ID_ARG]) { "Falta o id do lote." }

    private data class Recents(
        val feedings: List<Arracoamento>,
        val biometries: List<Biometria>,
        val movements: List<MovimentacaoLote>,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<LotDetailUiState> = lots.observe(lotId)
        .flatMapLatest { lote ->
            if (lote == null) {
                flowOf(LotDetailUiState(isLoading = false))
            } else {
                val latestBiometries = biometry.observeLatest(lotId, LATEST_BIOMETRIES)
                val feedingsInWindow: Flow<List<Arracoamento>> = latestBiometries.flatMapLatest { biometries ->
                    val window = LotCalculator.fcrWindow(lote, biometries)
                    // O período da TCA é (início, fim]; a consulta é [início, fim), então soma 1 ms ao fim.
                    if (window == null) flowOf(emptyList()) else feeding.observeByLot(lotId, window.first, window.second.plusMillis(1))
                }
                val calculation = combine(movements.observeByLot(lotId), latestBiometries, feedingsInWindow) { m, b, f ->
                    Triple(m, b, f)
                }
                val context = combine(
                    tanks.observe(lote.tanqueId),
                    water.observeLatestReadings(lote.tanqueId),
                    combine(feeding.observeRecent(lotId, RECENT_RECORDS), biometry.observeLatest(lotId, RECENT_RECORDS), movements.observeByLot(lotId)) { f, b, m ->
                        Recents(f, b, m.takeLast(RECENT_RECORDS).reversed())
                    },
                ) { tank, readings, recents -> Triple(tank, readings, recents) }

                combine(calculation, context) { (lotMovements, biometries, feedings), (tank, readings, recents) ->
                    val tankName = tank?.nome.orEmpty()
                    val mortality = LotCalculator.accumulatedMortalityPct(lote, lotMovements)
                    val mortalityAlert = CompositeAlerts
                        .accumulatedMortality(lote.quantidadeInicial, LotCalculator.deadCount(lotMovements))
                        ?.let { AlertItem(lote.tanqueId, tankName, lote.id, AlertKind.MORTALITY, mortality, it, null) }
                    val waterAlerts = WaterReadingsEvaluator.evaluate(readings).alerts
                        .filter { it.status.level > AlertLevel.ADEQUADO }
                        .map { AlertItem(lote.tanqueId, tankName, null, it.kind, it.value, it.status, it.measuredAt) }
                    LotDetailUiState(
                        isLoading = false,
                        lot = LotDetailData(
                            lote = lote,
                            tankName = tankName,
                            currentQuantity = LotCalculator.currentQuantity(lote, lotMovements),
                            biomassKg = LotCalculator.biomassKg(lote, lotMovements),
                            mortalityPct = mortality,
                            fcr = LotCalculator.estimatedFcr(lote, lotMovements, biometries, feedings),
                            hasBiometry = biometries.isNotEmpty(),
                            alerts = (listOfNotNull(mortalityAlert) + waterAlerts).sortedByDescending { it.status.level },
                            recentFeedings = recents.feedings,
                            recentBiometries = recents.biometries,
                            recentMovements = recents.movements,
                        ),
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LotDetailUiState())

    /** Exclusão lógica de um trato (a tela já pediu a confirmação). */
    fun deleteFeeding(id: Long) {
        viewModelScope.launch { feeding.delete(id) }
    }

    /** Exclusão lógica de uma biometria. O peso médio do lote não volta ao valor anterior. */
    fun deleteBiometry(id: Long) {
        viewModelScope.launch { biometry.delete(id) }
    }

    /** Exclusão lógica de uma mortalidade ou despesca. Uma despesca final excluída não reabre o lote. */
    fun deleteMovement(id: Long) {
        viewModelScope.launch { movements.delete(id) }
    }

    companion object {
        /** Nome do argumento da rota `LotDetail(id)`. */
        const val LOT_ID_ARG = "id"
        private const val LATEST_BIOMETRIES = 2
        private const val RECENT_RECORDS = 5
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
