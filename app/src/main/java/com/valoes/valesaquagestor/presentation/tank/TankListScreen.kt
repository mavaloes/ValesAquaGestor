package com.valoes.valesaquagestor.presentation.tank

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.TipoTanque
import com.valoes.valesaquagestor.domain.rules.AlertLevel
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.EmptyMessage
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.LevelBadge
import com.valoes.valesaquagestor.presentation.components.labelRes
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/** Aba "Tanques": cada tanque com tipo, lote ativo e o pior nível de alerta. */
@Composable
fun TankListScreen(
    onOpenTank: (Long) -> Unit,
    onNewTank: () -> Unit,
    viewModel: TankListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TankListContent(state = state, onOpenTank = onOpenTank, onNewTank = onNewTank)
}

@Composable
internal fun TankListContent(
    state: TankListUiState,
    onOpenTank: (Long) -> Unit,
    onNewTank: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.tanks_title)) },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(innerPadding),
        ) {
            item {
                LargeButton(
                    text = stringResource(R.string.tanks_new),
                    icon = Icons.Default.Add,
                    onClick = onNewTank,
                )
            }
            if (!state.isLoading && state.tanks.isEmpty()) {
                item { EmptyMessage(text = stringResource(R.string.tanks_empty)) }
            }
            items(state.tanks, key = { it.tankId }) { tank ->
                TankCard(tank = tank, onClick = { onOpenTank(tank.tankId) })
            }
        }
    }
}

@Composable
private fun TankCard(tank: TankListItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = tank.nome, style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(tank.tipo.labelRes()), style = MaterialTheme.typography.bodyMedium)
            Text(text = lotSummaryText(tank.lots), style = MaterialTheme.typography.bodyLarge)
            if (tank.worstLevel != null) {
                LevelBadge(level = tank.worstLevel)
            } else {
                Text(
                    text = stringResource(R.string.level_no_readings),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun lotSummaryText(lots: List<LotSummary>): String = when (lots.size) {
    0 -> stringResource(R.string.tank_no_lot)
    1 -> stringResource(
        R.string.tank_one_lot,
        stringResource(lots.first().especie.labelRes()),
        pluralStringResource(R.plurals.fish_count, lots.first().quantity, lots.first().quantity),
    )
    else -> stringResource(R.string.tank_many_lots, lots.size)
}

@Preview(name = "Tanques", showBackground = true, fontScale = 2f)
@Composable
private fun TankListPreview() {
    AquaGestorTheme {
        TankListContent(
            state = TankListUiState(
                isLoading = false,
                tanks = listOf(
                    TankListItem(1, "Viveiro 1", TipoTanque.ESCAVADO, listOf(LotSummary(Especie.TILAPIA, 950)), AlertLevel.CRITICO),
                    TankListItem(2, "Viveiro 2", TipoTanque.ACUDE, emptyList(), null),
                ),
            ),
            onOpenTank = {},
            onNewTank = {},
        )
    }
}

@Preview(name = "Tanques vazio", showBackground = true, fontScale = 2f)
@Composable
private fun TankListEmptyPreview() {
    AquaGestorTheme { TankListContent(TankListUiState(isLoading = false), onOpenTank = {}, onNewTank = {}) }
}
