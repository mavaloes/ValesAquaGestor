package com.valoes.valesaquagestor.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.ConfirmDialog
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.SectionTitle
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/**
 * Configurações. Todos veem a própria conta e o "Sair"; as faixas da água e a agenda de medição só
 * aparecem para o ADMIN (`specs/06-perfis-permissoes.md`).
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenWaterRanges: () -> Unit,
    onOpenSchedule: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsContent(
        state = state,
        onBack = onBack,
        onOpenWaterRanges = onOpenWaterRanges,
        onOpenSchedule = onOpenSchedule,
        onSignOutClick = viewModel::onSignOutClick,
        onSignOutConfirm = viewModel::onSignOutConfirm,
        onSignOutDismiss = viewModel::onSignOutDismiss,
    )
}

@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onOpenWaterRanges: () -> Unit,
    onOpenSchedule: () -> Unit,
    onSignOutClick: () -> Unit,
    onSignOutConfirm: () -> Unit,
    onSignOutDismiss: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.settings_title), onBack = onBack, help = HelpTips.Settings) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.session?.let { AccountCard(it) }
            if (state.canManageSettings) {
                SectionTitle(stringResource(R.string.settings_farm_title))
                LargeButton(
                    text = stringResource(R.string.settings_water_ranges),
                    icon = Icons.Default.Settings,
                    onClick = onOpenWaterRanges,
                )
                LargeButton(
                    text = stringResource(R.string.settings_schedule),
                    icon = Icons.Default.DateRange,
                    onClick = onOpenSchedule,
                )
            }
            LargeButton(
                text = stringResource(R.string.settings_sign_out),
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = onSignOutClick,
                isLoading = state.isSigningOut,
            )
        }
    }
    if (state.showSignOutConfirm) {
        // O aviso diz quantos registros ainda não foram enviados (`specs/11-autenticacao.md`, "Sair").
        val explanation = stringResource(R.string.sign_out_text)
        val text = if (state.pendingCount > 0) {
            pluralStringResource(R.plurals.sign_out_pending, state.pendingCount, state.pendingCount) + "\n\n" + explanation
        } else {
            explanation
        }
        ConfirmDialog(
            title = stringResource(R.string.sign_out_title),
            text = text,
            confirmText = stringResource(R.string.sign_out_confirm),
            onConfirm = onSignOutConfirm,
            onDismiss = onSignOutDismiss,
        )
    }
}

@Composable
private fun AccountCard(session: Session) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionTitle(stringResource(R.string.settings_account_title))
            Text(stringResource(R.string.settings_account_name, session.nome), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.settings_account_email, session.email), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.settings_account_role, stringResource(session.papel.labelRes())),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private val previewAdmin = Session("u1", "Maria Silva", "maria@exemplo.com", Papel.ADMIN)

@Preview(name = "Configurações do administrador", showBackground = true, fontScale = 2f)
@Composable
private fun SettingsAdminPreview() {
    AquaGestorTheme {
        SettingsContent(
            state = SettingsUiState(session = previewAdmin, pendingCount = 3),
            onBack = {}, onOpenWaterRanges = {}, onOpenSchedule = {},
            onSignOutClick = {}, onSignOutConfirm = {}, onSignOutDismiss = {},
        )
    }
}

@Preview(name = "Configurações do funcionário", showBackground = true, fontScale = 2f)
@Composable
private fun SettingsEmployeePreview() {
    AquaGestorTheme {
        SettingsContent(
            state = SettingsUiState(session = previewAdmin.copy(nome = "João", papel = Papel.FUNCIONARIO)),
            onBack = {}, onOpenWaterRanges = {}, onOpenSchedule = {},
            onSignOutClick = {}, onSignOutConfirm = {}, onSignOutDismiss = {},
        )
    }
}

@Preview(name = "Sair com registros pendentes", showBackground = true, fontScale = 2f)
@Composable
private fun SettingsSignOutPreview() {
    AquaGestorTheme {
        SettingsContent(
            state = SettingsUiState(session = previewAdmin, pendingCount = 3, showSignOutConfirm = true),
            onBack = {}, onOpenWaterRanges = {}, onOpenSchedule = {},
            onSignOutClick = {}, onSignOutConfirm = {}, onSignOutDismiss = {},
        )
    }
}
