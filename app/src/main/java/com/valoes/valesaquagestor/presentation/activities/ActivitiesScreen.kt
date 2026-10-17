package com.valoes.valesaquagestor.presentation.activities

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.Atividade
import com.valoes.valesaquagestor.presentation.components.AppTextField
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.ConfirmDialog
import com.valoes.valesaquagestor.presentation.components.EmptyMessage
import com.valoes.valesaquagestor.presentation.components.FeedbackEffect
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.SectionTitle
import com.valoes.valesaquagestor.presentation.components.requiredLabel
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.stringRes
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ptBr: Locale = Locale.forLanguageTag("pt-BR")
private val DayCellHeight = 48.dp
private val DayMarkSize = 6.dp
private const val DAYS_IN_WEEK = 7

/** Atividades diárias: calendário do mês, lista do dia escolhido e cadastro de novas atividades. */
@Composable
fun ActivitiesScreen(onBack: () -> Unit, viewModel: ActivitiesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saved = stringResource(R.string.activities_saved)
    val deleted = stringResource(R.string.activities_deleted)
    FeedbackEffect(
        message = when (state.message) {
            null -> null
            ActivitiesMessage.SAVED -> saved
            ActivitiesMessage.DELETED -> deleted
        },
        hostState = snackbarHostState,
        onShown = viewModel::onMessageShown,
    )
    ActivitiesContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onSelectDay = viewModel::onSelectDay,
        onGoToToday = viewModel::onGoToToday,
        onNewActivity = viewModel::onNewActivity,
        onEditActivity = viewModel::onEditActivity,
        onToggleDone = viewModel::onToggleDone,
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        onFormDismiss = viewModel::onFormDismiss,
        onTitleChange = viewModel::onTitleChange,
        onTimeChange = viewModel::onTimeChange,
        onNotesChange = viewModel::onNotesChange,
        onSave = viewModel::onSave,
    )
}

@Composable
internal fun ActivitiesContent(
    state: ActivitiesUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onGoToToday: () -> Unit,
    onNewActivity: () -> Unit,
    onEditActivity: (Atividade) -> Unit,
    onToggleDone: (Atividade) -> Unit,
    onDeleteClick: (Atividade) -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onFormDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onTimeChange: (LocalTime?) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.activities_title), onBack = onBack) },
        snackbarHost = { FeedbackSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonthCalendar(
                    state = state,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectDay = onSelectDay,
                    onGoToToday = onGoToToday,
                )
            }
            item {
                SectionTitle(text = stringResource(R.string.activities_day_title, formatLongDate(state.selected)))
            }
            item {
                LargeButton(
                    text = stringResource(R.string.activities_new),
                    icon = Icons.Default.Add,
                    onClick = onNewActivity,
                )
            }
            if (state.selectedActivities.isEmpty()) {
                item { EmptyMessage(text = stringResource(R.string.activities_day_empty)) }
            }
            items(state.selectedActivities, key = { it.id }) { activity ->
                ActivityCard(
                    activity = activity,
                    onToggleDone = { onToggleDone(activity) },
                    onEdit = { onEditActivity(activity) },
                    onDelete = { onDeleteClick(activity) },
                )
            }
        }
    }
    state.form?.let { form ->
        ActivityFormDialog(
            form = form,
            onTitleChange = onTitleChange,
            onTimeChange = onTimeChange,
            onNotesChange = onNotesChange,
            onSave = onSave,
            onDismiss = onFormDismiss,
        )
    }
    state.deleting?.let { activity ->
        ConfirmDialog(
            title = stringResource(R.string.activities_delete_title),
            text = stringResource(R.string.activities_delete_text, activity.titulo),
            confirmText = stringResource(R.string.activities_delete),
            onConfirm = onDeleteConfirm,
            onDismiss = onDeleteDismiss,
        )
    }
}

/** Calendário do mês: setas para trocar de mês, dias da semana e uma célula por dia. */
@Composable
private fun MonthCalendar(
    state: ActivitiesUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onGoToToday: () -> Unit,
) {
    val withPending = state.daysWithPending
    val withActivities = state.daysWithActivities
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.activities_previous_month),
                    )
                }
                Text(
                    text = formatMonth(state.month),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.activities_next_month),
                    )
                }
            }
            TextButton(onClick = onGoToToday, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(text = stringResource(R.string.activities_today), style = MaterialTheme.typography.titleMedium)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays().forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, ptBr),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).clearAndSetSemantics { },
                    )
                }
            }
            monthWeeks(state.month).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        if (day == null) {
                            Spacer(modifier = Modifier.weight(1f).heightIn(min = DayCellHeight))
                        } else {
                            DayCell(
                                day = day,
                                isSelected = day == state.selected,
                                isToday = day == state.today,
                                hasPending = day in withPending,
                                hasActivities = day in withActivities,
                                onClick = { onSelectDay(day) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Um dia do calendário. O dia escolhido fica preenchido, o de hoje ganha contorno e uma bolinha
 * embaixo avisa que há atividade (cheia = ainda falta fazer). A descrição para o leitor de tela
 * repete tudo isso em palavras, então nada depende só da cor.
 */
@Composable
private fun DayCell(
    day: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasPending: Boolean,
    hasActivities: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val date = formatLongDate(day)
    val description = when {
        hasPending -> stringResource(R.string.activities_day_with_pending, date)
        hasActivities -> stringResource(R.string.activities_day_all_done, date)
        else -> date
    }
    val todayDescription = if (isToday) stringResource(R.string.activities_day_is_today, description) else description
    val numberColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = DayCellHeight)
            .clip(CircleShape)
            .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier)
            .then(if (isToday && !isSelected) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = todayDescription
                selected = isSelected
                role = Role.Button
            },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = numberColor)
            Box(
                modifier = Modifier
                    .size(DayMarkSize)
                    .then(
                        when {
                            hasPending -> Modifier.background(numberColor, CircleShape)
                            hasActivities -> Modifier.border(1.dp, numberColor, CircleShape)
                            else -> Modifier
                        },
                    ),
            )
        }
    }
}

@Composable
private fun ActivityCard(
    activity: Atividade,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val toggleDescription = stringResource(
        if (activity.concluida) R.string.activities_mark_undone else R.string.activities_mark_done,
        activity.titulo,
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = activity.concluida,
                    onCheckedChange = { onToggleDone() },
                    modifier = Modifier.semantics { contentDescription = toggleDescription },
                )
                Column(modifier = Modifier.weight(1f).padding(top = 12.dp, end = 8.dp)) {
                    Text(
                        text = activity.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (activity.concluida) TextDecoration.LineThrough else null,
                    )
                    Text(
                        text = activity.hora?.let { formatTime(it) } ?: stringResource(R.string.activities_any_time),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!activity.observacao.isNullOrBlank()) {
                        Text(text = activity.observacao, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Row(modifier = Modifier.padding(start = 48.dp)) {
                TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(text = stringResource(R.string.activities_edit), style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(text = stringResource(R.string.activities_delete), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun ActivityFormDialog(
    form: ActivityForm,
    onTitleChange: (String) -> Unit,
    onTimeChange: (LocalTime?) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (form.id == 0L) R.string.activities_form_new else R.string.activities_form_edit))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppTextField(
                    value = form.titulo,
                    onValueChange = onTitleChange,
                    label = requiredLabel(stringResource(R.string.activities_field_title)),
                    placeholder = stringResource(R.string.activities_field_title_example),
                    errorText = form.tituloError?.let { stringResource(it.stringRes()) },
                )
                Text(text = stringResource(R.string.activities_field_time), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = form.hora?.let { formatTime(it) } ?: stringResource(R.string.activities_time_none),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row {
                    TextButton(onClick = { showTimePicker = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(
                            text = stringResource(if (form.hora == null) R.string.activities_time_set else R.string.activities_time_change),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    if (form.hora != null) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { onTimeChange(null) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(text = stringResource(R.string.activities_time_clear), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                AppTextField(
                    value = form.observacao,
                    onValueChange = onNotesChange,
                    label = stringResource(R.string.activities_field_notes),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = form.canSave, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.activities_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
    if (showTimePicker) {
        TimePickerDialog(
            initial = form.hora,
            onConfirm = {
                onTimeChange(it)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

/** Escolha da hora, em 24 horas. Sem hora marcada, abre em 08:00 (só o ponto de partida do seletor). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initial: LocalTime?, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberTimePickerState(
        initialHour = initial?.hour ?: DEFAULT_PICKER_HOUR,
        initialMinute = initial?.minute ?: 0,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.activities_time_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TimePicker(state = pickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.activities_time_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

private const val DEFAULT_PICKER_HOUR = 8

/** Os dias da semana na ordem do calendário: domingo primeiro. */
private fun weekDays(): List<DayOfWeek> = listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.dropLast(1)

/**
 * As semanas do [month], de domingo a sábado. Os dias que sobram antes do dia 1 e depois do último
 * dia do mês ficam `null` (célula vazia).
 */
internal fun monthWeeks(month: YearMonth): List<List<LocalDate?>> {
    val leading = month.atDay(1).dayOfWeek.value % DAYS_IN_WEEK
    val days: List<LocalDate?> = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    return days.chunked(DAYS_IN_WEEK).map { week -> week + List(DAYS_IN_WEEK - week.size) { null } }
}

private fun formatMonth(month: YearMonth): String =
    month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ptBr)).replaceFirstChar { it.uppercase() }

private fun formatLongDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", ptBr))

private fun formatTime(time: LocalTime): String = time.format(DateTimeFormatter.ofPattern("HH:mm", ptBr))

private val previewToday: LocalDate = LocalDate.of(2026, 10, 16)

private val previewActivities = listOf(
    Atividade(1, "Limpar os bebedouros", previewToday, LocalTime.of(7, 0), "Usar a escova nova"),
    Atividade(2, "Conferir o aerador", previewToday, LocalTime.of(16, 30), concluida = true),
    Atividade(3, "Anotar o estoque de ração", previewToday),
    Atividade(4, "Pedir ração", previewToday.plusDays(3), LocalTime.of(9, 0)),
)

private fun previewState(form: ActivityForm? = null, activities: List<Atividade> = previewActivities) = ActivitiesUiState(
    month = YearMonth.from(previewToday),
    today = previewToday,
    selected = previewToday,
    monthActivities = activities,
    form = form,
)

@Composable
private fun PreviewContent(state: ActivitiesUiState) {
    AquaGestorTheme {
        ActivitiesContent(
            state = state,
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onPreviousMonth = {}, onNextMonth = {}, onSelectDay = {}, onGoToToday = {},
            onNewActivity = {}, onEditActivity = {}, onToggleDone = {}, onDeleteClick = {}, onDeleteConfirm = {},
            onDeleteDismiss = {}, onFormDismiss = {}, onTitleChange = {}, onTimeChange = {}, onNotesChange = {},
            onSave = {},
        )
    }
}

@Preview(name = "Atividades", showBackground = true, fontScale = 2f)
@Composable
private fun ActivitiesPreview() = PreviewContent(previewState())

@Preview(name = "Atividades sem nenhuma", showBackground = true, fontScale = 2f)
@Composable
private fun ActivitiesEmptyPreview() = PreviewContent(previewState(activities = emptyList()))

@Preview(name = "Nova atividade", showBackground = true, fontScale = 2f)
@Composable
private fun ActivityFormPreview() = PreviewContent(
    previewState(form = ActivityForm(titulo = "", tituloError = com.valoes.valesaquagestor.presentation.util.FormError.REQUIRED)),
)
