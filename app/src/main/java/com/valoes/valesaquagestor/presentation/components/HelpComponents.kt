package com.valoes.valesaquagestor.presentation.components

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/**
 * Dica curta de uma tela, aberta pelo botão "?" (`specs/04-requisitos-ux.md`, seção 6). Sempre traz
 * um exemplo real.
 */
class HelpTip(@StringRes val title: Int, @StringRes val text: Int)

/** A dica de cada tela. */
object HelpTips {
    val Home = HelpTip(R.string.tip_home_title, R.string.tip_home_text)
    val Tanks = HelpTip(R.string.tip_tanks_title, R.string.tip_tanks_text)
    val TankForm = HelpTip(R.string.tip_tank_form_title, R.string.tip_tank_form_text)
    val TankDetail = HelpTip(R.string.tip_tank_detail_title, R.string.tip_tank_detail_text)
    val LotDetail = HelpTip(R.string.tip_lot_detail_title, R.string.tip_lot_detail_text)
    val LotForm = HelpTip(R.string.tip_lot_form_title, R.string.tip_lot_form_text)
    val Register = HelpTip(R.string.tip_register_title, R.string.tip_register_text)
    val Water = HelpTip(R.string.tip_water_title, R.string.tip_water_text)
    val Feeding = HelpTip(R.string.tip_feeding_title, R.string.tip_feeding_text)
    val Biometry = HelpTip(R.string.tip_biometry_title, R.string.tip_biometry_text)
    val Movement = HelpTip(R.string.tip_movement_title, R.string.tip_movement_text)
    val More = HelpTip(R.string.tip_more_title, R.string.tip_more_text)
    val Settings = HelpTip(R.string.tip_settings_title, R.string.tip_settings_text)
    val Users = HelpTip(R.string.tip_users_title, R.string.tip_users_text)
    val WaterRanges = HelpTip(R.string.tip_water_ranges_title, R.string.tip_water_ranges_text)
    val Schedule = HelpTip(R.string.tip_schedule_title, R.string.tip_schedule_text)
    val Login = HelpTip(R.string.tip_login_title, R.string.tip_login_text)
    val SignUp = HelpTip(R.string.tip_signup_title, R.string.tip_signup_text)
    val ForgotPassword = HelpTip(R.string.tip_forgot_title, R.string.tip_forgot_text)
}

/**
 * Abre a tela de Ajuda completa. A navegação fornece a ação; fora dela o botão "Mais ajuda" não faz nada
 * (previews e testes).
 */
val LocalOpenHelp = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * Botão "?" que abre a [tip] num diálogo, com o botão "Entendi" e o atalho "Mais ajuda" para a
 * tela de Ajuda. Alvo de toque de 48 dp e descrição para o leitor de tela.
 */
@Composable
fun HelpButton(tip: HelpTip, modifier: Modifier = Modifier) {
    var showTip by remember { mutableStateOf(false) }
    val description = stringResource(R.string.help_button_description)
    val openHelp = LocalOpenHelp.current
    IconButton(onClick = { showTip = true }, modifier = modifier.semantics { contentDescription = description }) {
        Text(text = stringResource(R.string.help_symbol), style = MaterialTheme.typography.titleLarge)
    }
    if (showTip) {
        AlertDialog(
            onDismissRequest = { showTip = false },
            title = { Text(stringResource(tip.title)) },
            text = { Text(stringResource(tip.text)) },
            confirmButton = { TextButton(onClick = { showTip = false }) { Text(stringResource(R.string.help_dialog_close)) } },
            dismissButton = {
                TextButton(onClick = {
                    showTip = false
                    openHelp()
                }) { Text(stringResource(R.string.help_dialog_more)) }
            },
        )
    }
}

@Preview(name = "Botão de ajuda", showBackground = true, fontScale = 2f)
@Composable
private fun HelpButtonPreview() {
    AquaGestorTheme {
        CompositionLocalProvider(LocalOpenHelp provides {}) { HelpButton(HelpTips.Water) }
    }
}
