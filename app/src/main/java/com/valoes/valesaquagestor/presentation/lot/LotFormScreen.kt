package com.valoes.valesaquagestor.presentation.lot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.calc.CapacityCalculator
import com.valoes.valesaquagestor.domain.calc.CapacityCheck
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.presentation.components.AppTextField
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.ChoiceGroup
import com.valoes.valesaquagestor.presentation.components.DateField
import com.valoes.valesaquagestor.presentation.components.FeedbackEffect
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.labelRes
import com.valoes.valesaquagestor.presentation.components.requiredLabel
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.formatNumber
import com.valoes.valesaquagestor.presentation.util.stringRes
import java.time.LocalDate

/** Formulário de novo lote. Mostra a capacidade do viveiro e avisa se o povoamento passar dela. */
@Composable
fun LotFormScreen(
    onBack: () -> Unit,
    onSaved: (lotId: Long) -> Unit,
    viewModel: LotFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    state.savedLotId?.let { id -> LaunchedEffect(id) { onSaved(id) } }
    FeedbackEffect(
        message = if (state.saveFailed) stringResource(R.string.message_unknown_error) else null,
        hostState = snackbarHostState,
        onShown = viewModel::onSaveFailedShown,
    )
    LotFormContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSpeciesChange = viewModel::onSpeciesChange,
        onPhaseChange = viewModel::onPhaseChange,
        onQuantityChange = viewModel::onQuantityChange,
        onInitialWeightChange = viewModel::onInitialWeightChange,
        onFinalWeightChange = viewModel::onFinalWeightChange,
        onOriginChange = viewModel::onOriginChange,
        onStockingDateChange = viewModel::onStockingDateChange,
        onSave = viewModel::onSave,
    )
}

@Composable
internal fun LotFormContent(
    state: LotFormUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSpeciesChange: (Especie) -> Unit,
    onPhaseChange: (FaseLote) -> Unit,
    onQuantityChange: (String) -> Unit,
    onInitialWeightChange: (String) -> Unit,
    onFinalWeightChange: (String) -> Unit,
    onOriginChange: (String) -> Unit,
    onStockingDateChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.lot_form_title), onBack = onBack) },
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
            Text(text = stringResource(R.string.form_required_legend), style = MaterialTheme.typography.bodyMedium)
            ChoiceGroup(
                label = requiredLabel(stringResource(R.string.field_species)),
                options = Especie.entries,
                selected = state.species,
                optionLabel = { stringResource(it.labelRes()) },
                onSelect = onSpeciesChange,
            )
            ChoiceGroup(
                label = requiredLabel(stringResource(R.string.field_phase)),
                options = FaseLote.entries,
                selected = state.phase,
                optionLabel = { stringResource(it.labelRes()) },
                onSelect = onPhaseChange,
            )
            AppTextField(
                value = state.quantity,
                onValueChange = onQuantityChange,
                label = requiredLabel(stringResource(R.string.field_quantity)),
                placeholder = stringResource(R.string.field_quantity_example),
                suffix = stringResource(R.string.unit_fish),
                keyboardType = KeyboardType.Number,
                errorText = state.quantityError?.let { stringResource(it.stringRes()) },
                enabled = !state.isSaving,
            )
            AppTextField(
                value = state.initialWeight,
                onValueChange = onInitialWeightChange,
                label = requiredLabel(stringResource(R.string.field_initial_weight)),
                placeholder = stringResource(R.string.field_initial_weight_example),
                suffix = stringResource(R.string.unit_g),
                keyboardType = KeyboardType.Decimal,
                errorText = state.initialWeightError?.let { stringResource(it.stringRes()) },
                enabled = !state.isSaving,
            )
            if (state.capacityApplies) {
                val typical = CapacityCalculator.typicalFinalWeightG(state.phase)
                AppTextField(
                    value = state.finalWeight,
                    onValueChange = onFinalWeightChange,
                    label = requiredLabel(stringResource(R.string.field_final_weight)),
                    placeholder = if (typical != null) {
                        stringResource(
                            R.string.field_final_weight_example_range,
                            formatNumber(typical.start, 0),
                            formatNumber(typical.endInclusive, 0),
                        )
                    } else {
                        stringResource(R.string.field_final_weight_example_market)
                    },
                    suffix = stringResource(R.string.unit_g),
                    keyboardType = KeyboardType.Decimal,
                    errorText = state.finalWeightError?.let { stringResource(it.stringRes()) },
                    enabled = !state.isSaving,
                )
            }
            CapacityPanel(state)
            DateField(
                label = stringResource(R.string.field_stocking_date),
                date = state.stockingDate,
                onDateChange = onStockingDateChange,
            )
            AppTextField(
                value = state.origin,
                onValueChange = onOriginChange,
                label = stringResource(R.string.field_origin),
                placeholder = stringResource(R.string.field_origin_optional),
                imeAction = ImeAction.Done,
                enabled = !state.isSaving,
            )
            LargeButton(
                text = stringResource(if (state.exceedsCapacity) R.string.action_save_anyway else R.string.action_save),
                icon = Icons.Default.Check,
                onClick = onSave,
                enabled = state.canSave,
                isLoading = state.isSaving,
            )
        }
    }
}

/** Capacidade do viveiro e o aviso quando o povoamento passa dela (aviso, não bloqueio). */
@Composable
private fun CapacityPanel(state: LotFormUiState) {
    val capacity: CapacityCheck? = state.capacity
    when {
        !state.tankLoaded -> Unit
        !state.capacityApplies -> Text(
            text = stringResource(R.string.capacity_unavailable),
            style = MaterialTheme.typography.bodyMedium,
        )

        capacity != null -> Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (capacity.exceeds) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (capacity.exceeds) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = stringResource(R.string.capacity_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(
                        R.string.capacity_info,
                        formatNumber(capacity.capacityKg, 0),
                        formatNumber(capacity.range.minKg, 0),
                        formatNumber(capacity.range.maxKg, 0),
                        pluralStringResource(R.plurals.fish_count, capacity.maxFish, capacity.maxFish),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (capacity.exceeds) {
                    androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.level_atencao),
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                        Text(
                            text = stringResource(
                                R.string.capacity_warning,
                                pluralStringResource(R.plurals.fish_count, capacity.maxFish, capacity.maxFish),
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Novo lote com aviso", showBackground = true, fontScale = 2f)
@Composable
private fun LotFormPreview() {
    val check = CapacityCheck(
        condition = com.valoes.valesaquagestor.domain.calc.RearingCondition.NO_AERATOR_NO_EXCHANGE,
        range = com.valoes.valesaquagestor.domain.calc.CapacityRange(100.0, 120.0),
        capacityKg = 27.0,
        maxFish = 1350,
        exceeds = true,
    )
    AquaGestorTheme {
        LotFormContent(
            state = LotFormUiState(
                tankName = "Viveiro 1", tankLoaded = true, capacityApplies = true, quantity = "2000",
                initialWeight = "5", finalWeight = "20", capacity = check, stockingDate = LocalDate.of(2026, 10, 16),
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onSpeciesChange = {}, onPhaseChange = {}, onQuantityChange = {}, onInitialWeightChange = {},
            onFinalWeightChange = {}, onOriginChange = {}, onStockingDateChange = {}, onSave = {},
        )
    }
}
