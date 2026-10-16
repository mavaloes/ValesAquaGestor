package com.valoes.valesaquagestor.presentation.settings

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.valoes.valesaquagestor.domain.rules.WaterRangeField
import com.valoes.valesaquagestor.domain.rules.WaterRanges
import com.valoes.valesaquagestor.presentation.components.AppTextField
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.ConfirmDialog
import com.valoes.valesaquagestor.presentation.components.EmptyMessage
import com.valoes.valesaquagestor.presentation.components.FeedbackEffect
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.SectionTitle
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.formatInput

private class RangeFieldSpec(val field: WaterRangeField, @StringRes val label: Int)

/** Um parâmetro da água: os campos aparecem na ordem em que a validação exige (cada um maior que o de cima). */
private class RangeGroup(@StringRes val title: Int, @StringRes val unit: Int?, val fields: List<RangeFieldSpec>)

private val rangeGroups = listOf(
    RangeGroup(
        R.string.water_ranges_group_oxygen, R.string.unit_mg_l,
        listOf(
            RangeFieldSpec(WaterRangeField.OXYGEN_EMERGENCY_BELOW, R.string.range_label_emergency_below),
            RangeFieldSpec(WaterRangeField.OXYGEN_CRITICAL_BELOW, R.string.range_label_critical_below),
            RangeFieldSpec(WaterRangeField.OXYGEN_ADEQUATE_MIN, R.string.range_label_adequate_from),
        ),
    ),
    RangeGroup(
        R.string.water_ranges_group_ph, null,
        listOf(
            RangeFieldSpec(WaterRangeField.PH_CRITICAL_MIN, R.string.range_label_emergency_below),
            RangeFieldSpec(WaterRangeField.PH_ATTENTION_MIN, R.string.range_label_critical_below),
            RangeFieldSpec(WaterRangeField.PH_ADEQUATE_MIN, R.string.range_label_adequate_from),
            RangeFieldSpec(WaterRangeField.PH_ADEQUATE_MAX, R.string.range_label_adequate_until),
            RangeFieldSpec(WaterRangeField.PH_ATTENTION_MAX, R.string.range_label_critical_above),
            RangeFieldSpec(WaterRangeField.PH_CRITICAL_MAX, R.string.range_label_emergency_above),
        ),
    ),
    RangeGroup(
        R.string.water_ranges_group_temperature, R.string.unit_celsius,
        listOf(
            RangeFieldSpec(WaterRangeField.TEMPERATURE_CRITICAL_BELOW, R.string.range_label_critical_below),
            RangeFieldSpec(WaterRangeField.TEMPERATURE_ADEQUATE_MIN, R.string.range_label_adequate_from),
            RangeFieldSpec(WaterRangeField.TEMPERATURE_ADEQUATE_MAX, R.string.range_label_adequate_until),
        ),
    ),
    RangeGroup(
        R.string.water_ranges_group_ammonia, R.string.unit_mg_l,
        listOf(
            RangeFieldSpec(WaterRangeField.AMMONIA_ADEQUATE_BELOW, R.string.range_label_adequate_below),
            RangeFieldSpec(WaterRangeField.AMMONIA_CRITICAL_FROM, R.string.range_label_critical_from),
            RangeFieldSpec(WaterRangeField.AMMONIA_EMERGENCY_FROM, R.string.range_label_emergency_from),
        ),
    ),
    RangeGroup(
        R.string.water_ranges_group_nitrite, R.string.unit_mg_l,
        listOf(
            RangeFieldSpec(WaterRangeField.NITRITE_ADEQUATE_BELOW, R.string.range_label_adequate_below),
            RangeFieldSpec(WaterRangeField.NITRITE_CRITICAL_FROM, R.string.range_label_critical_from),
            RangeFieldSpec(WaterRangeField.NITRITE_EMERGENCY_FROM, R.string.range_label_emergency_from),
        ),
    ),
    RangeGroup(
        R.string.water_ranges_group_transparency, R.string.unit_cm,
        listOf(
            RangeFieldSpec(WaterRangeField.TRANSPARENCY_CRITICAL_BELOW, R.string.range_label_critical_below),
            RangeFieldSpec(WaterRangeField.TRANSPARENCY_ADEQUATE_MIN, R.string.range_label_adequate_from),
            RangeFieldSpec(WaterRangeField.TRANSPARENCY_ADEQUATE_MAX, R.string.range_label_adequate_until),
            RangeFieldSpec(WaterRangeField.TRANSPARENCY_CRITICAL_ABOVE, R.string.range_label_critical_above),
        ),
    ),
    RangeGroup(
        R.string.water_ranges_group_alkalinity, R.string.unit_mg_l,
        listOf(
            RangeFieldSpec(WaterRangeField.ALKALINITY_CRITICAL_BELOW, R.string.range_label_critical_below),
            RangeFieldSpec(WaterRangeField.ALKALINITY_ADEQUATE_MIN, R.string.range_label_adequate_from),
        ),
    ),
    RangeGroup(
        R.string.water_ranges_group_hardness, R.string.unit_mg_l,
        listOf(RangeFieldSpec(WaterRangeField.HARDNESS_ADEQUATE_MIN, R.string.range_label_adequate_from)),
    ),
)

/** Tela das faixas da água (só ADMIN). */
@Composable
fun WaterRangesScreen(onBack: () -> Unit, viewModel: WaterRangesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedText = stringResource(R.string.water_ranges_saved)
    val restoredText = stringResource(R.string.settings_restored)
    val failureText = (state.message as? SettingsMessage.Failed)?.let { stringResource(it.error.messageRes()) }
    FeedbackEffect(
        message = when (state.message) {
            null -> null
            SettingsMessage.Saved -> savedText
            SettingsMessage.Restored -> restoredText
            is SettingsMessage.Failed -> failureText
        },
        hostState = snackbarHostState,
        onShown = viewModel::onMessageShown,
    )
    WaterRangesContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onValueChange = viewModel::onValueChange,
        onSave = viewModel::onSave,
        onRestoreClick = viewModel::onRestoreClick,
        onRestoreConfirm = viewModel::onRestoreConfirm,
        onRestoreDismiss = viewModel::onRestoreDismiss,
    )
}

@Composable
internal fun WaterRangesContent(
    state: WaterRangesUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onValueChange: (WaterRangeField, String) -> Unit,
    onSave: () -> Unit,
    onRestoreClick: () -> Unit,
    onRestoreConfirm: () -> Unit,
    onRestoreDismiss: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.water_ranges_title), onBack = onBack, help = HelpTips.WaterRanges) },
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
            if (!state.isLoading && !state.allowed) {
                EmptyMessage(stringResource(R.string.no_permission))
                return@Column
            }
            Text(text = stringResource(R.string.water_ranges_intro), style = MaterialTheme.typography.bodyLarge)
            rangeGroups.forEach { group ->
                SectionTitle(stringResource(group.title))
                group.fields.forEach { spec ->
                    AppTextField(
                        value = state.values[spec.field].orEmpty(),
                        onValueChange = { onValueChange(spec.field, it) },
                        label = stringResource(spec.label),
                        suffix = group.unit?.let { stringResource(it) },
                        keyboardType = KeyboardType.Decimal,
                        errorText = state.errors[spec.field]?.let { stringResource(it.messageRes()) },
                        enabled = !state.isSaving && !state.isLoading,
                    )
                }
            }
            LargeButton(
                text = stringResource(R.string.water_ranges_save),
                icon = Icons.Default.Check,
                onClick = onSave,
                enabled = state.canSave,
                isLoading = state.isSaving,
            )
            TextButton(onClick = onRestoreClick, enabled = !state.isSaving) {
                Text(text = stringResource(R.string.settings_restore_defaults), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    if (state.showRestoreConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.settings_restore_title),
            text = stringResource(R.string.settings_restore_text),
            confirmText = stringResource(R.string.settings_restore_confirm),
            onConfirm = onRestoreConfirm,
            onDismiss = onRestoreDismiss,
        )
    }
}

@StringRes
private fun RangeFieldError.messageRes(): Int = when (this) {
    RangeFieldError.INVALID_NUMBER -> R.string.error_invalid_number
    RangeFieldError.ORDER -> R.string.error_range_order
}

private fun defaultInputs(): Map<WaterRangeField, String> =
    WaterRangeField.entries.associateWith { formatInput(WaterRanges.DEFAULT.valueOf(it), 4) }

@Preview(name = "Faixas da água", showBackground = true, fontScale = 2f)
@Composable
private fun WaterRangesPreview() {
    AquaGestorTheme {
        WaterRangesContent(
            state = WaterRangesUiState(
                isLoading = false,
                allowed = true,
                values = defaultInputs() + (WaterRangeField.OXYGEN_CRITICAL_BELOW to "5"),
                errors = mapOf(WaterRangeField.OXYGEN_CRITICAL_BELOW to RangeFieldError.ORDER),
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onValueChange = { _, _ -> }, onSave = {},
            onRestoreClick = {}, onRestoreConfirm = {}, onRestoreDismiss = {},
        )
    }
}

@Preview(name = "Faixas da água sem permissão", showBackground = true, fontScale = 2f)
@Composable
private fun WaterRangesDeniedPreview() {
    AquaGestorTheme {
        WaterRangesContent(
            state = WaterRangesUiState(isLoading = false, allowed = false),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onValueChange = { _, _ -> }, onSave = {},
            onRestoreClick = {}, onRestoreConfirm = {}, onRestoreDismiss = {},
        )
    }
}
