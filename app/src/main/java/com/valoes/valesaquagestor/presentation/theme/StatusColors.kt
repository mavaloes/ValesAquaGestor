package com.valoes.valesaquagestor.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Cores de status da qualidade da água (ok, atenção, crítico).
 * O Material 3 não traz essas cores prontas, por isso ficam aqui.
 * Sempre use junto com ícone ou texto (acessibilidade para daltônicos).
 */
@Immutable
data class StatusColors(
    val ok: Color,
    val onOk: Color,
    val okContainer: Color,
    val onOkContainer: Color,
    val atencao: Color,
    val onAtencao: Color,
    val atencaoContainer: Color,
    val onAtencaoContainer: Color,
    val critico: Color,
    val onCritico: Color,
    val criticoContainer: Color,
    val onCriticoContainer: Color,
)

val LightStatusColors = StatusColors(
    ok = Color(0xFF2E7D32),
    onOk = Color(0xFFFFFFFF),
    okContainer = Color(0xFFC8E6C9),
    onOkContainer = Color(0xFF0B3D0E),
    // Âmbar escuro: o amarelo #F9A825 não tem contraste suficiente com texto branco
    atencao = Color(0xFF7A5900),
    onAtencao = Color(0xFFFFFFFF),
    atencaoContainer = Color(0xFFFFE08A),
    onAtencaoContainer = Color(0xFF261A00),
    critico = Color(0xFFC62828),
    onCritico = Color(0xFFFFFFFF),
    criticoContainer = Color(0xFFFFDAD6),
    onCriticoContainer = Color(0xFF410002),
)

val DarkStatusColors = StatusColors(
    ok = Color(0xFF81C784),
    onOk = Color(0xFF003909),
    okContainer = Color(0xFF1B5E20),
    onOkContainer = Color(0xFFC8E6C9),
    atencao = Color(0xFFFFD54F),
    onAtencao = Color(0xFF3D2E00),
    atencaoContainer = Color(0xFF5C4300),
    onAtencaoContainer = Color(0xFFFFE08A),
    critico = Color(0xFFFFB4AB),
    onCritico = Color(0xFF690005),
    criticoContainer = Color(0xFF93000A),
    onCriticoContainer = Color(0xFFFFDAD6),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
