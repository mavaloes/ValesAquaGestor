package com.valoes.valesaquagestor.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Barra de cima das telas internas, com o botão Voltar quando [onBack] é informado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onBack: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(text = title, maxLines = 2, modifier = Modifier.semantics { heading() }) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            }
        },
    )
}

/** Título de uma seção da tela. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.semantics { heading() },
    )
}

/** Mensagem simples para listas vazias e estados sem dados. */
@Composable
fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Marca o rótulo de um campo obrigatório com "*" (a legenda está no topo do formulário). */
@Composable
fun requiredLabel(label: String): String = stringResource(R.string.label_required, label)

/** Escolha de uma opção entre poucas (botões de rádio, alvo de toque de 48 dp). */
@Composable
fun <T> ChoiceGroup(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup()) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(selected = option == selected, onClick = { onSelect(option) }, role = Role.RadioButton),
            ) {
                RadioButton(selected = option == selected, onClick = null)
                Text(text = optionLabel(option), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

/** Linha com texto e chave liga/desliga, toda clicável (alvo de toque de 48 dp). */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * Campo de data: mostra a data e o botão "Alterar", que abre o calendário. A data já vem
 * preenchida pelo chamador (automática e editável, `specs/07-automacoes.md`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = formatDate(date),
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        label = { Text(label) },
        trailingIcon = { TextButton(onClick = { showPicker = true }) { Text(stringResource(R.string.action_change)) } },
        modifier = modifier.fillMaxWidth(),
    )
    if (showPicker) {
        // O calendário trabalha com a meia-noite UTC do dia escolhido.
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    showPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

@Preview(name = "Formulário", showBackground = true, fontScale = 2f)
@Composable
private fun FormComponentsPreview() {
    AquaGestorTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceGroup(
                label = stringResource(R.string.field_tank_type),
                options = listOf("A", "B"),
                selected = "A",
                optionLabel = { it },
                onSelect = {},
            )
            SwitchRow(label = stringResource(R.string.field_has_aerator), checked = true, onCheckedChange = {})
            DateField(label = stringResource(R.string.field_stocking_date), date = LocalDate.of(2026, 10, 16), onDateChange = {})
        }
    }
}
