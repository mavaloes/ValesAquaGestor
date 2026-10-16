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
import com.valoes.valesaquagestor.domain.calc.MonitoringSchedule
import com.valoes.valesaquagestor.domain.calc.ScheduleField
import com.valoes.valesaquagestor.presentation.components.AppTextField
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.ChoiceGroup
import com.valoes.valesaquagestor.presentation.components.ConfirmDialog
import com.valoes.valesaquagestor.presentation.components.EmptyMessage
import com.valoes.valesaquagestor.presentation.components.FeedbackEffect
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.SectionTitle
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import java.time.DayOfWeek

private class ScheduleFieldSpec(val field: ScheduleField, @StringRes val label: Int, @StringRes val unit: Int)

private val hourFields = listOf(
    ScheduleFieldSpec(ScheduleField.OXYGEN_MORNING_HOUR, R.string.schedule_oxygen_morning, R.string.unit_hour),
    ScheduleFieldSpec(ScheduleField.OXYGEN_AFTERNOON_HOUR, R.string.schedule_oxygen_afternoon, R.string.unit_hour),
    ScheduleFieldSpec(ScheduleField.TRANSPARENCY_FROM_HOUR, R.string.schedule_transparency_from, R.string.unit_hour),
    ScheduleFieldSpec(ScheduleField.TRANSPARENCY_TO_HOUR, R.string.schedule_transparency_to, R.string.unit_hour),
    ScheduleFieldSpec(ScheduleField.HIGH_LOAD_AMMONIA_HOUR, R.string.schedule_high_load_ammonia_hour, R.string.unit_hour),
)

private val intervalFields = listOf(
    ScheduleFieldSpec(ScheduleField.HIGH_LOAD_AMMONIA_INTERVAL, R.string.schedule_high_load_ammonia_interval, R.string.unit_days),
    ScheduleFieldSpec(ScheduleField.RECRIA_BIOMETRY_INTERVAL, R.string.schedule_recria_biometry, R.string.unit_days),
    ScheduleFieldSpec(ScheduleField.ENGORDA_BIOMETRY_INTERVAL, R.string.schedule_engorda_biometry, R.string.unit_days),
    ScheduleFieldSpec(ScheduleField.ALEVINAGEM_FEED_READJUST_INTERVAL, R.string.schedule_alevinagem_readjust, R.string.unit_days),
    ScheduleFieldSpec(ScheduleField.FEED_READJUST_INTERVAL, R.string.schedule_feed_readjust, R.string.unit_days),
)

/** Tela da agenda de medições (só ADMIN). */
@Composable
fun ScheduleScreen(onBack: () -> Unit, viewModel: ScheduleViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedText = stringResource(R.string.schedule_saved)
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
    ScheduleContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onValueChange = viewModel::onValueChange,
        onWeeklyDayChange = viewModel::onWeeklyDayChange,
        onSave = viewModel::onSave,
        onRestoreClick = viewModel::onRestoreClick,
        onRestoreConfirm = viewModel::onRestoreConfirm,
        onRestoreDismiss = viewModel::onRestoreDismiss,
    )
}

@Composable
internal fun ScheduleContent(
    state: ScheduleUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onValueChange: (ScheduleField, String) -> Unit,
    onWeeklyDayChange: (DayOfWeek) -> Unit,
    onSave: () -> Unit,
    onRestoreClick: () -> Unit,
    onRestoreConfirm: () -> Unit,
    onRestoreDismiss: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.schedule_title), onBack = onBack, help = HelpTips.Schedule) },
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
            Text(text = stringResource(R.string.schedule_intro), style = MaterialTheme.typography.bodyLarge)
            SectionTitle(stringResource(R.string.schedule_group_hours))
            hourFields.forEach { spec -> ScheduleTextField(spec, state, onValueChange) }
            SectionTitle(stringResource(R.string.schedule_group_intervals))
            intervalFields.forEach { spec -> ScheduleTextField(spec, state, onValueChange) }
            ChoiceGroup(
                label = stringResource(R.string.schedule_weekly_day),
                options = DayOfWeek.entries,
                selected = state.weeklyDay,
                optionLabel = { stringResource(it.labelRes()) },
                onSelect = onWeeklyDayChange,
            )
            LargeButton(
                text = stringResource(R.string.schedule_save),
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

@Composable
private fun ScheduleTextField(
    spec: ScheduleFieldSpec,
    state: ScheduleUiState,
    onValueChange: (ScheduleField, String) -> Unit,
) {
    AppTextField(
        value = state.values[spec.field].orEmpty(),
        onValueChange = { onValueChange(spec.field, it) },
        label = stringResource(spec.label),
        suffix = stringResource(spec.unit),
        keyboardType = KeyboardType.Number,
        errorText = state.errors[spec.field]?.let { stringResource(it.messageRes()) },
        enabled = !state.isSaving && !state.isLoading,
    )
}

@StringRes
private fun ScheduleFieldError.messageRes(): Int = when (this) {
    ScheduleFieldError.INVALID_NUMBER -> R.string.error_invalid_number
    ScheduleFieldError.INVALID_VALUE -> R.string.error_schedule_value
}

@StringRes
private fun DayOfWeek.labelRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.weekday_monday
    DayOfWeek.TUESDAY -> R.string.weekday_tuesday
    DayOfWeek.WEDNESDAY -> R.string.weekday_wednesday
    DayOfWeek.THURSDAY -> R.string.weekday_thursday
    DayOfWeek.FRIDAY -> R.string.weekday_friday
    DayOfWeek.SATURDAY -> R.string.weekday_saturday
    DayOfWeek.SUNDAY -> R.string.weekday_sunday
}

private fun defaultInputs(): Map<ScheduleField, String> =
    ScheduleField.entries.associateWith { MonitoringSchedule().valueOf(it).toString() }

@Preview(name = "Agenda de medições", showBackground = true, fontScale = 2f)
@Composable
private fun SchedulePreview() {
    AquaGestorTheme {
        ScheduleContent(
            state = ScheduleUiState(
                isLoading = false,
                allowed = true,
                values = defaultInputs() + (ScheduleField.OXYGEN_AFTERNOON_HOUR to "5"),
                errors = mapOf(ScheduleField.OXYGEN_AFTERNOON_HOUR to ScheduleFieldError.INVALID_VALUE),
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onValueChange = { _, _ -> }, onWeeklyDayChange = {}, onSave = {},
            onRestoreClick = {}, onRestoreConfirm = {}, onRestoreDismiss = {},
        )
    }
}
