package com.valoes.valesaquagestor.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.rules.AlertLevel
import com.valoes.valesaquagestor.domain.rules.WaterEvaluator
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.theme.colors

/** Nome do nível, em texto (a cor sozinha não basta). */
@StringRes
fun AlertLevel.labelRes(): Int = when (this) {
    AlertLevel.ADEQUADO -> R.string.level_adequado
    AlertLevel.ATENCAO -> R.string.level_atencao
    AlertLevel.CRITICO -> R.string.level_critico
    AlertLevel.EMERGENCIA -> R.string.level_emergencia
}

/** Ícone do nível: confere, informa ou avisa. */
fun AlertLevel.icon(): ImageVector = when (this) {
    AlertLevel.ADEQUADO -> Icons.Default.Check
    AlertLevel.ATENCAO -> Icons.Default.Info
    AlertLevel.CRITICO, AlertLevel.EMERGENCIA -> Icons.Default.Warning
}

/**
 * Cartão de um valor medido: **valor + o que significa + o que fazer**, com a cor do nível
 * (`specs/04-requisitos-ux.md`, seção 4). O nível aparece também em texto e ícone.
 *
 * @param title nome do parâmetro (ex.: "Oxigênio"); em alertas, pode incluir o tanque.
 * @param valueText valor com unidade (ex.: "3,5 mg/L").
 * @param meaning o que o valor significa.
 * @param action o que fazer.
 * @param footnote linha pequena opcional (ex.: quando foi medido).
 * @param onClick torna o cartão clicável (ex.: abrir o tanque).
 */
@Composable
fun StatusCard(
    title: String,
    valueText: String,
    meaning: String,
    action: String,
    level: AlertLevel,
    modifier: Modifier = Modifier,
    footnote: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = level.colors()
    val cardModifier = modifier.fillMaxWidth().let { if (onClick != null) it.heightIn(min = 48.dp) else it }
    val cardColors = CardDefaults.cardColors(containerColor = colors.container, contentColor = colors.content)
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                LevelBadge(level)
            }
            Text(text = valueText, style = MaterialTheme.typography.headlineSmall)
            Text(text = meaning, style = MaterialTheme.typography.bodyLarge)
            Text(text = action, style = MaterialTheme.typography.bodyLarge)
            if (footnote != null) Text(text = footnote, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = cardModifier, colors = cardColors) { content() }
    } else {
        Card(modifier = cardModifier, colors = cardColors) { content() }
    }
}

/** Etiqueta do nível: ícone + texto, na cor do nível. */
@Composable
fun LevelBadge(level: AlertLevel, modifier: Modifier = Modifier) {
    val colors = level.colors()
    val label = stringResource(level.labelRes())
    Surface(
        shape = RoundedCornerShape(50),
        color = colors.content,
        contentColor = colors.container,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            // O texto ao lado já diz o nível; o leitor de tela não repete a descrição do ícone.
            Icon(
                imageVector = level.icon(),
                contentDescription = stringResource(R.string.status_card_level_description, label),
                modifier = Modifier.size(18.dp).clearAndSetSemantics { },
            )
            Spacer(Modifier.width(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(name = "StatusCard crítico", showBackground = true, fontScale = 2f)
@Composable
private fun StatusCardPreview() {
    val status = WaterEvaluator.evaluateOxygen(2.5)
    AquaGestorTheme {
        StatusCard(
            title = stringResource(R.string.kind_oxygen),
            valueText = stringResource(R.string.value_with_unit, "2,5", stringResource(R.string.unit_mg_l)),
            meaning = stringResource(status.messageRes()),
            action = stringResource(status.actionRes()),
            level = status.level,
            footnote = stringResource(R.string.measured_at, "16/10 06:10"),
        )
    }
}

@Preview(name = "StatusCard adequado", showBackground = true, fontScale = 2f)
@Composable
private fun StatusCardOkPreview() {
    val status = WaterEvaluator.evaluateOxygen(5.0)
    AquaGestorTheme {
        StatusCard(
            title = stringResource(R.string.kind_oxygen),
            valueText = stringResource(R.string.value_with_unit, "5,0", stringResource(R.string.unit_mg_l)),
            meaning = stringResource(status.messageRes()),
            action = stringResource(status.actionRes()),
            level = status.level,
        )
    }
}
