package com.valoes.valesaquagestor.presentation.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.calc.BiometrySummary
import com.valoes.valesaquagestor.domain.calc.FeedingCalculator
import com.valoes.valesaquagestor.domain.calc.WaterReadings
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.presentation.components.AppTextField
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.DateTimeField
import com.valoes.valesaquagestor.presentation.components.FeedbackEffect
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.SectionTitle
import com.valoes.valesaquagestor.presentation.components.WarningCard
import com.valoes.valesaquagestor.presentation.components.messageRes
import com.valoes.valesaquagestor.presentation.components.requiredLabel
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.SaveFeedback
import com.valoes.valesaquagestor.presentation.util.formatNumber
import com.valoes.valesaquagestor.presentation.util.stringRes
import java.time.LocalDateTime

/**
 * Biometria: várias pesagens (peso com peixes, tara e nº de peixes), o peso médio calculado na
 * hora e, depois de salvar, a sugestão do novo valor de ração.
 *
 * @param onDone chamado ao concluir, com o aviso certo.
 * @param onRegisterFeeding atalho "Registrar ração" depois de salvar.
 */
@Composable
fun BiometryScreen(
    onBack: () -> Unit,
    onDone: (SaveFeedback) -> Unit,
    onRegisterFeeding: (SaveFeedback) -> Unit,
    viewModel: BiometryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    FeedbackEffect(
        message = if (state.saveFailed) stringResource(R.string.message_unknown_error) else null,
        hostState = snackbarHostState,
        onShown = viewModel::onSaveFailedShown,
    )
    BiometryContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onDone = onDone,
        onRegisterFeeding = onRegisterFeeding,
        onGrossChange = viewModel::onGrossChange,
        onTareChange = viewModel::onTareChange,
        onFishCountChange = viewModel::onFishCountChange,
        onAddWeighing = viewModel::onAddWeighing,
        onRemoveWeighing = viewModel::onRemoveWeighing,
        onNotesChange = viewModel::onNotesChange,
        onDateTimeChange = viewModel::onDateTimeChange,
        onSave = viewModel::onSave,
    )
}

@Composable
internal fun BiometryContent(
    state: BiometryUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onDone: (SaveFeedback) -> Unit,
    onRegisterFeeding: (SaveFeedback) -> Unit,
    onGrossChange: (Int, String) -> Unit,
    onTareChange: (Int, String) -> Unit,
    onFishCountChange: (Int, String) -> Unit,
    onAddWeighing: () -> Unit,
    onRemoveWeighing: (Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onDateTimeChange: (LocalDateTime) -> Unit,
    onSave: () -> Unit,
) {
    val result = state.result
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(if (result != null) R.string.biometry_result_title else R.string.biometry_title),
                onBack = onBack,
                help = HelpTips.Biometry,
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
                BiometryResultPanel(result, state.saved ?: SaveFeedback.SAVED, onDone, onRegisterFeeding)
                return@Column
            }
            Text(text = stringResource(R.string.form_required_legend), style = MaterialTheme.typography.bodyMedium)
            state.weighings.forEachIndexed { index, row ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle(text = stringResource(R.string.biometry_weighing_title, index + 1))
                        AppTextField(
                            value = row.gross,
                            onValueChange = { onGrossChange(row.id, it) },
                            label = requiredLabel(stringResource(R.string.field_gross_weight)),
                            placeholder = stringResource(R.string.field_gross_weight_example),
                            suffix = stringResource(R.string.unit_kg),
                            keyboardType = KeyboardType.Decimal,
                            errorText = row.grossError?.let { stringResource(it.stringRes()) },
                            enabled = !state.isSaving,
                        )
                        AppTextField(
                            value = row.tare,
                            onValueChange = { onTareChange(row.id, it) },
                            label = stringResource(R.string.field_tare),
                            placeholder = stringResource(R.string.field_tare_example),
                            suffix = stringResource(R.string.unit_kg),
                            keyboardType = KeyboardType.Decimal,
                            errorText = row.tareError?.let { stringResource(it.stringRes()) },
                            enabled = !state.isSaving,
                        )
                        AppTextField(
                            value = row.fishCount,
                            onValueChange = { onFishCountChange(row.id, it) },
                            label = requiredLabel(stringResource(R.string.field_fish_in_weighing)),
                            placeholder = stringResource(R.string.field_fish_in_weighing_example),
                            suffix = stringResource(R.string.unit_fish),
                            keyboardType = KeyboardType.Number,
                            errorText = row.fishCountError?.let { stringResource(it.stringRes()) },
                            enabled = !state.isSaving,
                        )
                        if (state.weighings.size > 1) {
                            LargeButton(
                                text = stringResource(R.string.biometry_remove_weighing, index + 1),
                                icon = Icons.Default.Close,
                                onClick = { onRemoveWeighing(row.id) },
                            )
                        }
                    }
                }
            }
            LargeButton(
                text = stringResource(R.string.biometry_add_weighing),
                icon = Icons.Default.Add,
                onClick = onAddWeighing,
            )
            SummaryPanel(state.summary)
            WarningCard(
                messages = buildList {
                    val summary = state.summary
                    if (state.sampleTooSmall && summary != null) {
                        add(
                            stringResource(
                                R.string.biometry_sample_warning,
                                pluralStringResource(R.plurals.fish_count, summary.fishCount, summary.fishCount),
                                pluralStringResource(R.plurals.fish_count, state.recommendedSample, state.recommendedSample),
                            ),
                        )
                    }
                    state.warnings.forEach { add(stringResource(it.messageRes())) }
                },
            )
            DateTimeField(dateTime = state.dateTime, onDateTimeChange = onDateTimeChange)
            AppTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                label = stringResource(R.string.field_health_notes),
                placeholder = stringResource(R.string.field_health_notes_optional),
                enabled = !state.isSaving,
            )
            LargeButton(
                text = stringResource(if (state.hasWarnings) R.string.action_save_anyway else R.string.action_save),
                icon = Icons.Default.Check,
                onClick = onSave,
                enabled = state.canSave,
                isLoading = state.isSaving,
            )
        }
    }
}

/** Peso médio e totais das pesagens completas, atualizados enquanto o usuário digita. */
@Composable
private fun SummaryPanel(summary: BiometrySummary?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(R.string.biometry_summary_title), style = MaterialTheme.typography.titleMedium)
            if (summary == null) {
                Text(text = stringResource(R.string.biometry_summary_none), style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(
                    text = stringResource(R.string.biometry_summary_average, formatNumber(summary.averageWeightG, 1)),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(
                        R.string.biometry_summary_totals,
                        formatNumber(summary.netWeightKg, 2),
                        pluralStringResource(R.plurals.fish_count, summary.fishCount, summary.fishCount),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

/** Depois de salvar: peso médio medido e a ração sugerida para ele. */
@Composable
private fun BiometryResultPanel(
    result: BiometryResult,
    saved: SaveFeedback,
    onDone: (SaveFeedback) -> Unit,
    onRegisterFeeding: (SaveFeedback) -> Unit,
) {
    Text(
        text = stringResource(R.string.biometry_summary_average, formatNumber(result.averageWeightG, 1)),
        style = MaterialTheme.typography.headlineSmall,
    )
    val suggestion = result.suggestion
    Text(
        text = if (suggestion == null) {
            stringResource(R.string.biometry_result_no_suggestion)
        } else {
            stringResource(
                R.string.biometry_result_suggestion,
                formatNumber(suggestion.dailyKg, 2),
                formatNumber(suggestion.perMealKg, 3),
                suggestion.band.meals,
            )
        },
        style = MaterialTheme.typography.bodyLarge,
    )
    LargeButton(text = stringResource(R.string.lot_register_feeding), icon = Icons.Default.Add, onClick = { onRegisterFeeding(saved) })
    LargeButton(text = stringResource(R.string.action_done), icon = Icons.Default.Check, onClick = { onDone(saved) })
}

private val previewTime: LocalDateTime = LocalDateTime.of(2026, 10, 16, 10, 0)

@Preview(name = "Biometria", showBackground = true, fontScale = 2f)
@Composable
private fun BiometryPreview() {
    AquaGestorTheme {
        BiometryContent(
            state = BiometryUiState(
                loaded = true, especie = Especie.TILAPIA, aliveFish = 1000, dateTime = previewTime,
                weighings = listOf(
                    WeighingInput(0, gross = "6", tare = "1", fishCount = "20"),
                    WeighingInput(1, gross = "4"),
                ),
                summary = BiometrySummary(5.0, 20, 250.0), recommendedSample = 100, sampleTooSmall = true,
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onDone = {}, onRegisterFeeding = {}, onGrossChange = { _, _ -> }, onTareChange = { _, _ -> },
            onFishCountChange = { _, _ -> }, onAddWeighing = {}, onRemoveWeighing = {}, onNotesChange = {}, onDateTimeChange = {}, onSave = {},
        )
    }
}

@Preview(name = "Biometria salva", showBackground = true, fontScale = 2f)
@Composable
private fun BiometryResultPreview() {
    AquaGestorTheme {
        BiometryContent(
            state = BiometryUiState(
                loaded = true, dateTime = previewTime, saved = SaveFeedback.SAVED,
                result = BiometryResult(250.0, FeedingCalculator.suggest(1000, 250.0, WaterReadings(temperatureC = 27.0))),
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onDone = {}, onRegisterFeeding = {}, onGrossChange = { _, _ -> }, onTareChange = { _, _ -> },
            onFishCountChange = { _, _ -> }, onAddWeighing = {}, onRemoveWeighing = {}, onNotesChange = {}, onDateTimeChange = {}, onSave = {},
        )
    }
}
