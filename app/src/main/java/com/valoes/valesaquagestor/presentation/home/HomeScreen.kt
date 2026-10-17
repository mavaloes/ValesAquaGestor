package com.valoes.valesaquagestor.presentation.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.calc.DailyRoutine
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.rules.AlertItem
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.domain.rules.WaterEvaluator
import com.valoes.valesaquagestor.domain.usecase.SetupSuggestion
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.EmptyMessage
import com.valoes.valesaquagestor.presentation.components.SectionTitle
import com.valoes.valesaquagestor.presentation.components.StatusCard
import com.valoes.valesaquagestor.presentation.components.actionRes
import com.valoes.valesaquagestor.presentation.components.alertValueText
import com.valoes.valesaquagestor.presentation.components.labelRes
import com.valoes.valesaquagestor.presentation.components.messageRes
import com.valoes.valesaquagestor.domain.model.Atividade
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.presentation.main.RegisterLot
import com.valoes.valesaquagestor.presentation.main.RegisterTank
import com.valoes.valesaquagestor.presentation.sync.SyncStatusIndicator
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.formatDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    onRegisterMovement: (lotId: Long) -> Unit,
    onNewLot: (tankId: Long) -> Unit,
    onOpenActivities: () -> Unit,
    onScanQr: () -> Unit,
    onOpenHelp: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    HomeContent(
        state = state,
        onExit = { context.finishActivity() },
        onOpenTank = onOpenTank,
        onOpenLot = onOpenLot,
        onRegisterWater = onRegisterWater,
        onRegisterFeeding = onRegisterFeeding,
        onRegisterBiometry = onRegisterBiometry,
        onRegisterMovement = onRegisterMovement,
        onNewLot = onNewLot,
        onOpenActivities = onOpenActivities,
        onScanQr = onScanQr,
        onOpenHelp = onOpenHelp,
        onSetupFarm = onSetupFarm,
        syncStatus = { SyncStatusIndicator() },
    )
}

/**
 * Três divisões na vertical: o quadro de avisos no alto (alertas e tarefas do dia, com rolagem), os
 * botões de atalho em grade no meio (cada botão com a mesma área) e, embaixo, o menu inferior, que
 * pertence à navegação do app (`AppBottomBar`).
 */
@Composable
internal fun HomeContent(
    state: HomeUiState,
    onOpenTank: (Long) -> Unit,
    onOpenLot: (Long) -> Unit,
    onRegisterWater: (Long) -> Unit = {},
    onRegisterFeeding: (Long) -> Unit = {},
    onRegisterBiometry: (Long) -> Unit = {},
    onRegisterMovement: (Long) -> Unit = {},
    onNewLot: (Long) -> Unit = {},
    onOpenActivities: () -> Unit = {},
    onScanQr: () -> Unit = {},
    onOpenHelp: () -> Unit = {},
    onSetupFarm: (Long?) -> Unit = {},
    onExit: () -> Unit = {},
    syncStatus: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                HomeTopBar(onExit = onExit)
                syncStatus()
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            NoticeBoard(
                state = state,
                onOpenTank = onOpenTank,
                onOpenLot = onOpenLot,
                onSetupFarm = onSetupFarm,
                onOpenActivities = onOpenActivities,
                modifier = Modifier.weight(NOTICE_BOARD_WEIGHT),
            )
            HorizontalDivider()
            ShortcutGrid(
                tanks = state.tanks,
                onRegisterWater = onRegisterWater,
                onRegisterFeeding = onRegisterFeeding,
                onRegisterBiometry = onRegisterBiometry,
                onRegisterMovement = onRegisterMovement,
                onNewLot = onNewLot,
                onOpenActivities = onOpenActivities,
                onScanQr = onScanQr,
                onOpenHelp = onOpenHelp,
                modifier = Modifier.weight(SHORTCUT_GRID_WEIGHT),
            )
        }
    }
}

/**
 * Barra do topo da Home: o nome do app em negrito, no início, e o botão de sair do aplicativo, no
 * fim, sobre a cor primária do tema.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onExit: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                modifier = Modifier.semantics { heading() },
            )
        },
        actions = {
            IconButton(onClick = onExit) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = stringResource(R.string.home_exit),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

/** Fecha o app: encerra a tela em que a Home está. Fora de uma tela (previews e testes), não faz nada. */
private fun Context.finishActivity() {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            current.finish()
            return
        }
        current = current.baseContext
    }
}

/** Quadro de avisos: convite de configuração e alertas (EMERGENCIA primeiro). */
@Composable
private fun NoticeBoard(
    state: HomeUiState,
    onOpenTank: (Long) -> Unit,
    onOpenLot: (Long) -> Unit,
    onSetupFarm: (Long?) -> Unit,
    onOpenActivities: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = remember { ZoneId.systemDefault() }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
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

        // Atividades do dia que ainda não foram feitas; tocar abre o calendário.
        if (state.todayActivities.isNotEmpty()) {
            item { SectionTitle(text = stringResource(R.string.home_activities_title)) }
            items(state.todayActivities, key = { it.id }) { activity ->
                ActivityNotice(activity = activity, onClick = onOpenActivities)
            }
        }
    }
}

/** Uma atividade de hoje no quadro de avisos, com a hora quando tem. */
@Composable
private fun ActivityNotice(activity: Atividade, onClick: () -> Unit) {
    val text = activity.hora?.let {
        stringResource(R.string.home_activity_with_time, it.format(DateTimeFormatter.ofPattern("HH:mm")), activity.titulo)
    } ?: activity.titulo
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
    }
}

/** Atalhos da Home. Os que dependem de tanque ou lote abrem a escolha antes (ver [ShortcutGrid]). */
private enum class Shortcut(
    @StringRes val label: Int,
    val icon: ImageVector,
    /** `true` se o destino é um lote; `false` se é um tanque. */
    val needsLot: Boolean,
) {
    WATER(R.string.home_shortcut_water, Icons.Default.WaterDrop, needsLot = false),
    FEEDING(R.string.home_shortcut_feeding, Icons.Default.Restaurant, needsLot = true),
    BIOMETRY(R.string.home_shortcut_biometry, Icons.Default.MonitorWeight, needsLot = true),
    MOVEMENT(R.string.home_shortcut_movement, Icons.Default.SetMeal, needsLot = true),
    NEW_LOT(R.string.home_shortcut_new_lot, Icons.Default.Add, needsLot = false),
    ACTIVITIES(R.string.home_shortcut_activities, Icons.Default.CalendarMonth, needsLot = false),
    SCAN_QR(R.string.home_shortcut_scan_qr, Icons.Default.QrCodeScanner, needsLot = false),
    HELP(R.string.home_shortcut_help, Icons.AutoMirrored.Filled.Help, needsLot = false),
    ;

    /** `true` se o botão abre a tela direto, sem perguntar tanque nem lote. */
    val isDirect: Boolean get() = this == ACTIVITIES || this == SCAN_QR || this == HELP
}

/** Onde o atalho vai registrar: o tanque ou o lote com o texto que o identifica. */
private data class ShortcutTarget(val id: Long, val label: String)

/**
 * Grade 2 × 4 de atalhos, com todos os botões do mesmo tamanho. Se há um só destino possível, abre
 * direto; se há vários, pergunta o tanque ou o lote (Home → botão → escolha → Salvar = 3 toques).
 */
@Composable
private fun ShortcutGrid(
    tanks: List<RegisterTank>,
    onRegisterWater: (Long) -> Unit,
    onRegisterFeeding: (Long) -> Unit,
    onRegisterBiometry: (Long) -> Unit,
    onRegisterMovement: (Long) -> Unit,
    onNewLot: (Long) -> Unit,
    onOpenActivities: () -> Unit,
    onScanQr: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf<Shortcut?>(null) }

    val tankTargets = tanks.map { ShortcutTarget(it.tankId, it.nome) }
    val lotTargets = tanks.flatMap { tank ->
        tank.lots.map { lot ->
            ShortcutTarget(
                id = lot.lotId,
                label = stringResource(
                    R.string.home_pick_lot_item,
                    tank.nome,
                    stringResource(lot.especie.labelRes()),
                    stringResource(lot.fase.labelRes()),
                ),
            )
        }
    }

    val open: (Shortcut, Long) -> Unit = { shortcut, id ->
        when (shortcut) {
            Shortcut.WATER -> onRegisterWater(id)
            Shortcut.FEEDING -> onRegisterFeeding(id)
            Shortcut.BIOMETRY -> onRegisterBiometry(id)
            Shortcut.MOVEMENT -> onRegisterMovement(id)
            Shortcut.NEW_LOT -> onNewLot(id)
            Shortcut.ACTIVITIES, Shortcut.SCAN_QR, Shortcut.HELP -> Unit
        }
    }
    val onShortcut: (Shortcut) -> Unit = { shortcut ->
        if (shortcut.isDirect) {
            when (shortcut) {
                Shortcut.ACTIVITIES -> onOpenActivities()
                Shortcut.SCAN_QR -> onScanQr()
                else -> onOpenHelp()
            }
        } else {
            val targets = if (shortcut.needsLot) lotTargets else tankTargets
            if (targets.size == 1) open(shortcut, targets.single().id) else picking = shortcut
        }
    }

    // A grade fica no meio do espaço entre o quadro de avisos e o menu inferior. Se não couber
    // (fonte ampliada), rola em vez de cortar os botões.
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Shortcut.entries.chunked(2).forEach { rowItems ->
                // Os dois botões da linha têm sempre a mesma altura, mesmo se um título quebrar em duas linhas.
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEach { shortcut ->
                        ShortcutTile(
                            text = stringResource(shortcut.label),
                            icon = shortcut.icon,
                            onClick = { onShortcut(shortcut) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }

    picking?.let { shortcut ->
        TargetPickerDialog(
            title = stringResource(if (shortcut.needsLot) R.string.home_pick_lot else R.string.home_pick_tank),
            targets = if (shortcut.needsLot) lotTargets else tankTargets,
            onPick = { id ->
                picking = null
                open(shortcut, id)
            },
            onDismiss = { picking = null },
        )
    }
}

/** Altura mínima de cada botão da grade: grande o bastante para ícone e título de duas linhas. */
private val ShortcutTileMinHeight = 88.dp

/** Divisão da altura: o quadro de avisos fica com pouco espaço e a grade de botões com o resto. */
private const val NOTICE_BOARD_WEIGHT = 0.6f
private const val SHORTCUT_GRID_WEIGHT = 1.4f

@Composable
private fun ShortcutTile(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ShortcutTileMinHeight),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        contentPadding = PaddingValues(8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            // O texto abaixo já diz o destino; o leitor de tela não repete a descrição do ícone.
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(32.dp).clearAndSetSemantics { },
            )
            Text(text = text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

/** Escolha do tanque ou do lote. Sem nenhum destino, explica o que cadastrar primeiro. */
@Composable
private fun TargetPickerDialog(
    title: String,
    targets: List<ShortcutTarget>,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            if (targets.isEmpty()) {
                Text(text = stringResource(R.string.register_empty), style = MaterialTheme.typography.bodyLarge)
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    targets.forEach { target ->
                        FilledTonalButton(
                            onClick = { onPick(target.id) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text(text = target.label, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
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

@Preview(name = "Home com alertas", showBackground = true, fontScale = 2f)
@Composable
private fun HomePreview() {
    val now = Instant.parse("2026-10-16T09:10:00Z")
    AquaGestorTheme {
        HomeContent(
            state = HomeUiState(
                isLoading = false,
                hasTanks = true,
                tanks = listOf(
                    RegisterTank(1, "Viveiro 1", listOf(RegisterLot(10, Especie.TILAPIA, FaseLote.ENGORDA))),
                    RegisterTank(2, "Viveiro 2", listOf(RegisterLot(11, Especie.TILAPIA, FaseLote.ENGORDA))),
                ),
                routine = DailyRoutine(
                    alerts = listOf(
                        AlertItem(1, "Viveiro 1", null, AlertKind.OXYGEN, 1.5, WaterEvaluator.evaluateOxygen(1.5), now),
                        AlertItem(2, "Viveiro 2", null, AlertKind.PH, 8.7, WaterEvaluator.evaluatePh(8.7), now),
                    ),
                    tasks = emptyList(),
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
