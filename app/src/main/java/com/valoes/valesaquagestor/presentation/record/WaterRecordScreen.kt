package com.valoes.valesaquagestor.presentation.record

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.PlausibilityWarning
import com.valoes.valesaquagestor.domain.rules.WaterAlert
import com.valoes.valesaquagestor.domain.rules.WaterEvaluator
import com.valoes.valesaquagestor.presentation.components.AppTextField
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.ChoiceGroup
import com.valoes.valesaquagestor.presentation.components.DateTimeField
import com.valoes.valesaquagestor.presentation.components.FeedbackEffect
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.StatusCard
import com.valoes.valesaquagestor.presentation.components.SwitchRow
import com.valoes.valesaquagestor.presentation.components.WarningCard
import com.valoes.valesaquagestor.presentation.components.actionRes
import com.valoes.valesaquagestor.presentation.components.alertValueText
import com.valoes.valesaquagestor.presentation.components.labelRes
import com.valoes.valesaquagestor.presentation.components.messageRes
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.SaveFeedback
import com.valoes.valesaquagestor.presentation.util.formatDateTime
import com.valoes.valesaquagestor.presentation.util.stringRes
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Como cada campo da água aparece: rótulo, exemplo e unidade (sempre visível ao lado do valor). */
private data class WaterFieldSpec(
    val field: WaterField,
    @StringRes val label: Int,
    @StringRes val example: Int,
    @StringRes val unit: Int?,
)

private val waterFields = listOf(
    WaterFieldSpec(WaterField.OXYGEN, R.string.field_oxygen, R.string.field_oxygen_example, R.string.unit_mg_l),
    WaterFieldSpec(WaterField.PH, R.string.field_ph, R.string.field_ph_example, null),
    WaterFieldSpec(WaterField.TEMPERATURE, R.string.field_temperature, R.string.field_temperature_example, R.string.unit_celsius),
    WaterFieldSpec(WaterField.TRANSPARENCY, R.string.field_transparency, R.string.field_transparency_example, R.string.unit_cm),
    WaterFieldSpec(WaterField.TOTAL_AMMONIA, R.string.field_total_ammonia, R.string.field_total_ammonia_example, R.string.unit_mg_l),
    WaterFieldSpec(WaterField.NITRITE, R.string.field_nitrite, R.string.field_nitrite_example, R.string.unit_mg_l),
    WaterFieldSpec(WaterField.ALKALINITY, R.string.field_alkalinity, R.string.field_alkalinity_example, R.string.unit_mg_l),
    WaterFieldSpec(WaterField.HARDNESS, R.string.field_hardness, R.string.field_hardness_example, R.string.unit_mg_l),
)

/**
 * Registro de água (Planilhas 2 e 3). Data e hora já vêm preenchidas e o momento é sugerido pela
 * hora. Ao salvar, mostra o nível de cada valor e a amônia tóxica calculada.
 *
 * @param onDone chamado ao concluir, com o aviso certo ("Registro salvo" ou "Sem conexão…").
 */
@Composable
fun WaterRecordScreen(
    onBack: () -> Unit,
    onDone: (SaveFeedback) -> Unit,
    viewModel: WaterRecordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    FeedbackEffect(
        message = if (state.saveFailed) stringResource(R.string.message_unknown_error) else null,
        hostState = snackbarHostState,
        onShown = viewModel::onSaveFailedShown,
    )
    WaterRecordContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onDone = onDone,
        onValueChange = viewModel::onValueChange,
        onDateTimeChange = viewModel::onDateTimeChange,
        onMomentChange = viewModel::onMomentChange,
        onCloudyChange = viewModel::onCloudyChange,
        onSave = viewModel::onSave,
    )
}

@Composable
internal fun WaterRecordContent(
    state: WaterRecordUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onDone: (SaveFeedback) -> Unit,
    onValueChange: (WaterField, String) -> Unit,
    onDateTimeChange: (LocalDateTime) -> Unit,
    onMomentChange: (MomentoMedicao) -> Unit,
    onCloudyChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    val result = state.result
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(if (result != null) R.string.water_result_title else R.string.water_title),
                onBack = onBack,
                help = HelpTips.Water,
            )
        },
        snackbarHost = { FeedbackSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (result != null) {
                WaterResult(result, state.saved, onDone)
                return@Column
            }
            Text(text = stringResource(R.string.lot_in_tank, state.tankName), style = MaterialTheme.typography.bodyLarge)
            Text(text = stringResource(R.string.water_hint), style = MaterialTheme.typography.bodyMedium)
            if (state.fromSensor.isNotEmpty()) {
                Text(text = stringResource(R.string.water_sensor_banner), style = MaterialTheme.typography.bodyLarge)
            }
            DateTimeField(dateTime = state.dateTime, onDateTimeChange = onDateTimeChange)
            ChoiceGroup(
                label = stringResource(R.string.field_moment),
                options = MomentoMedicao.entries,
                selected = state.moment,
                optionLabel = { stringResource(it.labelRes()).replaceFirstChar { c -> c.uppercase() } },
                onSelect = onMomentChange,
            )
            waterFields.forEach { spec ->
                AppTextField(
                    value = state.values[spec.field].orEmpty(),
                    onValueChange = { onValueChange(spec.field, it) },
                    label = stringResource(spec.label),
                    placeholder = stringResource(spec.example),
                    helperText = if (spec.field in state.fromSensor) stringResource(R.string.field_from_sensor) else null,
                    suffix = spec.unit?.let { stringResource(it) },
                    keyboardType = KeyboardType.Decimal,
                    errorText = state.errors[spec.field]?.let { stringResource(it.stringRes()) },
                    enabled = !state.isSaving,
                )
            }
            SwitchRow(label = stringResource(R.string.field_cloudy), checked = state.cloudy, onCheckedChange = onCloudyChange)
            WarningCard(messages = state.warnings.map { stringResource(it.messageRes()) })
            LargeButton(
                text = stringResource(if (state.warnings.isEmpty()) R.string.action_save else R.string.action_save_anyway),
                icon = Icons.Default.Check,
                onClick = onSave,
                enabled = state.canSave,
                isLoading = state.isSaving,
            )
        }
    }
}

/** Depois de salvar: o nível de cada valor informado e a amônia tóxica calculada. */
@Composable
private fun WaterResult(result: WaterSaveResult, saved: SaveFeedback?, onDone: (SaveFeedback) -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    if (result.alerts.isEmpty() && !result.ammoniaNeedsPhAndTemperature) {
        Text(text = stringResource(R.string.water_result_none), style = MaterialTheme.typography.bodyLarge)
    }
    result.alerts.forEach { alert: WaterAlert ->
        StatusCard(
            title = stringResource(alert.kind.labelRes()),
            valueText = alertValueText(alert.kind, alert.value),
            meaning = stringResource(alert.status.messageRes()),
            action = stringResource(alert.status.actionRes()),
            level = alert.status.level,
            footnote = stringResource(R.string.measured_at, formatDateTime(alert.measuredAt, zone)),
        )
    }
    if (result.ammoniaNeedsPhAndTemperature) {
        Text(text = stringResource(R.string.ammonia_needs_ph_temperature), style = MaterialTheme.typography.bodyLarge)
    }
    LargeButton(
        text = stringResource(R.string.action_done),
        icon = Icons.Default.Check,
        onClick = { onDone(saved ?: SaveFeedback.SAVED) },
    )
}

private val previewTime: LocalDateTime = LocalDateTime.of(2026, 10, 16, 6, 10)

@Preview(name = "Registrar água", showBackground = true, fontScale = 2f)
@Composable
private fun WaterRecordPreview() {
    AquaGestorTheme {
        WaterRecordContent(
            state = WaterRecordUiState(
                tankName = "Viveiro 1", tankLoaded = true, dateTime = previewTime, moment = MomentoMedicao.MANHA,
                values = mapOf(WaterField.PH to "10,5", WaterField.OXYGEN to "x"),
                errors = mapOf(WaterField.OXYGEN to com.valoes.valesaquagestor.presentation.util.FormError.INVALID_NUMBER),
                warnings = listOf(PlausibilityWarning.PH_VERY_HIGH),
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onDone = {}, onValueChange = { _, _ -> }, onDateTimeChange = {}, onMomentChange = {}, onCloudyChange = {}, onSave = {},
        )
    }
}

@Preview(name = "Água salva", showBackground = true, fontScale = 2f)
@Composable
private fun WaterResultPreview() {
    val now = Instant.parse("2026-10-16T09:10:00Z")
    AquaGestorTheme {
        WaterRecordContent(
            state = WaterRecordUiState(
                tankName = "Viveiro 1", tankLoaded = true, dateTime = previewTime, moment = MomentoMedicao.MANHA,
                saved = SaveFeedback.SAVED_OFFLINE,
                result = WaterSaveResult(
                    alerts = listOf(
                        WaterAlert(AlertKind.OXYGEN, 2.5, WaterEvaluator.evaluateOxygen(2.5), now),
                        WaterAlert(AlertKind.TOXIC_AMMONIA, 0.66, WaterEvaluator.evaluateToxicAmmonia(0.66), now),
                    ),
                    ammoniaNeedsPhAndTemperature = false,
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onDone = {}, onValueChange = { _, _ -> }, onDateTimeChange = {}, onMomentChange = {}, onCloudyChange = {}, onSave = {},
        )
    }
}
