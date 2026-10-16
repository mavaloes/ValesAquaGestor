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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import com.valoes.valesaquagestor.domain.rules.CompositeAlerts
import com.valoes.valesaquagestor.presentation.components.AppTextField
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.ChoiceGroup
import com.valoes.valesaquagestor.presentation.components.ConfirmDialog
import com.valoes.valesaquagestor.presentation.components.DateTimeField
import com.valoes.valesaquagestor.presentation.components.FeedbackEffect
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.WarningCard
import com.valoes.valesaquagestor.presentation.components.actionRes
import com.valoes.valesaquagestor.presentation.components.labelRes
import com.valoes.valesaquagestor.presentation.components.messageRes
import com.valoes.valesaquagestor.presentation.components.requiredLabel
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.SaveFeedback
import com.valoes.valesaquagestor.presentation.util.formatNumber
import com.valoes.valesaquagestor.presentation.util.stringRes
import java.time.LocalDateTime

/**
 * Mortalidade e despesca (parcial ou final) em fluxo rápido: escolher o que aconteceu, informar a
 * quantidade e salvar. A despesca final pede confirmação, porque encerra o lote.
 *
 * @param onDone chamado quando o registro foi salvo, com o aviso certo.
 */
@Composable
fun MovementScreen(
    onBack: () -> Unit,
    onDone: (SaveFeedback) -> Unit,
    viewModel: MovementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.saved) { state.saved?.let(onDone) }
    FeedbackEffect(
        message = if (state.saveFailed) stringResource(R.string.message_unknown_error) else null,
        hostState = snackbarHostState,
        onShown = viewModel::onSaveFailedShown,
    )
    MovementContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onTypeChange = viewModel::onTypeChange,
        onQuantityChange = viewModel::onQuantityChange,
        onTotalWeightChange = viewModel::onTotalWeightChange,
        onNotesChange = viewModel::onNotesChange,
        onDateTimeChange = viewModel::onDateTimeChange,
        onSave = viewModel::onSave,
        onConfirmFinal = viewModel::onConfirmFinal,
        onDismissFinal = viewModel::onDismissFinal,
    )
}

@Composable
internal fun MovementContent(
    state: MovementUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onTypeChange: (TipoMovimentacao) -> Unit,
    onQuantityChange: (String) -> Unit,
    onTotalWeightChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDateTimeChange: (LocalDateTime) -> Unit,
    onSave: () -> Unit,
    onConfirmFinal: () -> Unit,
    onDismissFinal: () -> Unit,
) {
    val fishText = pluralStringResource(R.plurals.fish_count, state.aliveFish, state.aliveFish)
    if (state.confirmFinal) {
        ConfirmDialog(
            title = stringResource(R.string.movement_confirm_final_title),
            text = stringResource(R.string.movement_confirm_final_text, fishText),
            confirmText = stringResource(R.string.movement_confirm_final_button),
            onConfirm = onConfirmFinal,
            onDismiss = onDismissFinal,
        )
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.movement_title), onBack = onBack, help = HelpTips.Movement) },
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
            Text(text = stringResource(R.string.lot_in_tank, state.tankName), style = MaterialTheme.typography.bodyLarge)
            Text(text = stringResource(R.string.movement_alive, fishText), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(R.string.form_required_legend), style = MaterialTheme.typography.bodyMedium)
            ChoiceGroup(
                label = stringResource(R.string.field_movement_type),
                options = MovementViewModel.TYPES,
                selected = state.type,
                optionLabel = { stringResource(it.labelRes()) },
                onSelect = onTypeChange,
            )
            if (state.quantityLocked) {
                Text(text = stringResource(R.string.movement_final_all, fishText), style = MaterialTheme.typography.bodyLarge)
            } else {
                AppTextField(
                    value = state.quantity,
                    onValueChange = onQuantityChange,
                    label = requiredLabel(stringResource(R.string.field_movement_quantity)),
                    placeholder = stringResource(R.string.field_movement_quantity_example),
                    suffix = stringResource(R.string.unit_fish),
                    keyboardType = KeyboardType.Number,
                    errorText = state.quantityError?.let { stringResource(it.stringRes()) },
                    enabled = !state.isSaving,
                )
            }
            val projected = state.projectedMortalityPct
            if (projected != null) {
                Text(
                    text = stringResource(
                        R.string.movement_projected_mortality,
                        stringResource(R.string.value_with_unit, formatNumber(projected, 1), stringResource(R.string.unit_percent)),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            state.mortalityWarning?.let { status ->
                WarningCard(messages = listOf(stringResource(status.messageRes()), stringResource(status.actionRes())))
            }
            if (state.type != TipoMovimentacao.MORTALIDADE) {
                AppTextField(
                    value = state.totalWeight,
                    onValueChange = onTotalWeightChange,
                    label = stringResource(R.string.field_total_weight),
                    placeholder = stringResource(R.string.field_total_weight_example),
                    suffix = stringResource(R.string.unit_kg),
                    keyboardType = KeyboardType.Decimal,
                    errorText = state.weightError?.let { stringResource(it.stringRes()) },
                    enabled = !state.isSaving,
                )
            }
            DateTimeField(dateTime = state.dateTime, onDateTimeChange = onDateTimeChange)
            AppTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                label = stringResource(R.string.field_notes),
                placeholder = stringResource(R.string.field_notes_optional),
                enabled = !state.isSaving,
            )
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

private val previewTime: LocalDateTime = LocalDateTime.of(2026, 10, 16, 8, 0)

@Preview(name = "Mortalidade com alerta", showBackground = true, fontScale = 2f)
@Composable
private fun MovementPreview() {
    AquaGestorTheme {
        MovementContent(
            state = MovementUiState(
                loaded = true, aliveFish = 900, deadSoFar = 90, initialQuantity = 1000, quantity = "20", dateTime = previewTime,
                projectedMortalityPct = 11.0, mortalityWarning = CompositeAlerts.accumulatedMortality(1000, 110),
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onTypeChange = {}, onQuantityChange = {}, onTotalWeightChange = {}, onNotesChange = {},
            onDateTimeChange = {}, onSave = {}, onConfirmFinal = {}, onDismissFinal = {},
        )
    }
}

@Preview(name = "Despesca final", showBackground = true, fontScale = 2f)
@Composable
private fun MovementFinalPreview() {
    AquaGestorTheme {
        MovementContent(
            state = MovementUiState(
                loaded = true, aliveFish = 900, initialQuantity = 1000, type = TipoMovimentacao.DESPESCA_FINAL,
                quantity = "900", dateTime = previewTime,
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onTypeChange = {}, onQuantityChange = {}, onTotalWeightChange = {}, onNotesChange = {},
            onDateTimeChange = {}, onSave = {}, onConfirmFinal = {}, onDismissFinal = {},
        )
    }
}
