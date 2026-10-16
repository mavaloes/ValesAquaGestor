package com.valoes.valesaquagestor.presentation.record

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
import androidx.compose.runtime.LaunchedEffect
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
import com.valoes.valesaquagestor.domain.calc.FeedWarning
import com.valoes.valesaquagestor.domain.calc.FeedingCalculator
import com.valoes.valesaquagestor.domain.calc.WaterReadings
import com.valoes.valesaquagestor.domain.model.ComportamentoPeixe
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.MotivoSuspensao
import com.valoes.valesaquagestor.presentation.components.AppTextField
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.ChoiceGroup
import com.valoes.valesaquagestor.presentation.components.DateTimeField
import com.valoes.valesaquagestor.presentation.components.FeedbackEffect
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.SwitchRow
import com.valoes.valesaquagestor.presentation.components.WarningCard
import com.valoes.valesaquagestor.presentation.components.labelRes
import com.valoes.valesaquagestor.presentation.components.requiredLabel
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.SaveFeedback
import com.valoes.valesaquagestor.presentation.util.formatNumber
import com.valoes.valesaquagestor.presentation.util.stringRes
import java.time.LocalDateTime

/**
 * Registro de ração. Já vem com a quantidade sugerida pela tabela, o horário atual e a marca, a
 * proteína e o pellet da última ração. No caso comum, abrir e tocar em Salvar.
 *
 * @param onDone chamado quando o trato foi salvo, com o aviso certo.
 */
@Composable
fun FeedingRecordScreen(
    onBack: () -> Unit,
    onDone: (SaveFeedback) -> Unit,
    viewModel: FeedingRecordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.saved) { state.saved?.let(onDone) }
    FeedbackEffect(
        message = if (state.saveFailed) stringResource(R.string.message_unknown_error) else null,
        hostState = snackbarHostState,
        onShown = viewModel::onSaveFailedShown,
    )
    FeedingRecordContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onQuantityChange = viewModel::onQuantityChange,
        onDateTimeChange = viewModel::onDateTimeChange,
        onSuspendedChange = viewModel::onSuspendedChange,
        onReasonChange = viewModel::onReasonChange,
        onBrandChange = viewModel::onBrandChange,
        onProteinChange = viewModel::onProteinChange,
        onPelletChange = viewModel::onPelletChange,
        onBehaviorChange = viewModel::onBehaviorChange,
        onLeftoverChange = viewModel::onLeftoverChange,
        onSave = viewModel::onSave,
    )
}

@Composable
internal fun FeedingRecordContent(
    state: FeedingUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onDateTimeChange: (LocalDateTime) -> Unit,
    onSuspendedChange: (Boolean) -> Unit,
    onReasonChange: (MotivoSuspensao) -> Unit,
    onBrandChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onPelletChange: (String) -> Unit,
    onBehaviorChange: (ComportamentoPeixe) -> Unit,
    onLeftoverChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.feeding_title), onBack = onBack) },
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
            state.especie?.let { especie ->
                Text(
                    text = stringResource(R.string.feeding_for_lot, stringResource(R.string.lot_detail_title, stringResource(especie.labelRes()), state.tankName)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Text(text = stringResource(R.string.form_required_legend), style = MaterialTheme.typography.bodyMedium)
            SuggestionPanel(state)

            if (!state.suspended) {
                AppTextField(
                    value = state.quantity,
                    onValueChange = onQuantityChange,
                    label = requiredLabel(stringResource(R.string.field_feed_quantity)),
                    placeholder = stringResource(R.string.field_feed_quantity_example),
                    suffix = stringResource(R.string.unit_kg),
                    keyboardType = KeyboardType.Decimal,
                    errorText = state.quantityError?.let { stringResource(it.stringRes()) },
                    enabled = !state.isSaving,
                )
            }
            DateTimeField(dateTime = state.dateTime, onDateTimeChange = onDateTimeChange)
            SwitchRow(label = stringResource(R.string.field_suspended), checked = state.suspended, onCheckedChange = onSuspendedChange)
            if (state.suspended) {
                ChoiceGroup(
                    label = requiredLabel(stringResource(R.string.field_suspension_reason)),
                    options = MotivoSuspensao.entries,
                    selected = state.reason,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelect = onReasonChange,
                )
                state.reasonError?.let {
                    Text(text = stringResource(it.stringRes()), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
            AppTextField(
                value = state.brand,
                onValueChange = onBrandChange,
                label = stringResource(R.string.field_brand),
                placeholder = stringResource(R.string.field_brand_optional),
                enabled = !state.isSaving,
            )
            AppTextField(
                value = state.protein,
                onValueChange = onProteinChange,
                label = stringResource(R.string.field_protein),
                placeholder = stringResource(R.string.field_protein_example),
                suffix = stringResource(R.string.unit_percent),
                keyboardType = KeyboardType.Decimal,
                errorText = state.proteinError?.let { stringResource(it.stringRes()) },
                enabled = !state.isSaving,
            )
            AppTextField(
                value = state.pellet,
                onValueChange = onPelletChange,
                label = stringResource(R.string.field_pellet),
                placeholder = stringResource(R.string.field_pellet_example),
                suffix = stringResource(R.string.unit_mm),
                keyboardType = KeyboardType.Decimal,
                errorText = state.pelletError?.let { stringResource(it.stringRes()) },
                enabled = !state.isSaving,
            )
            ChoiceGroup(
                label = stringResource(R.string.field_behavior),
                options = ComportamentoPeixe.entries,
                selected = state.behavior,
                optionLabel = { stringResource(it.labelRes()) },
                onSelect = onBehaviorChange,
            )
            SwitchRow(label = stringResource(R.string.field_leftover), checked = state.leftover, onCheckedChange = onLeftoverChange)
            LargeButton(
                text = stringResource(R.string.action_save),
                icon = Icons.Default.Check,
                onClick = onSave,
                enabled = state.canSave,
                isLoading = state.isSaving,
            )
        }
    }
}

/** Sugestão da tabela e os avisos: água fora do limite para alimentar, oxigênio no limite, sobra anterior. */
@Composable
private fun SuggestionPanel(state: FeedingUiState) {
    val suggestion = state.suggestion
    if (state.noTable) {
        Text(text = stringResource(R.string.feeding_no_table), style = MaterialTheme.typography.bodyLarge)
    }
    if (suggestion != null) {
        Text(
            text = stringResource(
                R.string.feeding_suggestion,
                formatNumber(suggestion.perMealKg, 3),
                formatNumber(suggestion.dailyKg, 2),
                suggestion.band.meals,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    val messages = buildList {
        if (suggestion != null && suggestion.blockReasons.isNotEmpty()) {
            add(stringResource(R.string.feeding_block_title))
            suggestion.blockReasons.forEach { add(stringResource(it.labelRes())) }
        }
        suggestion?.warnings?.forEach {
            when (it) {
                FeedWarning.OXYGEN_ATTENTION -> add(stringResource(R.string.feeding_warning_oxygen))
                FeedWarning.TEMPERATURE_MISSING -> add(stringResource(R.string.feeding_warning_temperature_missing))
            }
        }
        if (state.lastHadLeftover) add(stringResource(R.string.feeding_leftover_warning))
    }
    WarningCard(messages = messages)
}

private val previewTime: LocalDateTime = LocalDateTime.of(2026, 10, 16, 10, 0)

@Preview(name = "Registrar ração", showBackground = true, fontScale = 2f)
@Composable
private fun FeedingRecordPreview() {
    AquaGestorTheme {
        FeedingRecordContent(
            state = FeedingUiState(
                loaded = true, especie = Especie.TILAPIA, tankName = "Viveiro 1", dateTime = previewTime,
                suggestion = FeedingCalculator.suggest(500, 43.0, WaterReadings(oxygenMgL = 3.5, temperatureC = 27.0)),
                quantity = "0,323", brand = "Marca X", protein = "36", pellet = "3",
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onQuantityChange = {}, onDateTimeChange = {}, onSuspendedChange = {}, onReasonChange = {},
            onBrandChange = {}, onProteinChange = {}, onPelletChange = {}, onBehaviorChange = {}, onLeftoverChange = {}, onSave = {},
        )
    }
}

@Preview(name = "Registrar ração, água ruim", showBackground = true, fontScale = 2f)
@Composable
private fun FeedingRecordBlockedPreview() {
    AquaGestorTheme {
        FeedingRecordContent(
            state = FeedingUiState(
                loaded = true, especie = Especie.TILAPIA, tankName = "Viveiro 1", dateTime = previewTime,
                suggestion = FeedingCalculator.suggest(500, 43.0, WaterReadings(oxygenMgL = 2.5, temperatureC = 27.0)),
                suspended = true, reason = MotivoSuspensao.OXIGENIO_BAIXO,
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onQuantityChange = {}, onDateTimeChange = {}, onSuspendedChange = {}, onReasonChange = {},
            onBrandChange = {}, onProteinChange = {}, onPelletChange = {}, onBehaviorChange = {}, onLeftoverChange = {}, onSave = {},
        )
    }
}
