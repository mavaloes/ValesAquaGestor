package com.valoes.valesaquagestor.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.Arracoamento
import com.valoes.valesaquagestor.domain.model.Biometria
import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import com.valoes.valesaquagestor.domain.model.QualidadeAgua
import com.valoes.valesaquagestor.presentation.util.formatDateTime
import com.valoes.valesaquagestor.presentation.util.formatNumber
import java.time.ZoneId

/**
 * Pedido de exclusão aguardando confirmação. [label] diz **o que** será apagado e [note] avisa o
 * que a exclusão não desfaz (ex.: o peso médio do lote).
 */
data class DeleteRequest(val label: String, val note: String?, val onConfirm: () -> Unit)

/** Linha de um registro recente, com o botão Excluir (alvo de toque de 48 dp). */
@Composable
fun RecordRow(text: String, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val deleteDescription = stringResource(R.string.dialog_delete_text, text)
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
            TextButton(
                onClick = onDelete,
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = deleteDescription },
            ) {
                Text(stringResource(R.string.action_delete))
            }
        }
    }
}

/** Diálogo de confirmação de exclusão. Não mostra nada se [request] for `null`. */
@Composable
fun DeleteRecordDialog(request: DeleteRequest?, onDismiss: () -> Unit) {
    if (request == null) return
    val text = buildString {
        append(stringResource(R.string.dialog_delete_text, request.label))
        if (request.note != null) append(" ").append(request.note)
    }
    ConfirmDialog(
        title = stringResource(R.string.dialog_delete_title),
        text = text,
        confirmText = stringResource(R.string.dialog_delete_confirm),
        onConfirm = {
            request.onConfirm()
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@Composable
fun feedingLine(feeding: Arracoamento, zone: ZoneId): String {
    val whenText = formatDateTime(feeding.dataHora, zone)
    return if (feeding.suspenso) {
        stringResource(R.string.record_feeding_suspended, whenText)
    } else {
        stringResource(R.string.record_feeding_line, whenText, formatNumber(feeding.quantidadeKg, 2))
    }
}

@Composable
fun biometryLine(biometry: Biometria, zone: ZoneId): String = stringResource(
    R.string.record_biometry_line,
    formatDateTime(biometry.dataHora, zone),
    formatNumber(biometry.pesoMedioG, 1),
    pluralStringResource(R.plurals.fish_count, biometry.numeroPeixesAmostra, biometry.numeroPeixesAmostra),
)

@Composable
fun movementLine(movement: MovimentacaoLote, zone: ZoneId): String = stringResource(
    R.string.record_movement_line,
    formatDateTime(movement.dataHora, zone),
    stringResource(movement.tipo.labelRes()),
    pluralStringResource(R.plurals.fish_count, movement.quantidade, movement.quantidade),
)

/** Resumo de uma medição de água: data, momento e os valores informados. */
@Composable
fun waterLine(record: QualidadeAgua, zone: ZoneId): String {
    fun entry(label: String, value: Double?, decimals: Int, unit: String?): String? = value?.let {
        val number = formatNumber(it, decimals)
        "$label ${if (unit == null) number else "$number $unit"}"
    }
    val mgL = stringResource(R.string.unit_mg_l)
    val values = listOfNotNull(
        entry(stringResource(R.string.kind_oxygen), record.oxigenioMgL, 1, mgL),
        entry(stringResource(R.string.kind_ph), record.ph, 1, null),
        entry(stringResource(R.string.kind_temperature), record.temperaturaC, 1, stringResource(R.string.unit_celsius)),
        entry(stringResource(R.string.kind_transparency), record.transparenciaCm, 0, stringResource(R.string.unit_cm)),
        entry(stringResource(R.string.kind_total_ammonia), record.amoniaTotalMgL, 2, mgL),
        entry(stringResource(R.string.kind_nitrite), record.nitritoMgL, 2, mgL),
        entry(stringResource(R.string.kind_alkalinity), record.alcalinidadeMgL, 0, mgL),
        entry(stringResource(R.string.kind_hardness), record.durezaMgL, 0, mgL),
    )
    return stringResource(
        R.string.record_water_line,
        formatDateTime(record.dataHora, zone),
        stringResource(record.momento.labelRes()).replaceFirstChar { it.uppercase() },
        values.joinToString(" · "),
    )
}
