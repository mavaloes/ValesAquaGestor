package com.valoes.valesaquagestor.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.EmptyMessage
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/**
 * Tela provisória das telas de registro (Fase 06): aba "Registrar" e os destinos de registrar
 * água e ração. Só avisa que a tela chega na próxima fase.
 *
 * @param title título da barra de cima.
 * @param onBack botão Voltar; `null` nas abas do menu.
 */
@Composable
fun ComingSoonScreen(title: String, onBack: (() -> Unit)? = null) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = title, onBack = onBack) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EmptyMessage(text = stringResource(R.string.coming_soon_text))
        }
    }
}

/**
 * Aba "Mais". Na Fase 04 o "Sair" ficava na Home; agora fica aqui até as Configurações (Fase 08),
 * que trazem a confirmação e o aviso de registros não enviados.
 */
@Composable
fun MoreScreen(viewModel: MoreViewModel = hiltViewModel()) {
    MoreContent(onSignOut = viewModel::onSignOut)
}

@Composable
internal fun MoreContent(onSignOut: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.more_title)) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LargeButton(
                text = stringResource(R.string.more_sign_out),
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = onSignOut,
            )
        }
    }
}

@Preview(name = "Em breve", showBackground = true, fontScale = 2f)
@Composable
private fun ComingSoonPreview() {
    AquaGestorTheme { ComingSoonScreen(title = stringResource(R.string.register_title)) }
}

@Preview(name = "Mais", showBackground = true, fontScale = 2f)
@Composable
private fun MorePreview() {
    AquaGestorTheme { MoreContent(onSignOut = {}) }
}
