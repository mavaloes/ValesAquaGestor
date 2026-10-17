package com.valoes.valesaquagestor.presentation.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.calc.LotCalculator
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import com.valoes.valesaquagestor.domain.repository.ConnectivityChecker
import com.valoes.valesaquagestor.domain.repository.LotMovementRepository
import com.valoes.valesaquagestor.domain.repository.LotRepository
import com.valoes.valesaquagestor.domain.repository.TankRepository
import com.valoes.valesaquagestor.domain.rules.CompositeAlerts
import com.valoes.valesaquagestor.domain.rules.WaterStatus
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
 * Estado da tela de mortalidade e despesca (fluxo rápido: escolher, informar a quantidade, salvar).
 *
 * - **Mortalidade:** mostra a mortalidade acumulada com este registro e avisa se passar de 10%.
 * - **Despesca final:** a quantidade é todos os peixes vivos e o lote é encerrado, por isso pede
 *   confirmação ([confirmFinal]).
 * - **Transferência** não tem tela (`specs/08`, conflito 16).
 *
 * @property aliveFish peixes vivos agora; não se pode retirar mais que isso.
 * @property projectedMortalityPct mortalidade acumulada se este registro de mortalidade for salvo.
 * @property mortalityWarning aviso "chame um técnico" quando a mortalidade projetada passa de 10%.
 */
data class MovementUiState(
    val loaded: Boolean = false,
    val especie: Especie? = null,
    val tankName: String = "",
    val aliveFish: Int = 0,
    val deadSoFar: Int = 0,
    val initialQuantity: Int = 0,
    val type: TipoMovimentacao = TipoMovimentacao.MORTALIDADE,
    val quantity: String = "",
    val totalWeight: String = "",
    val notes: String = "",
    val dateTime: LocalDateTime,
    val quantityError: FormError? = null,
    val weightError: FormError? = null,
    val projectedMortalityPct: Double? = null,
    val mortalityWarning: WaterStatus? = null,
    val confirmFinal: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: SaveFeedback? = null,
) {
    /** Na despesca final a quantidade é fixa (todos os peixes vivos). */
    val quantityLocked: Boolean get() = type == TipoMovimentacao.DESPESCA_FINAL

    /** O botão Salvar habilita com a quantidade preenchida e peixes vivos no lote. */
    val canSave: Boolean get() = loaded && !isSaving && saved == null && aliveFish > 0 && quantity.isNotBlank()
}

@HiltViewModel
class MovementViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val lots: LotRepository,
    tanks: TankRepository,
    private val movements: LotMovementRepository,
    private val connectivity: ConnectivityChecker,
    private val clock: Clock,
) : ViewModel() {

    private val lotId: Long = checkNotNull(savedStateHandle[LOT_ID_ARG]) { "Falta o id do lote." }

    private val _state = MutableStateFlow(
        MovementUiState(dateTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES)),
    )
    val state: StateFlow<MovementUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val lote = lots.get(lotId) ?: return@launch
            val lotMovements = movements.observeByLot(lotId).first()
            _state.update {
                recalculate(
                    it.copy(
                        loaded = true,
                        especie = lote.especie,
                        tankName = tanks.get(lote.tanqueId)?.nome.orEmpty(),
                        aliveFish = LotCalculator.currentQuantity(lote, lotMovements),
                        deadSoFar = LotCalculator.deadCount(lotMovements),
                        initialQuantity = lote.quantidadeInicial,
                    ),
                )
            }
        }
    }

    fun onTypeChange(type: TipoMovimentacao) = _state.update {
        recalculate(
            it.copy(
                type = type,
                // Despesca final: todos os peixes vivos. Ao sair dela, a quantidade volta a ser digitada.
                quantity = when {
                    type == TipoMovimentacao.DESPESCA_FINAL -> it.aliveFish.toString()
                    it.type == TipoMovimentacao.DESPESCA_FINAL -> ""
                    else -> it.quantity
                },
                quantityError = null,
            ),
        )
    }

    fun onQuantityChange(value: String) = _state.update { recalculate(it.copy(quantity = value, quantityError = null)) }

    fun onTotalWeightChange(value: String) = _state.update { it.copy(totalWeight = value, weightError = null) }

    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    fun onDateTimeChange(value: LocalDateTime) = _state.update { it.copy(dateTime = value) }

    fun onSaveFailedShown() = _state.update { it.copy(saveFailed = false) }

    fun onDismissFinal() = _state.update { it.copy(confirmFinal = false) }

    /** Valida. A despesca final pede confirmação antes de salvar; os outros tipos salvam direto. */
    fun onSave() {
        val current = _state.value
        if (!current.canSave) return

        val quantityError = FormValidation.wholeNumberAtMost(current.quantity, current.aliveFish)
        val weightError = FormValidation.nonNegativeDecimal(current.totalWeight)
        if (quantityError != null || weightError != null) {
            _state.update { it.copy(quantityError = quantityError, weightError = weightError) }
            return
        }
        if (current.type == TipoMovimentacao.DESPESCA_FINAL) {
            _state.update { it.copy(confirmFinal = true) }
        } else {
            persist(current)
        }
    }

    /** O usuário confirmou o encerramento do lote. */
    fun onConfirmFinal() {
        val current = _state.value
        _state.update { it.copy(confirmFinal = false) }
        persist(current)
    }

    private fun persist(current: MovementUiState) {
        val record = MovimentacaoLote(
            loteId = lotId,
            dataHora = current.dateTime.atZone(clock.zone).toInstant(),
            tipo = current.type,
            quantidade = checkNotNull(parseWholeNumber(current.quantity)),
            pesoTotalKg = parseDecimal(current.totalWeight),
            observacao = current.notes.trim().ifBlank { null },
        )
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching { movements.save(record) }
                .onSuccess {
                    _state.update { it.copy(isSaving = false, saved = SaveFeedback.forConnection(connectivity.isOnline())) }
                }
                .onFailure { _state.update { it.copy(isSaving = false, saveFailed = true) } }
        }
    }

    /** Mortalidade acumulada se este registro de mortalidade for salvo, e o aviso de mais de 10%. */
    private fun recalculate(state: MovementUiState): MovementUiState {
        if (state.type != TipoMovimentacao.MORTALIDADE || state.initialQuantity <= 0) {
            return state.copy(projectedMortalityPct = null, mortalityWarning = null)
        }
        val quantity = parseWholeNumber(state.quantity)?.takeIf { it > 0 } ?: 0
        val dead = state.deadSoFar + quantity
        return state.copy(
            projectedMortalityPct = dead * PERCENT / state.initialQuantity,
            mortalityWarning = CompositeAlerts.accumulatedMortality(state.initialQuantity, dead),
        )
    }

    companion object {
        /** Nome do argumento da rota `MortalityRecord(lotId)`. */
        const val LOT_ID_ARG = "lotId"

        /** Tipos oferecidos na tela. A transferência não tem tela (`specs/08`, conflito 16). */
        val TYPES = listOf(
            TipoMovimentacao.MORTALIDADE,
            TipoMovimentacao.DESPESCA_PARCIAL,
            TipoMovimentacao.DESPESCA_FINAL,
        )

        private const val PERCENT = 100.0
    }
}
