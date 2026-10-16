package com.valoes.valesaquagestor.presentation.tank

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.TipoTanque
import com.valoes.valesaquagestor.presentation.components.AppTextField
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.ChoiceGroup
import com.valoes.valesaquagestor.presentation.components.FeedbackEffect
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.SwitchRow
import com.valoes.valesaquagestor.presentation.components.labelRes
import com.valoes.valesaquagestor.presentation.components.requiredLabel
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.FormError
import com.valoes.valesaquagestor.presentation.util.stringRes

/** Formulário de novo tanque. Ao salvar com sucesso, chama [onSaved] para voltar à lista. */
@Composable
fun TankFormScreen(
    onBack: () -> Unit,
    onSaved: (tankId: Long) -> Unit,
    viewModel: TankFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    state.savedTankId?.let { id -> LaunchedEffect(id) { onSaved(id) } }
    FeedbackEffect(
        message = if (state.saveFailed) stringResource(R.string.message_unknown_error) else null,
        hostState = snackbarHostState,
        onShown = viewModel::onSaveFailedShown,
    )
    TankFormContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onTypeChange = viewModel::onTypeChange,
        onAreaChange = viewModel::onAreaChange,
        onDepthChange = viewModel::onDepthChange,
        onVolumeChange = viewModel::onVolumeChange,
        onHasAeratorChange = viewModel::onHasAeratorChange,
        onAeratorPowerChange = viewModel::onAeratorPowerChange,
        onRenewalChange = viewModel::onRenewalChange,
        onSave = viewModel::onSave,
    )
}

@Composable
internal fun TankFormContent(
    state: TankFormUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (TipoTanque) -> Unit,
    onAreaChange: (String) -> Unit,
    onDepthChange: (String) -> Unit,
    onVolumeChange: (String) -> Unit,
    onHasAeratorChange: (Boolean) -> Unit,
    onAeratorPowerChange: (String) -> Unit,
    onRenewalChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.tank_form_title), onBack = onBack) },
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
            Text(text = stringResource(R.string.form_required_legend), style = MaterialTheme.typography.bodyMedium)
            AppTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = requiredLabel(stringResource(R.string.field_tank_name)),
                placeholder = stringResource(R.string.field_tank_name_example),
                errorText = state.nameError?.let { stringResource(it.stringRes()) },
                enabled = !state.isSaving,
            )
            ChoiceGroup(
                label = requiredLabel(stringResource(R.string.field_tank_type)),
                options = TipoTanque.entries,
                selected = state.type,
                optionLabel = { stringResource(it.labelRes()) },
                onSelect = onTypeChange,
            )
            AppTextField(
                value = state.area,
                onValueChange = onAreaChange,
                label = if (state.usesArea) requiredLabel(stringResource(R.string.field_area)) else stringResource(R.string.field_area),
                placeholder = stringResource(R.string.field_area_example),
                suffix = stringResource(R.string.unit_m2),
                keyboardType = KeyboardType.Decimal,
                errorText = state.areaError?.let { stringResource(it.stringRes()) },
                enabled = !state.isSaving,
            )
            AppTextField(
                value = state.depth,
                onValueChange = onDepthChange,
                label = stringResource(R.string.field_depth),
                placeholder = stringResource(R.string.field_depth_example),
                suffix = stringResource(R.string.unit_m),
                keyboardType = KeyboardType.Decimal,
                errorText = state.depthError?.let { stringResource(it.stringRes()) },
                enabled = !state.isSaving,
            )
            if (!state.usesArea) {
                AppTextField(
                    value = state.volume,
                    onValueChange = onVolumeChange,
                    label = requiredLabel(stringResource(R.string.field_volume)),
                    placeholder = stringResource(R.string.field_volume_example),
                    suffix = stringResource(R.string.unit_m3),
                    keyboardType = KeyboardType.Decimal,
                    errorText = state.volumeError?.let { stringResource(it.stringRes()) },
                    enabled = !state.isSaving,
                )
            }
            SwitchRow(
                label = stringResource(R.string.field_has_aerator),
                checked = state.hasAerator,
                onCheckedChange = onHasAeratorChange,
            )
            if (state.hasAerator) {
                AppTextField(
                    value = state.aeratorPower,
                    onValueChange = onAeratorPowerChange,
                    label = stringResource(R.string.field_aerator_power),
                    placeholder = stringResource(R.string.field_aerator_power_example),
                    suffix = stringResource(R.string.unit_hp),
                    keyboardType = KeyboardType.Decimal,
                    errorText = state.aeratorPowerError?.let { stringResource(it.stringRes()) },
                    enabled = !state.isSaving,
                )
            }
            AppTextField(
                value = state.renewal,
                onValueChange = onRenewalChange,
                label = requiredLabel(stringResource(R.string.field_renewal)),
                placeholder = stringResource(R.string.field_renewal_example),
                suffix = stringResource(R.string.unit_percent_per_day),
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
                onImeAction = { if (state.canSave) onSave() },
                errorText = state.renewalError?.let { stringResource(it.stringRes()) },
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

@Preview(name = "Novo tanque", showBackground = true, fontScale = 2f)
@Composable
private fun TankFormPreview() {
    AquaGestorTheme {
        TankFormContent(
            state = TankFormUiState(name = "Viveiro 1", area = "1000", renewal = "5", hasAerator = true),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onNameChange = {}, onTypeChange = {}, onAreaChange = {}, onDepthChange = {},
            onVolumeChange = {}, onHasAeratorChange = {}, onAeratorPowerChange = {}, onRenewalChange = {}, onSave = {},
        )
    }
}

@Preview(name = "Novo tanque com erros", showBackground = true, fontScale = 2f)
@Composable
private fun TankFormErrorPreview() {
    AquaGestorTheme {
        TankFormContent(
            state = TankFormUiState(
                type = TipoTanque.TANQUE_REDE,
                nameError = FormError.REQUIRED,
                volumeError = FormError.INVALID_NUMBER,
                renewalError = FormError.OUT_OF_RANGE_PERCENT,
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onNameChange = {}, onTypeChange = {}, onAreaChange = {}, onDepthChange = {},
            onVolumeChange = {}, onHasAeratorChange = {}, onAeratorPowerChange = {}, onRenewalChange = {}, onSave = {},
        )
    }
}
