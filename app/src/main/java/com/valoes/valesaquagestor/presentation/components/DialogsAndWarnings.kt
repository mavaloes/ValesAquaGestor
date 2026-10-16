package com.valoes.valesaquagestor.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.rules.PlausibilityWarning
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Texto do aviso de valor improvável. */
@StringRes
fun PlausibilityWarning.messageRes(): Int = when (this) {
    PlausibilityWarning.PH_VERY_HIGH -> R.string.warning_ph_very_high
    PlausibilityWarning.OXYGEN_SUPERSATURATED -> R.string.warning_oxygen_supersaturated
    PlausibilityWarning.AVERAGE_WEIGHT_JUMP -> R.string.warning_weight_jump
}

/**
 * Aviso (não bloqueio) numa caixa de destaque, com ícone e texto. Nada é mostrado se a lista está
 * vazia. O botão de salvar do formulário vira "Salvar mesmo assim" enquanto houver aviso.
 */
@Composable
fun WarningCard(messages: List<String>, modifier: Modifier = Modifier) {
    if (messages.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            messages.forEach { message ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // O texto ao lado já diz o aviso; o leitor de tela não repete a descrição do ícone.
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = stringResource(R.string.level_atencao),
                        modifier = Modifier.size(24.dp).clearAndSetSemantics { },
                    )
                    Text(text = message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}

/**
 * Diálogo de confirmação (ex.: antes de excluir). O texto deve dizer **o que** será apagado.
 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/**
 * Data e hora do registro: já vêm preenchidas com agora (automáticas) e podem ser alteradas
 * (`specs/07-automacoes.md`). Data e hora ficam em campos separados, um embaixo do outro, para
 * caberem com a fonte ampliada.
 */
@Composable
fun DateTimeField(
    dateTime: LocalDateTime,
    onDateTimeChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DateField(
            label = stringResource(R.string.field_date),
            date = dateTime.toLocalDate(),
            onDateChange = { onDateTimeChange(it.atTime(dateTime.toLocalTime())) },
        )
        TimeField(
            label = stringResource(R.string.field_time),
            time = dateTime.toLocalTime(),
            onTimeChange = { onDateTimeChange(dateTime.toLocalDate().atTime(it)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(label: String, time: LocalTime, onTimeChange: (LocalTime) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = time.format(DateTimeFormatter.ofPattern("HH:mm")),
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        label = { Text(label) },
        trailingIcon = { TextButton(onClick = { showPicker = true }) { Text(stringResource(R.string.action_change_time)) } },
        modifier = Modifier.fillMaxWidth(),
    )
    if (showPicker) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(LocalTime.of(state.hour, state.minute))
                    showPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Preview(name = "Data, hora e aviso", showBackground = true, fontScale = 2f)
@Composable
private fun DialogsAndWarningsPreview() {
    AquaGestorTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DateTimeField(dateTime = LocalDateTime.of(LocalDate.of(2026, 10, 16), LocalTime.of(6, 10)), onDateTimeChange = {})
            WarningCard(messages = listOf(stringResource(R.string.warning_ph_very_high)))
        }
    }
}
