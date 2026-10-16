package com.valoes.valesaquagestor.presentation.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.calc.BiometryCalculator
import com.valoes.valesaquagestor.domain.calc.BiometrySummary
import com.valoes.valesaquagestor.domain.calc.FeedSuggestion
import com.valoes.valesaquagestor.domain.calc.FeedingCalculator
import com.valoes.valesaquagestor.domain.calc.LotCalculator
import com.valoes.valesaquagestor.domain.calc.Weighing
import com.valoes.valesaquagestor.domain.model.Biometria
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.repository.BiometryRepository
import com.valoes.valesaquagestor.domain.repository.ConnectivityChecker
import com.valoes.valesaquagestor.domain.repository.LotMovementRepository
import com.valoes.valesaquagestor.domain.repository.LotRepository
import com.valoes.valesaquagestor.domain.repository.TankRepository
import com.valoes.valesaquagestor.domain.repository.WaterQualityRepository
import com.valoes.valesaquagestor.domain.rules.Plausibility
import com.valoes.valesaquagestor.domain.rules.PlausibilityWarning
import com.valoes.valesaquagestor.presentation.util.FormError
import com.valoes.valesaquagestor.presentation.util.FormValidation
import com.valoes.valesaquagestor.presentation.util.SaveFeedback
import com.valoes.valesaquagestor.presentation.util.parseDecimal
import com.valoes.valesaquagestor.presentation.util.parseWholeNumber
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
 * Uma pesagem digitada. A tara em branco vale 0 (puçá sem balde).
 *
 * @property id identifica a pesagem na lista (a posição muda quando uma é removida).
 */
data class WeighingInput(
    val id: Int,
    val gross: String = "",
    val tare: String = "",
    val fishCount: String = "",
    val grossError: FormError? = null,
    val tareError: FormError? = null,
    val fishCountError: FormError? = null,
) {
    /** Nenhum campo preenchido: uma linha em branco é ignorada. */
    val isBlank: Boolean get() = gross.isBlank() && tare.isBlank() && fishCount.isBlank()
}

/**
 * Resultado mostrado depois de salvar.
 *
 * @property averageWeightG peso médio medido, em g.
 * @property suggestion novo valor de ração para o peso médio medido; `null` sem tabela.
 */
data class BiometryResult(val averageWeightG: Double, val suggestion: FeedSuggestion?)

/**
 * Estado da biometria: várias pesagens (peso com peixes, tara e nº de peixes), o peso médio
 * calculado na hora e os avisos de amostra pequena e de peso improvável.
 *
 * @property summary totais das pesagens válidas; `null` enquanto não há nenhuma.
 * @property recommendedSample tamanho de amostra recomendado para os peixes vivos do lote.
 * @property sampleTooSmall a amostra tem menos peixes que o recomendado (aviso, não bloqueio).
 * @property warnings peso médio improvável (aviso, não bloqueio).
 */
data class BiometryUiState(
    val loaded: Boolean = false,
    val especie: Especie? = null,
    val tankName: String = "",
    val aliveFish: Int = 0,
    val dateTime: LocalDateTime,
    val weighings: List<WeighingInput> = listOf(WeighingInput(id = 0)),
    val notes: String = "",
    val summary: BiometrySummary? = null,
    val recommendedSample: Int = 0,
    val sampleTooSmall: Boolean = false,
    val warnings: List<PlausibilityWarning> = emptyList(),
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: SaveFeedback? = null,
    val result: BiometryResult? = null,
) {
    /**
     * O botão Salvar habilita assim que há alguma pesagem preenchida. Ao tocar, as pesagens
     * incompletas ou erradas mostram o erro (em vez de o botão ficar apagado sem explicação).
     */
    val canSave: Boolean get() = loaded && !isSaving && saved == null && weighings.any { !it.isBlank }

    /** Há avisos: o botão vira "Salvar mesmo assim". */
    val hasWarnings: Boolean get() = sampleTooSmall || warnings.isNotEmpty()
}

@HiltViewModel
class BiometryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val lots: LotRepository,
    tanks: TankRepository,
    movements: LotMovementRepository,
    private val water: WaterQualityRepository,
    private val biometry: BiometryRepository,
    private val connectivity: ConnectivityChecker,
    private val clock: Clock,
) : ViewModel() {

    private val lotId: Long = checkNotNull(savedStateHandle[LOT_ID_ARG]) { "Falta o id do lote." }
    private var nextRowId = 1
    private var currentAverageG = 0.0
    private var tankId = 0L

    private val _state = MutableStateFlow(
        BiometryUiState(dateTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES)),
    )
    val state: StateFlow<BiometryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val lote = lots.get(lotId) ?: return@launch
            val alive = LotCalculator.currentQuantity(lote, movements.observeByLot(lotId).first())
            currentAverageG = lote.pesoMedioAtualG
            tankId = lote.tanqueId
            _state.update {
                recalculate(
                    it.copy(
                        loaded = true,
                        especie = lote.especie,
                        tankName = tanks.get(lote.tanqueId)?.nome.orEmpty(),
                        aliveFish = alive,
                        recommendedSample = BiometryCalculator.recommendedSample(alive),
                    ),
                )
            }
        }
    }

    fun onGrossChange(id: Int, value: String) = updateRow(id) { it.copy(gross = value, grossError = null) }

    fun onTareChange(id: Int, value: String) = updateRow(id) { it.copy(tare = value, tareError = null) }

    fun onFishCountChange(id: Int, value: String) = updateRow(id) { it.copy(fishCount = value, fishCountError = null) }

    fun onAddWeighing() = _state.update { it.copy(weighings = it.weighings + WeighingInput(id = nextRowId++)) }

    /** Remove uma pesagem; sempre sobra pelo menos uma. */
    fun onRemoveWeighing(id: Int) = _state.update {
        if (it.weighings.size <= 1) it else recalculate(it.copy(weighings = it.weighings.filterNot { row -> row.id == id }))
    }

    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    fun onDateTimeChange(value: LocalDateTime) = _state.update { it.copy(dateTime = value) }

    fun onSaveFailedShown() = _state.update { it.copy(saveFailed = false) }

    /** Valida todas as pesagens e salva os totais (peso sem a tara e nº de peixes). */
    fun onSave() {
        val current = _state.value
        if (!current.canSave) return

        val rows = current.weighings.filterNot { it.isBlank }
        val validated = rows.map(::validateRow)
        if (validated.any { it.hasError }) {
            _state.update { state ->
                state.copy(weighings = state.weighings.map { row -> validated.firstOrNull { it.id == row.id } ?: row })
            }
            return
        }
        val summary = current.summary ?: return
        val record = Biometria(
            loteId = lotId,
            dataHora = current.dateTime.atZone(clock.zone).toInstant(),
            pesoAmostraKg = summary.netWeightKg,
            numeroPeixesAmostra = summary.fishCount,
            observacaoSaude = current.notes.trim().ifBlank { null },
        )
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching {
                biometry.save(record)
                resultFor(current, summary)
            }
                .onSuccess { result ->
                    _state.update {
                        it.copy(isSaving = false, saved = SaveFeedback.forConnection(connectivity.isOnline()), result = result)
                    }
                }
                .onFailure { _state.update { it.copy(isSaving = false, saveFailed = true) } }
        }
    }

    /** Sugere a ração para o peso médio que acabou de ser medido (a biometria real substitui a estimativa). */
    private suspend fun resultFor(state: BiometryUiState, summary: BiometrySummary): BiometryResult {
        val suggestion = if (state.especie == Especie.TILAPIA) {
            val readings = FeedingRecordViewModel.feedingReadings(water.observeLatestReadings(tankId).first())
            FeedingCalculator.suggest(state.aliveFish, summary.averageWeightG, readings)
        } else {
            null
        }
        return BiometryResult(summary.averageWeightG, suggestion)
    }

    private fun updateRow(id: Int, change: (WeighingInput) -> WeighingInput) = _state.update { state ->
        recalculate(state.copy(weighings = state.weighings.map { if (it.id == id) change(it) else it }))
    }

    /** Valida uma pesagem; se estiver certa, devolve a mesma linha sem erros. */
    private fun validateRow(row: WeighingInput): WeighingInput {
        val grossError = FormValidation.positiveDecimal(row.gross, required = true)
        val tareError = FormValidation.nonNegativeDecimal(row.tare)
        val countError = FormValidation.positiveWholeNumber(row.fishCount, required = true)
        val gross = parseDecimal(row.gross)
        val tare = parseDecimal(row.tare) ?: 0.0
        val belowTare = if (grossError == null && tareError == null && gross != null && gross <= tare) FormError.GROSS_BELOW_TARE else null
        return row.copy(grossError = grossError ?: belowTare, tareError = tareError, fishCountError = countError)
    }

    private val WeighingInput.hasError: Boolean get() = grossError != null || tareError != null || fishCountError != null

    /** Recalcula os totais com as pesagens completas e os avisos de amostra e de peso. */
    private fun recalculate(state: BiometryUiState): BiometryUiState {
        val weighings = state.weighings.mapNotNull { row ->
            val validated = validateRow(row.copy(grossError = null, tareError = null, fishCountError = null))
            if (validated.hasError) {
                null
            } else {
                Weighing(parseDecimal(row.gross)!!, parseDecimal(row.tare) ?: 0.0, parseWholeNumber(row.fishCount)!!)
            }
        }
        val summary = BiometryCalculator.summarize(weighings)
        val sampleTooSmall = summary != null && state.aliveFish > 0 && summary.fishCount < state.recommendedSample
        val warnings = summary?.let { Plausibility.checkAverageWeight(it.averageWeightG, currentAverageG) }
            ?.let { listOf(it) }.orEmpty()
        return state.copy(summary = summary, sampleTooSmall = sampleTooSmall, warnings = warnings)
    }

    companion object {
        /** Nome do argumento da rota `Biometry(lotId)`. */
        const val LOT_ID_ARG = "lotId"
    }
}
