package com.valoes.valesaquagestor.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AquaPrimaryDark,
    onPrimary = AquaOnPrimaryDark,
    primaryContainer = AquaPrimaryContainerDark,
    onPrimaryContainer = AquaOnPrimaryContainerDark,
    secondary = AquaSecondaryDark,
    onSecondary = AquaOnSecondaryDark,
    background = AquaBackgroundDark,
    onBackground = AquaOnBackgroundDark,
    surface = AquaBackgroundDark,
    onSurface = AquaOnBackgroundDark,
    error = AquaErrorDark,
    onError = AquaOnErrorDark,
)

private val LightColorScheme = lightColorScheme(
    primary = AquaPrimaryLight,
    onPrimary = AquaOnPrimaryLight,
    primaryContainer = AquaPrimaryContainerLight,
    onPrimaryContainer = AquaOnPrimaryContainerLight,
    secondary = AquaSecondaryLight,
    onSecondary = AquaOnSecondaryLight,
    background = AquaBackgroundLight,
    onBackground = AquaOnBackgroundLight,
    surface = AquaBackgroundLight,
    onSurface = AquaOnBackgroundLight,
    error = AquaErrorLight,
    onError = AquaOnErrorLight,
)

/**
 * Tema do app (Material 3). Não usa cores dinâmicas do sistema: a paleta é fixa para garantir o
 * contraste mínimo de 4,5:1 e para as cores de alerta (Fase 05) terem sentido em qualquer aparelho.
 */
@Composable
fun AquaGestorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
