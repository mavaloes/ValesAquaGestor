package com.valoes.valesaquagestor.presentation.lot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.Arracoamento
import com.valoes.valesaquagestor.domain.model.Biometria
import com.valoes.valesaquagestor.domain.model.ComportamentoPeixe
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import com.valoes.valesaquagestor.domain.rules.AlertItem
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.CompositeAlerts
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.DeleteRecordDialog
import com.valoes.valesaquagestor.presentation.components.DeleteRequest
import com.valoes.valesaquagestor.presentation.components.EmptyMessage
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.RecordRow
import com.valoes.valesaquagestor.presentation.components.SavedMessageEffect
import com.valoes.valesaquagestor.presentation.components.SectionTitle
import com.valoes.valesaquagestor.presentation.components.StatusCard
import com.valoes.valesaquagestor.presentation.components.actionRes
import com.valoes.valesaquagestor.presentation.components.alertValueText
import com.valoes.valesaquagestor.presentation.components.biometryLine
import com.valoes.valesaquagestor.presentation.components.feedingLine
import com.valoes.valesaquagestor.presentation.components.labelRes
import com.valoes.valesaquagestor.presentation.components.messageRes
import com.valoes.valesaquagestor.presentation.components.movementLine
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.formatDateTime
import com.valoes.valesaquagestor.presentation.util.formatNumber
import java.time.Instant
import java.time.ZoneId

/**
 * Tela do lote: peixes vivos, biomassa, peso médio, mortalidade, TCA e alertas, com os atalhos para
 * registrar ração, biometria e mortalidade/despesca, e os últimos registros (com exclusão).
 */
@Composable
fun LotDetailScreen(
    onBack: () -> Unit,
    onRegisterFeeding: (lotId: Long) -> Unit,
    onRegisterBiometry: (lotId: Long) -> Unit,
    onRegisterMovement: (lotId: Long) -> Unit,
    showSavedMessage: Boolean = false,
    viewModel: LotDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    SavedMessageEffect(show = showSavedMessage, hostState = snackbarHostState)
    LotDetailContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRegisterFeeding = onRegisterFeeding,
        onRegisterBiometry = onRegisterBiometry,
        onRegisterMovement = onRegisterMovement,
        onDeleteFeeding = viewModel::deleteFeeding,
        onDeleteBiometry = viewModel::deleteBiometry,
        onDeleteMovement = viewModel::deleteMovement,
    )
}

@Composable
internal fun LotDetailContent(
    state: LotDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRegisterFeeding: (Long) -> Unit = {},
    onRegisterBiometry: (Long) -> Unit = {},
    onRegisterMovement: (Long) -> Unit = {},
    onDeleteFeeding: (Long) -> Unit = {},
    onDeleteBiometry: (Long) -> Unit = {},
    onDeleteMovement: (Long) -> Unit = {},
) {
    val lot = state.lot
    var pendingDelete by remember { mutableStateOf<DeleteRequest?>(null) }
    DeleteRecordDialog(request = pendingDelete, onDismiss = { pendingDelete = null })
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { FeedbackSnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = if (lot == null) {
                    stringResource(R.string.lot_form_title)
                } else {
                    stringResource(R.string.lot_detail_title, stringResource(lot.lote.especie.labelRes()), stringResource(lot.lote.fase.labelRes()))
                },
                onBack = onBack,
                help = HelpTips.LotDetail,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (lot == null) {
                if (!state.isLoading) EmptyMessage(text = stringResource(R.string.lot_not_found))
                return@Column
            }
            val zone = remember { ZoneId.systemDefault() }
            val lotId = lot.lote.id
            Text(text = stringResource(R.string.lot_in_tank, lot.tankName), style = MaterialTheme.typography.bodyLarge)

            LargeButton(text = stringResource(R.string.lot_register_feeding), icon = Icons.Default.Add, onClick = { onRegisterFeeding(lotId) })
            LargeButton(text = stringResource(R.string.register_biometry), icon = Icons.Default.Add, onClick = { onRegisterBiometry(lotId) })
            LargeButton(text = stringResource(R.string.register_movement), icon = Icons.Default.Add, onClick = { onRegisterMovement(lotId) })

            Metric(
                label = stringResource(R.string.lot_alive),
                value = pluralStringResource(R.plurals.fish_count, lot.currentQuantity, lot.currentQuantity),
            )
            Metric(
                label = stringResource(R.string.lot_biomass),
                value = stringResource(R.string.value_with_unit, formatNumber(lot.biomassKg, 1), stringResource(R.string.unit_kg)),
            )
            Metric(
                label = stringResource(R.string.lot_average_weight),
                value = stringResource(R.string.value_with_unit, formatNumber(lot.lote.pesoMedioAtualG, 1), stringResource(R.string.unit_g)),
            )
            Metric(
                label = stringResource(R.string.lot_mortality),
                value = stringResource(R.string.value_with_unit, formatNumber(lot.mortalityPct, 1), stringResource(R.string.unit_percent)),
            )
            Metric(
                label = stringResource(R.string.lot_fcr),
                value = lot.fcr?.let { formatNumber(it, 2) } ?: stringResource(R.string.lot_fcr_none),
            )

            SectionTitle(text = stringResource(R.string.lot_alerts_title))
            if (lot.alerts.isEmpty()) EmptyMessage(text = stringResource(R.string.lot_no_alerts))
            lot.alerts.forEach { alert ->
                StatusCard(
                    title = stringResource(alert.kind.labelRes()),
                    valueText = alertValueText(alert.kind, checkNotNull(alert.value)),
                    meaning = stringResource(alert.status.messageRes()),
                    action = stringResource(alert.status.actionRes()),
                    level = alert.status.level,
                    footnote = alert.measuredAt?.let { stringResource(R.string.measured_at, formatDateTime(it, zone)) },
                )
            }

            SectionTitle(text = stringResource(R.string.lot_recent_title))
            if (lot.recentFeedings.isEmpty() && lot.recentBiometries.isEmpty() && lot.recentMovements.isEmpty()) {
                EmptyMessage(text = stringResource(R.string.lot_recent_empty))
            }
            lot.recentFeedings.forEach { feeding ->
                val line = feedingLine(feeding, zone)
                RecordRow(text = line, onDelete = { pendingDelete = DeleteRequest(line, null) { onDeleteFeeding(feeding.id) } })
            }
            val biometryNote = stringResource(R.string.dialog_delete_note_biometry)
            lot.recentBiometries.forEach { biometry ->
                val line = biometryLine(biometry, zone)
                RecordRow(text = line, onDelete = { pendingDelete = DeleteRequest(line, biometryNote) { onDeleteBiometry(biometry.id) } })
            }
            val finalNote = stringResource(R.string.dialog_delete_note_final)
            lot.recentMovements.forEach { movement ->
                val line = movementLine(movement, zone)
                val note = if (movement.tipo == TipoMovimentacao.DESPESCA_FINAL) finalNote else null
                RecordRow(text = line, onDelete = { pendingDelete = DeleteRequest(line, note) { onDeleteMovement(movement.id) } })
            }
        }
    }
}

/** Um número do lote: rótulo pequeno em cima e valor grande embaixo. */
@Composable
private fun Metric(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.titleMedium)
                Text(text = value, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Preview(name = "Detalhe do lote", showBackground = true, fontScale = 2f)
@Composable
private fun LotDetailPreview() {
    val lote = Lote(
        id = 1, tanqueId = 1, especie = Especie.TILAPIA, fase = FaseLote.ENGORDA, quantidadeInicial = 1000,
        pesoMedioInicialG = 20.0, dataPovoamento = Instant.EPOCH, pesoMedioAtualG = 250.0,
    )
    val status = CompositeAlerts.accumulatedMortality(1000, 120)!!
    val at = Instant.parse("2026-10-16T09:10:00Z")
    AquaGestorTheme {
        LotDetailContent(
            state = LotDetailUiState(
                isLoading = false,
                lot = LotDetailData(
                    lote = lote, tankName = "Viveiro 1", currentQuantity = 880, biomassKg = 220.0, mortalityPct = 12.0,
                    fcr = 1.42, hasBiometry = true,
                    alerts = listOf(AlertItem(1, "Viveiro 1", 1, AlertKind.MORTALITY, 12.0, status, null)),
                    recentFeedings = listOf(
                        Arracoamento(id = 1, loteId = 1, dataHora = at, quantidadeKg = 1.5, comportamento = ComportamentoPeixe.NORMAL),
                    ),
                    recentBiometries = listOf(Biometria(id = 1, loteId = 1, dataHora = at, pesoAmostraKg = 5.0, numeroPeixesAmostra = 20)),
                    recentMovements = listOf(
                        MovimentacaoLote(id = 1, loteId = 1, dataHora = at, tipo = TipoMovimentacao.MORTALIDADE, quantidade = 12),
                    ),
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
        )
    }
}
