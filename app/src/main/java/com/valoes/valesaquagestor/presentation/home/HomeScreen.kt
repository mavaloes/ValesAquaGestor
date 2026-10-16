package com.valoes.valesaquagestor.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.valoes.valesaquagestor.domain.calc.DailyRoutine
import com.valoes.valesaquagestor.domain.calc.MeasuredParameter
import com.valoes.valesaquagestor.domain.calc.PendingTask
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.rules.AlertItem
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.WaterEvaluator
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.EmptyMessage
import com.valoes.valesaquagestor.presentation.components.SectionTitle
import com.valoes.valesaquagestor.presentation.components.StatusCard
import com.valoes.valesaquagestor.presentation.components.actionRes
import com.valoes.valesaquagestor.presentation.components.alertValueText
import com.valoes.valesaquagestor.presentation.components.labelRes
import com.valoes.valesaquagestor.presentation.components.messageRes
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.formatDateTime
import java.time.Instant
import java.time.ZoneId

/**
 * Home = rotina diária: os alertas (EMERGENCIA primeiro) e o que falta fazer hoje.
 * Tocar num item abre o tanque ou o lote correspondente.
 */
@Composable
fun HomeScreen(
    onOpenTank: (Long) -> Unit,
    onOpenLot: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(state = state, onOpenTank = onOpenTank, onOpenLot = onOpenLot)
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    onOpenTank: (Long) -> Unit,
    onOpenLot: (Long) -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.home_title)) },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(innerPadding),
        ) {
            if (!state.isLoading && !state.hasTanks) {
                item { EmptyMessage(text = stringResource(R.string.home_no_tanks)) }
                return@LazyColumn
            }

            item { SectionTitle(text = stringResource(R.string.home_alerts_title)) }
            if (state.routine.alerts.isEmpty()) {
                item { EmptyMessage(text = stringResource(R.string.home_no_alerts)) }
            }
            items(state.routine.alerts.size) { index ->
                val alert = state.routine.alerts[index]
                StatusCard(
                    title = stringResource(R.string.home_alert_title, alert.tankName, stringResource(alert.kind.labelRes())),
                    valueText = alert.value?.let { alertValueText(alert.kind, it) }.orEmpty(),
                    meaning = stringResource(alert.status.messageRes()),
                    action = stringResource(alert.status.actionRes()),
                    level = alert.status.level,
                    footnote = alert.measuredAt?.let { stringResource(R.string.measured_at, formatDateTime(it, zone)) },
                    onClick = { alert.lotId?.let(onOpenLot) ?: onOpenTank(alert.tankId) },
                )
            }

            item { SectionTitle(text = stringResource(R.string.home_tasks_title)) }
            if (state.routine.tasks.isEmpty()) {
                item { EmptyMessage(text = stringResource(R.string.home_all_done)) }
            }
            val byTank = state.routine.tasks.groupBy { it.tankId }
            byTank.forEach { (_, tasks) ->
                item {
                    Text(text = tasks.first().tankName, style = MaterialTheme.typography.titleMedium)
                }
                items(tasks.size) { index ->
                    val task = tasks[index]
                    TaskRow(
                        text = taskText(task),
                        onClick = {
                            when (task) {
                                is PendingTask.Measurement -> onOpenTank(task.tankId)
                                is PendingTask.Feeding -> onOpenLot(task.lotId)
                                is PendingTask.Biometry -> onOpenLot(task.lotId)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(text: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun taskText(task: PendingTask): String = when (task) {
    is PendingTask.Measurement -> {
        val name = stringResource(task.parameter.labelRes())
        val moment = task.moment
        if (moment != null) {
            stringResource(R.string.task_measure_daily, name, stringResource(moment.labelRes()))
        } else {
            stringResource(R.string.task_measure_weekly, name)
        }
    }

    is PendingTask.Feeding -> pluralStringResource(
        R.plurals.task_feeding, task.missingMeals, task.missingMeals, task.expectedMeals, stringResource(task.especie.labelRes()),
    )

    is PendingTask.Biometry -> if (task.overdueDays == 0) {
        stringResource(R.string.task_biometry_today, stringResource(task.especie.labelRes()))
    } else {
        pluralStringResource(R.plurals.task_biometry_late, task.overdueDays, task.overdueDays, stringResource(task.especie.labelRes()))
    }
}

@Preview(name = "Home com alertas e tarefas", showBackground = true, fontScale = 2f)
@Composable
private fun HomePreview() {
    val now = Instant.parse("2026-10-16T09:10:00Z")
    AquaGestorTheme {
        HomeContent(
            state = HomeUiState(
                isLoading = false,
                hasTanks = true,
                routine = DailyRoutine(
                    alerts = listOf(
                        AlertItem(1, "Viveiro 1", null, AlertKind.OXYGEN, 1.5, WaterEvaluator.evaluateOxygen(1.5), now),
                        AlertItem(2, "Viveiro 2", null, AlertKind.PH, 8.7, WaterEvaluator.evaluatePh(8.7), now),
                    ),
                    tasks = listOf(
                        PendingTask.Measurement(1, "Viveiro 1", MeasuredParameter.OXYGEN, MomentoMedicao.TARDE),
                        PendingTask.Feeding(1, "Viveiro 1", 10, Especie.TILAPIA, 2, 3),
                        PendingTask.Biometry(2, "Viveiro 2", 11, Especie.TILAPIA, 0),
                    ),
                ),
            ),
            onOpenTank = {},
            onOpenLot = {},
        )
    }
}

@Preview(name = "Home sem tanques", showBackground = true, fontScale = 2f)
@Composable
private fun HomeEmptyPreview() {
    AquaGestorTheme { HomeContent(HomeUiState(isLoading = false), onOpenTank = {}, onOpenLot = {}) }
}
