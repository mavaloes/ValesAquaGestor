package com.valoes.valesaquagestor.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/**
 * Aba "Mais": Configurações (com o "Sair"), Ajuda e, só para o ADMIN, Usuários. Item que o perfil
 * não pode usar não aparece.
 */
@Composable
fun MoreScreen(
    onOpenHelp: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUsers: () -> Unit,
    viewModel: MoreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MoreContent(state = state, onOpenHelp = onOpenHelp, onOpenSettings = onOpenSettings, onOpenUsers = onOpenUsers)
}

@Composable
internal fun MoreContent(
    state: MoreUiState,
    onOpenHelp: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUsers: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.more_title), help = HelpTips.More) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LargeButton(text = stringResource(R.string.more_settings), icon = Icons.Default.Settings, onClick = onOpenSettings)
            if (state.canManageUsers) {
                LargeButton(text = stringResource(R.string.more_users), icon = Icons.Default.Person, onClick = onOpenUsers)
            }
            LargeButton(text = stringResource(R.string.more_help), icon = Icons.Default.Info, onClick = onOpenHelp)
        }
    }
}

@Preview(name = "Mais do administrador", showBackground = true, fontScale = 2f)
@Composable
private fun MoreAdminPreview() {
    AquaGestorTheme {
        MoreContent(state = MoreUiState(canManageUsers = true), onOpenHelp = {}, onOpenSettings = {}, onOpenUsers = {})
    }
}

@Preview(name = "Mais do funcionário", showBackground = true, fontScale = 2f)
@Composable
private fun MoreEmployeePreview() {
    AquaGestorTheme {
        MoreContent(state = MoreUiState(canManageUsers = false), onOpenHelp = {}, onOpenSettings = {}, onOpenUsers = {})
    }
}
