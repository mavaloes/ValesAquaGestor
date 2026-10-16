package com.valoes.valesaquagestor.presentation.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.calc.FeedBlockReason
import com.valoes.valesaquagestor.domain.calc.FeedSuggestion
import com.valoes.valesaquagestor.domain.calc.FeedingCalculator
import com.valoes.valesaquagestor.domain.calc.LotCalculator
import com.valoes.valesaquagestor.domain.calc.WaterReadings
import com.valoes.valesaquagestor.domain.model.Arracoamento
import com.valoes.valesaquagestor.domain.model.ComportamentoPeixe
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.LatestWaterReadings
import com.valoes.valesaquagestor.domain.model.MotivoSuspensao
import com.valoes.valesaquagestor.domain.repository.ConnectivityChecker
import com.valoes.valesaquagestor.domain.repository.FeedingRepository
import com.valoes.valesaquagestor.domain.repository.LotMovementRepository
import com.valoes.valesaquagestor.domain.repository.LotRepository
import com.valoes.valesaquagestor.domain.repository.SettingsRepository
import com.valoes.valesaquagestor.domain.repository.TankRepository
import com.valoes.valesaquagestor.domain.repository.WaterQualityRepository
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.WaterReadingsEvaluator
import com.valoes.valesaquagestor.presentation.util.FormError
import com.valoes.valesaquagestor.presentation.util.FormValidation
import com.valoes.valesaquagestor.presentation.util.SaveFeedback
import com.valoes.valesaquagestor.presentation.util.formatInput
import com.valoes.valesaquagestor.presentation.util.parseDecimal
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado do registro de ração. A quantidade já vem **sugerida** pela tabela (por refeição) e pode
 * ser editada. Marca, proteína e pellet vêm da última ração do lote.
 *
 * Com água fora dos limites para alimentar, a sugestão é 0 kg e a ração já vem marcada como
 * suspensa, com o motivo; o usuário pode desmarcar (a trava nunca bloqueia a tela).
 *
 * @property especie espécie do lote; `null` até carregar.
 * @property suggestion sugestão da tabela; `null` se o lote não tem tabela (outra espécie ou peso fora da tabela).
 * @property lastHadLeftover a última refeição teve sobra: a tela pede para reduzir esta.
 */
data class FeedingUiState(
    val loaded: Boolean = false,
    val especie: Especie? = null,
    val tankName: String = "",
    val suggestion: FeedSuggestion? = null,
    val lastHadLeftover: Boolean = false,
    val quantity: String = "",
    val dateTime: LocalDateTime,
    val suspended: Boolean = false,
    val reason: MotivoSuspensao? = null,
    val brand: String = "",
    val protein: String = "",
    val pellet: String = "",
    val behavior: ComportamentoPeixe = ComportamentoPeixe.NORMAL,
    val leftover: Boolean = false,
    val quantityError: FormError? = null,
    val reasonError: FormError? = null,
    val proteinError: FormError? = null,
    val pelletError: FormError? = null,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: SaveFeedback? = null,
) {
    /** O botão Salvar habilita com a quantidade preenchida (ou com a ração suspensa). */
    val canSave: Boolean get() = loaded && !isSaving && saved == null && (suspended || quantity.isNotBlank())

    /** Não há tabela para este lote: o usuário informa a quantidade. */
    val noTable: Boolean get() = loaded && suggestion == null
}

@HiltViewModel
class FeedingRecordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val lots: LotRepository,
    tanks: TankRepository,
    movements: LotMovementRepository,
    water: WaterQualityRepository,
    settings: SettingsRepository,
    private val feeding: FeedingRepository,
    private val connectivity: ConnectivityChecker,
    private val clock: Clock,
) : ViewModel() {

    private val lotId: Long = checkNotNull(savedStateHandle[LOT_ID_ARG]) { "Falta o id do lote." }

    private val _state = MutableStateFlow(
        FeedingUiState(dateTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES)),
    )
    val state: StateFlow<FeedingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val lote = lots.get(lotId) ?: return@launch
            val alive = LotCalculator.currentQuantity(lote, movements.observeByLot(lotId).first())
            val readings = water.observeLatestReadings(lote.tanqueId).first()
            val suggestion = if (lote.especie == Especie.TILAPIA) {
                FeedingCalculator.suggest(alive, lote.pesoMedioAtualG, feedingReadings(readings), settings.waterRanges.first())
            } else {
                null
            }
            val last = feeding.getLast(lotId)
            val blocked = suggestion?.blockReasons.orEmpty()
            _state.update {
                it.copy(
                    loaded = true,
                    especie = lote.especie,
                    tankName = tanks.get(lote.tanqueId)?.nome.orEmpty(),
                    suggestion = suggestion,
                    lastHadLeftover = last?.sobraRacao == true,
                    quantity = suggestion?.takeIf { s -> s.blockReasons.isEmpty() }?.let { s -> formatInput(s.perMealKg, QUANTITY_DECIMALS) }.orEmpty(),
                    // Água fora do limite: a ração já vem suspensa, com o motivo. O usuário pode desfazer.
                    suspended = blocked.isNotEmpty(),
                    reason = blocked.firstOrNull()?.suspensionReason(),
                    brand = last?.marcaRacao.orEmpty(),
                    protein = last?.proteinaBrutaPct?.let { p -> formatInput(p, 1) }.orEmpty(),
                    pellet = last?.tamanhoPelletMm?.let { p -> formatInput(p, 1) }.orEmpty(),
                )
            }
        }
    }

    fun onQuantityChange(value: String) = _state.update { it.copy(quantity = value, quantityError = null) }

    fun onDateTimeChange(value: LocalDateTime) = _state.update { it.copy(dateTime = value) }

    fun onSuspendedChange(value: Boolean) = _state.update {
        it.copy(suspended = value, quantityError = null, reasonError = null)
    }

    fun onReasonChange(value: MotivoSuspensao) = _state.update { it.copy(reason = value, reasonError = null) }

    fun onBrandChange(value: String) = _state.update { it.copy(brand = value) }

    fun onProteinChange(value: String) = _state.update { it.copy(protein = value, proteinError = null) }

    fun onPelletChange(value: String) = _state.update { it.copy(pellet = value, pelletError = null) }

    fun onBehaviorChange(value: ComportamentoPeixe) = _state.update { it.copy(behavior = value) }

    fun onLeftoverChange(value: Boolean) = _state.update { it.copy(leftover = value) }

    fun onSaveFailedShown() = _state.update { it.copy(saveFailed = false) }

    /** Valida e salva o trato. */
    fun onSave() {
        val current = _state.value
        if (!current.canSave) return

        val quantityError = if (current.suspended) null else FormValidation.positiveDecimal(current.quantity, required = true)
        val reasonError = if (current.suspended && current.reason == null) FormError.REQUIRED else null
        val proteinError = FormValidation.positiveDecimal(current.protein, required = false)
            ?: parseDecimal(current.protein)?.takeIf { it > PERCENT_MAX }?.let { FormError.OUT_OF_RANGE_PERCENT }
        val pelletError = FormValidation.positiveDecimal(current.pellet, required = false)
        if (listOf(quantityError, reasonError, proteinError, pelletError).any { it != null }) {
            _state.update {
                it.copy(quantityError = quantityError, reasonError = reasonError, proteinError = proteinError, pelletError = pelletError)
            }
            return
        }

        val record = Arracoamento(
            loteId = lotId,
            dataHora = current.dateTime.atZone(clock.zone).toInstant(),
            // Ração suspensa vale 0 kg (specs/02-dominio.md).
            quantidadeKg = if (current.suspended) 0.0 else checkNotNull(parseDecimal(current.quantity)),
            suspenso = current.suspended,
            motivoSuspensao = if (current.suspended) current.reason else null,
            marcaRacao = current.brand.trim().ifBlank { null },
            proteinaBrutaPct = parseDecimal(current.protein),
            tamanhoPelletMm = parseDecimal(current.pellet),
            comportamento = current.behavior,
            sobraRacao = current.leftover,
        )
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching { feeding.save(record) }
                .onSuccess {
                    _state.update { it.copy(isSaving = false, saved = SaveFeedback.forConnection(connectivity.isOnline())) }
                }
                .onFailure { _state.update { it.copy(isSaving = false, saveFailed = true) } }
        }
    }

    companion object {
        /** Nome do argumento da rota `FeedingRecord(lotId)`. */
        const val LOT_ID_ARG = "lotId"
        private const val QUANTITY_DECIMALS = 3
        private const val PERCENT_MAX = 100.0

        /**
         * Leituras que travam a ração: oxigênio, temperatura, amônia **tóxica** (calculada) e nitrito.
         * Quem não foi medido fica `null` e não trava.
         */
        fun feedingReadings(readings: LatestWaterReadings): WaterReadings {
            val toxic = WaterReadingsEvaluator.evaluate(readings).alerts.firstOrNull { it.kind == AlertKind.TOXIC_AMMONIA }
            return WaterReadings(
                oxygenMgL = readings.oxigenioMgL?.value,
                temperatureC = readings.temperaturaC?.value,
                toxicAmmoniaMgL = toxic?.value,
                nitriteMgL = readings.nitritoMgL?.value,
            )
        }
    }
}

/** Atalho: motivo de suspensão correspondente à trava de ração. */
private fun FeedBlockReason.suspensionReason(): MotivoSuspensao = when (this) {
    FeedBlockReason.LOW_OXYGEN -> MotivoSuspensao.OXIGENIO_BAIXO
    FeedBlockReason.TEMPERATURE -> MotivoSuspensao.TEMPERATURA
    FeedBlockReason.TOXIC_AMMONIA -> MotivoSuspensao.AMONIA_ALTA
    FeedBlockReason.NITRITE -> MotivoSuspensao.NITRITO_ALTO
}
