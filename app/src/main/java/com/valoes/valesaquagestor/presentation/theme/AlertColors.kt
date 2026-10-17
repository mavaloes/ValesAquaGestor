package com.valoes.valesaquagestor.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.valoes.valesaquagestor.domain.rules.AlertLevel

/**
 * Cores de um nível de alerta: fundo e texto. Todos os pares têm contraste de pelo menos 4,5:1
 * (o menor é 7,1:1). A cor nunca é a única informação: o nível também aparece em texto e ícone.
 */
data class AlertColors(val container: Color, val content: Color)

private val LightAlertColors = mapOf(
    AlertLevel.ADEQUADO to AlertColors(Color(0xFFC8E6C9), Color(0xFF0B3D14)),
    AlertLevel.ATENCAO to AlertColors(Color(0xFFFFF0B3), Color(0xFF4A3B00)),
    AlertLevel.CRITICO to AlertColors(Color(0xFFFFD9B3), Color(0xFF5A2A00)),
    AlertLevel.EMERGENCIA to AlertColors(Color(0xFFFFB4AB), Color(0xFF410002)),
)

private val DarkAlertColors = mapOf(
    AlertLevel.ADEQUADO to AlertColors(Color(0xFF1F4D26), Color(0xFFD7F0D9)),
    AlertLevel.ATENCAO to AlertColors(Color(0xFF5C4B00), Color(0xFFFFE9A6)),
    AlertLevel.CRITICO to AlertColors(Color(0xFF6B3200), Color(0xFFFFDCC1)),
    AlertLevel.EMERGENCIA to AlertColors(Color(0xFF93000A), Color(0xFFFFDAD6)),
)

private val LightConfirmedColor = Color(0xFF1B7A2E)
private val DarkConfirmedColor = Color(0xFF81C784)

/**
 * Verde do "V" de item confirmado (ex.: regra da senha atendida). Contraste de pelo menos 3:1 com
 * o fundo do tema, e o ícone sempre acompanha texto, então a cor não é a única informação.
 */
@Composable
fun confirmedColor(): Color = if (isSystemInDarkTheme()) DarkConfirmedColor else LightConfirmedColor

/** Cores do nível para o tema atual (claro ou escuro). */
@Composable
fun AlertLevel.colors(): AlertColors {
    val palette = if (isSystemInDarkTheme()) DarkAlertColors else LightAlertColors
    return palette.getValue(this)
}
