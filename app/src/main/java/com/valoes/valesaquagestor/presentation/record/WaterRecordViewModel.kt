package com.valoes.valesaquagestor.presentation.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.calc.MomentSuggester
import com.valoes.valesaquagestor.domain.model.LatestWaterReadings
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.model.QualidadeAgua
import com.valoes.valesaquagestor.domain.model.Reading
import com.valoes.valesaquagestor.domain.repository.ConnectivityChecker
import com.valoes.valesaquagestor.domain.repository.TankRepository
import com.valoes.valesaquagestor.domain.repository.WaterQualityRepository
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.Plausibility
import com.valoes.valesaquagestor.domain.rules.PlausibilityWarning
import com.valoes.valesaquagestor.domain.rules.WaterAlert
import com.valoes.valesaquagestor.domain.rules.WaterReadingsEvaluator
import com.valoes.valesaquagestor.presentation.util.FormError
import com.valoes.valesaquagestor.presentation.util.FormValidation
import com.valoes.valesaquagestor.presentation.util.SaveFeedback
import com.valoes.valesaquagestor.presentation.util.parseDecimal
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Os oito parâmetros das Planilhas 2 e 3 (F01 p.151-152). */
enum class WaterField {
    TRANSPARENCY,
    TEMPERATURE,
    OXYGEN,
    PH,
    TOTAL_AMMONIA,
    NITRITE,
    ALKALINITY,
    HARDNESS,
}

/**
 * Resultado mostrado depois de salvar: o nível de cada valor informado e a amônia tóxica calculada.
 *
 * @property ammoniaNeedsPhAndTemperature `true` se informou amônia total e faltam pH e temperatura
 *   recentes para calcular a tóxica.
 */
data class WaterSaveResult(
    val alerts: List<WaterAlert>,
    val ammoniaNeedsPhAndTemperature: Boolean,
)

/**
 * Estado do registro de água. Todas as medidas são opcionais, mas é preciso informar **pelo menos uma**.
 *
 * @property values texto digitado de cada parâmetro (vírgula ou ponto como separador decimal).
 * @property warnings valores improváveis: avisam, mas deixam salvar ("Salvar mesmo assim").
 * @property saved preenchido quando o registro foi salvo; diz qual aviso mostrar.
 * @property result nível de cada valor e amônia tóxica, mostrados depois de salvar.
 */
data class WaterRecordUiState(
    val tankName: String = "",
    val tankLoaded: Boolean = false,
    val dateTime: LocalDateTime,
    val moment: MomentoMedicao,
    val cloudy: Boolean = false,
    val values: Map<WaterField, String> = emptyMap(),
    val errors: Map<WaterField, FormError> = emptyMap(),
    val warnings: List<PlausibilityWarning> = emptyList(),
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: SaveFeedback? = null,
    val result: WaterSaveResult? = null,
) {
    /** O botão Salvar só habilita com pelo menos uma medida preenchida. */
    val canSave: Boolean get() = tankLoaded && !isSaving && saved == null && values.values.any { it.isNotBlank() }
}

@HiltViewModel
class WaterRecordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val water: WaterQualityRepository,
    tanks: TankRepository,
    private val connectivity: ConnectivityChecker,
    private val clock: Clock,
) : ViewModel() {

    private val tankId: Long = checkNotNull(savedStateHandle[TANK_ID_ARG]) { "Falta o id do tanque." }

    /** Depois que o usuário escolhe o momento, mudar a hora não o troca mais. */
    private var momentChosenByUser = false

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<WaterRecordUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val tank = tanks.get(tankId)
            _state.update { it.copy(tankName = tank?.nome.orEmpty(), tankLoaded = tank != null) }
        }
    }

    private fun initialState(): WaterRecordUiState {
        val now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES)
        return WaterRecordUiState(dateTime = now, moment = MomentSuggester.suggest(now.toLocalTime()))
    }

    fun onValueChange(field: WaterField, text: String) = _state.update {
        val values = it.values + (field to text)
        it.copy(values = values, errors = it.errors - field, warnings = warningsFor(values))
    }

    fun onDateTimeChange(dateTime: LocalDateTime) = _state.update {
        // O momento acompanha a hora enquanto o usuário não o escolher.
        val moment = if (momentChosenByUser) it.moment else MomentSuggester.suggest(dateTime.toLocalTime())
        it.copy(dateTime = dateTime, moment = moment)
    }

    fun onMomentChange(moment: MomentoMedicao) {
        momentChosenByUser = true
        _state.update { it.copy(moment = moment) }
    }

    fun onCloudyChange(cloudy: Boolean) = _state.update { it.copy(cloudy = cloudy) }

    fun onSaveFailedShown() = _state.update { it.copy(saveFailed = false) }

    /** Valida e salva. Valores impossíveis são bloqueados; os improváveis só avisam. */
    fun onSave() {
        val current = _state.value
        if (!current.canSave) return

        val errors = validate(current.values)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }

        val at = current.dateTime.atZone(clock.zone).toInstant()
        fun value(field: WaterField) = parseDecimal(current.values[field].orEmpty())
        val record = QualidadeAgua(
            tanqueId = tankId,
            dataHora = at,
            momento = current.moment,
            transparenciaCm = value(WaterField.TRANSPARENCY),
            temperaturaC = value(WaterField.TEMPERATURE),
            oxigenioMgL = value(WaterField.OXYGEN),
            ph = value(WaterField.PH),
            amoniaTotalMgL = value(WaterField.TOTAL_AMMONIA),
            nitritoMgL = value(WaterField.NITRITE),
            alcalinidadeMgL = value(WaterField.ALKALINITY),
            durezaMgL = value(WaterField.HARDNESS),
            diaNublado = current.cloudy,
        )
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching {
                water.save(record)
                buildResult(record, at)
            }
                .onSuccess { result ->
                    _state.update {
                        it.copy(isSaving = false, saved = SaveFeedback.forConnection(connectivity.isOnline()), result = result)
                    }
                }
                .onFailure { _state.update { it.copy(isSaving = false, saveFailed = true) } }
        }
    }

    private fun validate(values: Map<WaterField, String>): Map<WaterField, FormError> = buildMap {
        WaterField.entries.forEach { field ->
            val text = values[field].orEmpty()
            val error = if (field == WaterField.PH) FormValidation.ph(text) else FormValidation.nonNegativeDecimal(text)
            if (error != null) put(field, error)
        }
    }

    private fun warningsFor(values: Map<WaterField, String>): List<PlausibilityWarning> = Plausibility.checkWater(
        ph = parseDecimal(values[WaterField.PH].orEmpty()),
        oxygenMgL = parseDecimal(values[WaterField.OXYGEN].orEmpty()),
    )

    /**
     * Avalia cada valor informado e calcula a amônia tóxica. Se o registro não trouxe pH ou
     * temperatura, usa as últimas leituras do tanque (as de até 24 h em volta da amônia).
     */
    private suspend fun buildResult(record: QualidadeAgua, at: Instant): WaterSaveResult {
        val latest = water.observeLatestReadings(tankId).first()
        fun reading(value: Double?) = value?.let { Reading(it, at) }
        val readings = LatestWaterReadings(
            transparenciaCm = reading(record.transparenciaCm),
            temperaturaC = reading(record.temperaturaC) ?: latest.temperaturaC,
            oxigenioMgL = reading(record.oxigenioMgL),
            ph = reading(record.ph) ?: latest.ph,
            amoniaTotalMgL = reading(record.amoniaTotalMgL),
            nitritoMgL = reading(record.nitritoMgL),
            alcalinidadeMgL = reading(record.alcalinidadeMgL),
            durezaMgL = reading(record.durezaMgL),
        )
        val report = WaterReadingsEvaluator.evaluate(readings)
        val informed = buildSet {
            if (record.transparenciaCm != null) add(AlertKind.TRANSPARENCY)
            if (record.temperaturaC != null) add(AlertKind.TEMPERATURE)
            if (record.oxigenioMgL != null) add(AlertKind.OXYGEN)
            if (record.ph != null) add(AlertKind.PH)
            if (record.nitritoMgL != null) add(AlertKind.NITRITE)
            if (record.alcalinidadeMgL != null) add(AlertKind.ALKALINITY)
            if (record.durezaMgL != null) add(AlertKind.HARDNESS)
            add(AlertKind.TOXIC_AMMONIA)
        }
        return WaterSaveResult(
            alerts = report.alerts.filter { it.kind in informed },
            ammoniaNeedsPhAndTemperature = report.ammoniaNeedsPhAndTemperature,
        )
    }

    companion object {
        /** Nome do argumento da rota `WaterRecord(tankId)`. */
        const val TANK_ID_ARG = "tankId"
    }
}
