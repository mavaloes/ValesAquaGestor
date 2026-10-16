package com.valoes.valesaquagestor.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.valoes.valesaquagestor.domain.usecase.SetupSuggestion
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.LargeButton
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
    onRegisterWater: (tankId: Long) -> Unit,
    onRegisterFeeding: (lotId: Long) -> Unit,
    onRegisterBiometry: (lotId: Long) -> Unit,
    onSetupFarm: (tankId: Long?) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onOpenTank = onOpenTank,
        onOpenLot = onOpenLot,
        onRegisterWater = onRegisterWater,
        onRegisterFeeding = onRegisterFeeding,
        onRegisterBiometry = onRegisterBiometry,
        onSetupFarm = onSetupFarm,
    )
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    onOpenTank: (Long) -> Unit,
    onOpenLot: (Long) -> Unit,
    onRegisterWater: (Long) -> Unit = {},
    onRegisterFeeding: (Long) -> Unit = {},
    onRegisterBiometry: (Long) -> Unit = {},
    onSetupFarm: (Long?) -> Unit = {},
) {
    val zone = remember { ZoneId.systemDefault() }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.home_title), help = HelpTips.Home) },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(innerPadding),
        ) {
            // Fazenda ainda sem tanque ou sem lote: atalho para a configuração guiada (pode ter sido pulada).
            if (!state.isLoading) {
                when (val setup = state.setup) {
                    SetupSuggestion.CreateTank -> {
                        item { SetupCard(stringResource(R.string.home_setup_tank), stringResource(R.string.home_setup_tank_button)) { onSetupFarm(null) } }
                        return@LazyColumn
                    }

                    is SetupSuggestion.CreateLot -> item {
                        SetupCard(
                            stringResource(R.string.home_setup_lot, setup.tankName),
                            stringResource(R.string.home_setup_lot_button),
                        ) { onSetupFarm(setup.tankId) }
                    }

                    SetupSuggestion.None -> Unit
                }
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
                                // Cada tarefa abre direto a tela de registro: menos toques até salvar.
                                is PendingTask.Measurement -> onRegisterWater(task.tankId)
                                is PendingTask.Feeding -> onRegisterFeeding(task.lotId)
                                is PendingTask.Biometry -> onRegisterBiometry(task.lotId)
                            }
                        },
                    )
                }
            }
        }
    }
}

/** Convite para continuar a configuração da fazenda. */
@Composable
private fun SetupCard(text: String, buttonText: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
            LargeButton(text = buttonText, icon = Icons.Default.Add, onClick = onClick)
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
    AquaGestorTheme {
        HomeContent(HomeUiState(isLoading = false, setup = SetupSuggestion.CreateTank), onOpenTank = {}, onOpenLot = {})
    }
}

@Preview(name = "Home com tanque sem lote", showBackground = true, fontScale = 2f)
@Composable
private fun HomeNoLotPreview() {
    AquaGestorTheme {
        HomeContent(
            HomeUiState(isLoading = false, hasTanks = true, setup = SetupSuggestion.CreateLot(1, "Viveiro 1")),
            onOpenTank = {},
            onOpenLot = {},
        )
    }
}
