package com.valoes.valesaquagestor.presentation.lot

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.calc.CapacityCalculator
import com.valoes.valesaquagestor.domain.calc.CapacityCheck
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.model.Tanque
import com.valoes.valesaquagestor.domain.model.TipoTanque
import com.valoes.valesaquagestor.domain.repository.LotRepository
import com.valoes.valesaquagestor.domain.repository.TankRepository
import com.valoes.valesaquagestor.presentation.util.FormError
import com.valoes.valesaquagestor.presentation.util.FormValidation
import com.valoes.valesaquagestor.presentation.util.parseDecimal
import com.valoes.valesaquagestor.presentation.util.parseWholeNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado do formulário de novo lote.
 *
 * @property capacityApplies `true` se o tanque é viveiro escavado ou açude com área: só nesses a
 *   capacidade é calculada. Então o peso final esperado é obrigatório.
 * @property capacity conferência da capacidade; `null` enquanto o peso final não foi informado.
 *   Se [CapacityCheck.exceeds], a tela avisa e o botão vira "Salvar mesmo assim".
 */
data class LotFormUiState(
    val tankName: String = "",
    val tankLoaded: Boolean = false,
    val species: Especie = Especie.TILAPIA,
    val phase: FaseLote = FaseLote.ALEVINAGEM,
    val quantity: String = "",
    val initialWeight: String = "",
    val finalWeight: String = "",
    val origin: String = "",
    val stockingDate: LocalDate = LocalDate.ofEpochDay(0),
    val capacityApplies: Boolean = false,
    val capacity: CapacityCheck? = null,
    val quantityError: FormError? = null,
    val initialWeightError: FormError? = null,
    val finalWeightError: FormError? = null,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val savedLotId: Long? = null,
) {
    /** O botão Salvar só habilita com os obrigatórios preenchidos. */
    val canSave: Boolean
        get() = tankLoaded && !isSaving && quantity.isNotBlank() && initialWeight.isNotBlank() &&
            (!capacityApplies || finalWeight.isNotBlank())

    /** Passa da capacidade do viveiro: a tela avisa, mas deixa salvar. */
    val exceedsCapacity: Boolean get() = capacity?.exceeds == true
}

@HiltViewModel
class LotFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val lotRepository: LotRepository,
    private val tankRepository: TankRepository,
    private val clock: Clock,
) : ViewModel() {

    private val tankId: Long = checkNotNull(savedStateHandle[TANK_ID_ARG]) { "Falta o id do tanque." }
    private var tank: Tanque? = null

    private val _state = MutableStateFlow(LotFormUiState(stockingDate = LocalDate.now(clock)))
    val state: StateFlow<LotFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = tankRepository.get(tankId)
            tank = loaded
            _state.update {
                recalculateCapacity(
                    it.copy(
                        tankName = loaded?.nome.orEmpty(),
                        tankLoaded = loaded != null,
                        capacityApplies = loaded?.let(::supportsCapacity) == true,
                    ),
                )
            }
        }
    }

    fun onSpeciesChange(value: Especie) = _state.update { it.copy(species = value) }

    fun onPhaseChange(value: FaseLote) = _state.update { recalculateCapacity(it.copy(phase = value)) }

    fun onQuantityChange(value: String) = _state.update { recalculateCapacity(it.copy(quantity = value, quantityError = null)) }

    fun onInitialWeightChange(value: String) = _state.update { it.copy(initialWeight = value, initialWeightError = null) }

    fun onFinalWeightChange(value: String) = _state.update { recalculateCapacity(it.copy(finalWeight = value, finalWeightError = null)) }

    fun onOriginChange(value: String) = _state.update { it.copy(origin = value) }

    fun onStockingDateChange(value: LocalDate) = _state.update { it.copy(stockingDate = value) }

    fun onSaveFailedShown() = _state.update { it.copy(saveFailed = false) }

    /** Valida e salva. O aviso de capacidade não bloqueia: o usuário decide. */
    fun onSave() {
        val current = _state.value
        if (current.isSaving || !current.canSave) return

        val quantityError = FormValidation.positiveWholeNumber(current.quantity, required = true)
        val initialWeightError = FormValidation.positiveDecimal(current.initialWeight, required = true)
        val finalWeightError = FormValidation.positiveDecimal(current.finalWeight, required = current.capacityApplies)
        if (quantityError != null || initialWeightError != null || finalWeightError != null) {
            _state.update {
                it.copy(quantityError = quantityError, initialWeightError = initialWeightError, finalWeightError = finalWeightError)
            }
            return
        }

        val initialWeight = checkNotNull(parseDecimal(current.initialWeight))
        val povoamento = if (current.stockingDate == LocalDate.now(clock)) {
            clock.instant()
        } else {
            current.stockingDate.atStartOfDay(clock.zone).toInstant()
        }
        val lote = Lote(
            tanqueId = tankId,
            especie = current.species,
            fase = current.phase,
            quantidadeInicial = checkNotNull(parseWholeNumber(current.quantity)),
            pesoMedioInicialG = initialWeight,
            dataPovoamento = povoamento,
            // No povoamento o peso atual é o inicial; cada biometria o atualiza.
            pesoMedioAtualG = initialWeight,
            origemAlevinos = current.origin.trim().ifBlank { null },
        )
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching { lotRepository.save(lote) }
                .onSuccess { id -> _state.update { it.copy(isSaving = false, savedLotId = id) } }
                .onFailure { _state.update { it.copy(isSaving = false, saveFailed = true) } }
        }
    }

    private fun supportsCapacity(tank: Tanque) =
        tank.areaM2 != null && (tank.tipo == TipoTanque.ESCAVADO || tank.tipo == TipoTanque.ACUDE)

    /** Reconfere a capacidade a cada mudança de fase, quantidade ou peso final. */
    private fun recalculateCapacity(state: LotFormUiState): LotFormUiState {
        val loaded = tank ?: return state.copy(capacity = null)
        val finalWeight = parseDecimal(state.finalWeight)?.takeIf { it > 0.0 } ?: return state.copy(capacity = null)
        val fishCount = parseWholeNumber(state.quantity)?.takeIf { it > 0 } ?: 0
        return state.copy(capacity = CapacityCalculator.check(loaded, state.phase, fishCount, finalWeight))
    }

    companion object {
        /** Nome do argumento da rota `NewLot(tankId)`. */
        const val TANK_ID_ARG = "tankId"
    }
}
