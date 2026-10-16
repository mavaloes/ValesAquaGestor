package com.valoes.valesaquagestor.presentation.lot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.rules.AlertItem
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.CompositeAlerts
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.EmptyMessage
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.SavedMessageEffect
import com.valoes.valesaquagestor.presentation.components.SectionTitle
import com.valoes.valesaquagestor.presentation.components.StatusCard
import com.valoes.valesaquagestor.presentation.components.actionRes
import com.valoes.valesaquagestor.presentation.components.alertValueText
import com.valoes.valesaquagestor.presentation.components.labelRes
import com.valoes.valesaquagestor.presentation.components.messageRes
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.formatDateTime
import com.valoes.valesaquagestor.presentation.util.formatNumber
import java.time.Instant
import java.time.ZoneId

/** Tela do lote: peixes vivos, biomassa, peso médio, mortalidade, TCA e alertas. */
@Composable
fun LotDetailScreen(
    onBack: () -> Unit,
    showSavedMessage: Boolean = false,
    viewModel: LotDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    SavedMessageEffect(show = showSavedMessage, hostState = snackbarHostState)
    LotDetailContent(state = state, snackbarHostState = snackbarHostState, onBack = onBack)
}

@Composable
internal fun LotDetailContent(state: LotDetailUiState, snackbarHostState: SnackbarHostState, onBack: () -> Unit) {
    val lot = state.lot
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
            Text(text = stringResource(R.string.lot_in_tank, lot.tankName), style = MaterialTheme.typography.bodyLarge)

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
    AquaGestorTheme {
        LotDetailContent(
            state = LotDetailUiState(
                isLoading = false,
                lot = LotDetailData(
                    lote = lote, tankName = "Viveiro 1", currentQuantity = 880, biomassKg = 220.0, mortalityPct = 12.0,
                    fcr = 1.42, hasBiometry = true,
                    alerts = listOf(AlertItem(1, "Viveiro 1", 1, AlertKind.MORTALITY, 12.0, status, null)),
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
        )
    }
}
