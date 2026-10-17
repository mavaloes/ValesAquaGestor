package com.valoes.valesaquagestor.presentation.tank

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.LatestWaterReadings
import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.model.Reading
import com.valoes.valesaquagestor.domain.model.Tanque
import com.valoes.valesaquagestor.domain.model.TipoTanque
import com.valoes.valesaquagestor.domain.rules.WaterReadingsEvaluator
import com.valoes.valesaquagestor.domain.usecase.LotOverview
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.DeleteRecordDialog
import com.valoes.valesaquagestor.presentation.components.DeleteRequest
import com.valoes.valesaquagestor.presentation.components.EmptyMessage
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.LinkButton
import com.valoes.valesaquagestor.presentation.components.RecordRow
import com.valoes.valesaquagestor.presentation.components.SavedMessageEffect
import com.valoes.valesaquagestor.presentation.components.SectionTitle
import com.valoes.valesaquagestor.presentation.components.StatusCard
import com.valoes.valesaquagestor.presentation.components.actionRes
import com.valoes.valesaquagestor.presentation.components.alertValueText
import com.valoes.valesaquagestor.presentation.components.labelRes
import com.valoes.valesaquagestor.presentation.components.messageRes
import com.valoes.valesaquagestor.presentation.components.waterLine
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.formatDateTime
import com.valoes.valesaquagestor.presentation.util.formatNumber
import java.time.Instant
import java.time.ZoneId

/** Tela do tanque: últimas medições com [StatusCard], lotes ativos e atalhos para registrar água e ração. */
@Composable
fun TankDetailScreen(
    onBack: () -> Unit,
    onOpenLot: (Long) -> Unit,
    onNewLot: (tankId: Long) -> Unit,
    onRegisterWater: (tankId: Long) -> Unit,
    onRegisterFeeding: (lotId: Long) -> Unit,
    onShowQr: (tankId: Long) -> Unit,
    showSavedMessage: Boolean = false,
    viewModel: TankDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    SavedMessageEffect(show = showSavedMessage, hostState = snackbarHostState)
    TankDetailContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onOpenLot = onOpenLot,
        onNewLot = onNewLot,
        onRegisterWater = onRegisterWater,
        onRegisterFeeding = onRegisterFeeding,
        onShowQr = onShowQr,
        onDeleteWaterRecord = viewModel::deleteWaterRecord,
    )
}

@Composable
internal fun TankDetailContent(
    state: TankDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenLot: (Long) -> Unit,
    onNewLot: (Long) -> Unit,
    onRegisterWater: (Long) -> Unit,
    onRegisterFeeding: (Long) -> Unit,
    onShowQr: (Long) -> Unit = {},
    onDeleteWaterRecord: (Long) -> Unit = {},
) {
    val tank = state.tank
    var pendingDelete by remember { mutableStateOf<DeleteRequest?>(null) }
    DeleteRecordDialog(request = pendingDelete, onDismiss = { pendingDelete = null })
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = tank?.tanque?.nome ?: stringResource(R.string.tanks_title), onBack = onBack, help = HelpTips.TankDetail) },
        snackbarHost = { FeedbackSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (tank == null) {
                if (!state.isLoading) EmptyMessage(text = stringResource(R.string.tank_not_found))
                return@Column
            }
            val zone = remember { ZoneId.systemDefault() }
            val tankId = tank.tanque.id

            LargeButton(
                text = stringResource(R.string.tank_register_water),
                icon = Icons.Default.Add,
                onClick = { onRegisterWater(tankId) },
            )

            SectionTitle(text = stringResource(R.string.tank_detail_water_title))
            if (tank.waterAlerts.isEmpty() && !tank.ammoniaNeedsPhAndTemperature) {
                EmptyMessage(text = stringResource(R.string.tank_detail_no_readings))
            }
            tank.waterAlerts.forEach { alert ->
                StatusCard(
                    title = stringResource(alert.kind.labelRes()),
                    valueText = alertValueText(alert.kind, alert.value),
                    meaning = stringResource(alert.status.messageRes()),
                    action = stringResource(alert.status.actionRes()),
                    level = alert.status.level,
                    footnote = stringResource(R.string.measured_at, formatDateTime(alert.measuredAt, zone)),
                )
            }
            if (tank.ammoniaNeedsPhAndTemperature) {
                Text(
                    text = stringResource(R.string.ammonia_needs_ph_temperature),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            SectionTitle(text = stringResource(R.string.tank_recent_title))
            if (tank.recentRecords.isEmpty()) EmptyMessage(text = stringResource(R.string.lot_recent_empty))
            tank.recentRecords.forEach { record ->
                val line = waterLine(record, zone)
                RecordRow(text = line, onDelete = { pendingDelete = DeleteRequest(line, null) { onDeleteWaterRecord(record.id) } })
            }

            SectionTitle(text = stringResource(R.string.tank_lots_title))
            if (tank.lots.isEmpty()) EmptyMessage(text = stringResource(R.string.tank_no_lots))
            tank.lots.forEach { lot ->
                LotCard(lot = lot, onOpenLot = { onOpenLot(lot.lote.id) }, onRegisterFeeding = { onRegisterFeeding(lot.lote.id) })
            }
            LargeButton(
                text = stringResource(R.string.tank_new_lot),
                icon = Icons.Default.Add,
                onClick = { onNewLot(tankId) },
            )
            LinkButton(text = stringResource(R.string.tank_qr_button), onClick = { onShowQr(tankId) })
        }
    }
}

@Composable
private fun LotCard(lot: LotOverview, onOpenLot: () -> Unit, onRegisterFeeding: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(
                    R.string.lot_detail_title,
                    stringResource(lot.lote.especie.labelRes()),
                    stringResource(lot.lote.fase.labelRes()),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    R.string.lot_card_summary,
                    pluralStringResource(R.plurals.fish_count, lot.currentQuantity, lot.currentQuantity),
                    formatNumber(lot.biomassKg, 1),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            LargeButton(
                text = stringResource(R.string.lot_register_feeding),
                icon = Icons.Default.Add,
                onClick = onRegisterFeeding,
            )
            LinkButton(text = stringResource(R.string.lot_open_details), onClick = onOpenLot)
        }
    }
}

private fun previewData(): TankDetailData {
    val now = Instant.parse("2026-10-16T09:10:00Z")
    val readings = LatestWaterReadings(
        oxigenioMgL = Reading(2.5, now),
        ph = Reading(7.4, now),
        temperaturaC = Reading(28.0, now),
        amoniaTotalMgL = Reading(1.5, now),
    )
    val lote = Lote(
        id = 1, tanqueId = 1, especie = Especie.TILAPIA, fase = FaseLote.ENGORDA, quantidadeInicial = 1000,
        pesoMedioInicialG = 20.0, dataPovoamento = now, pesoMedioAtualG = 250.0,
    )
    return TankDetailData(
        tanque = Tanque(id = 1, nome = "Viveiro 1", tipo = TipoTanque.ESCAVADO, areaM2 = 1000.0, usaAerador = false, renovacaoAguaPctDia = 0.0),
        waterAlerts = WaterReadingsEvaluator.evaluate(readings).alerts,
        ammoniaNeedsPhAndTemperature = false,
        lots = listOf(LotOverview(lote, emptyList(), emptyList(), emptyList())),
    )
}

@Preview(name = "Detalhe do tanque", showBackground = true, fontScale = 2f)
@Composable
private fun TankDetailPreview() {
    AquaGestorTheme {
        TankDetailContent(
            state = TankDetailUiState(isLoading = false, tank = previewData()),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onOpenLot = {}, onNewLot = {}, onRegisterWater = {}, onRegisterFeeding = {},
        )
    }
}
