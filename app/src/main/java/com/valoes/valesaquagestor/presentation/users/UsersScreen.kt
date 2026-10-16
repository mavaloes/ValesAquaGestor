package com.valoes.valesaquagestor.presentation.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.StatusUsuario
import com.valoes.valesaquagestor.domain.model.Usuario
import com.valoes.valesaquagestor.presentation.components.AppTextField
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.ChoiceGroup
import com.valoes.valesaquagestor.presentation.components.ConfirmDialog
import com.valoes.valesaquagestor.presentation.components.EmptyMessage
import com.valoes.valesaquagestor.presentation.components.FeedbackEffect
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.requiredLabel
import com.valoes.valesaquagestor.presentation.settings.labelRes
import com.valoes.valesaquagestor.presentation.settings.messageRes
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.stringRes

/** Usuários da fazenda (só ADMIN): listar, convidar por e-mail, trocar perfil e desativar. */
@Composable
fun UsersScreen(onBack: () -> Unit, viewModel: UsersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message
    val inviteCreated = (message as? UsersMessage.InviteCreated)?.let { stringResource(R.string.users_invite_created, it.email) }
    val failure = (message as? UsersMessage.Failed)?.let { stringResource(it.error.messageRes()) }
    val offline = stringResource(R.string.users_invite_offline)
    val roleChanged = stringResource(R.string.users_role_changed)
    val deactivated = stringResource(R.string.users_deactivated)
    FeedbackEffect(
        message = when (message) {
            null -> null
            is UsersMessage.InviteCreated -> inviteCreated
            UsersMessage.InvitePendingOffline -> offline
            UsersMessage.RoleChanged -> roleChanged
            UsersMessage.Deactivated -> deactivated
            is UsersMessage.Failed -> failure
        },
        hostState = snackbarHostState,
        onShown = viewModel::onMessageShown,
    )
    UsersContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onInviteClick = viewModel::onInviteClick,
        onInviteDismiss = viewModel::onInviteDismiss,
        onInviteNameChange = viewModel::onInviteNameChange,
        onInviteEmailChange = viewModel::onInviteEmailChange,
        onInviteRoleChange = viewModel::onInviteRoleChange,
        onInviteSend = viewModel::onInviteSend,
        onToggleRole = viewModel::onToggleRole,
        onDeactivateClick = viewModel::onDeactivateClick,
        onDeactivateConfirm = viewModel::onDeactivateConfirm,
        onDeactivateDismiss = viewModel::onDeactivateDismiss,
    )
}

@Composable
internal fun UsersContent(
    state: UsersUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onInviteClick: () -> Unit,
    onInviteDismiss: () -> Unit,
    onInviteNameChange: (String) -> Unit,
    onInviteEmailChange: (String) -> Unit,
    onInviteRoleChange: (Papel) -> Unit,
    onInviteSend: () -> Unit,
    onToggleRole: (Usuario) -> Unit,
    onDeactivateClick: (Usuario) -> Unit,
    onDeactivateConfirm: () -> Unit,
    onDeactivateDismiss: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.users_title), onBack = onBack, help = HelpTips.Users) },
        snackbarHost = { FeedbackSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (!state.isLoading && !state.allowed) {
            Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                EmptyMessage(stringResource(R.string.no_permission))
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                LargeButton(
                    text = stringResource(R.string.users_invite),
                    icon = Icons.Default.Add,
                    onClick = onInviteClick,
                    enabled = !state.isWorking,
                )
            }
            if (state.users.isEmpty() && !state.isLoading) {
                item { EmptyMessage(stringResource(R.string.users_empty)) }
            }
            items(state.users, key = { it.usuario.uuid }) { row ->
                UserCard(row = row, enabled = !state.isWorking, onToggleRole = onToggleRole, onDeactivate = onDeactivateClick)
            }
        }
    }
    state.invite?.let { form ->
        InviteDialog(
            form = form,
            isWorking = state.isWorking,
            onNameChange = onInviteNameChange,
            onEmailChange = onInviteEmailChange,
            onRoleChange = onInviteRoleChange,
            onSend = onInviteSend,
            onDismiss = onInviteDismiss,
        )
    }
    state.deactivating?.let { user ->
        ConfirmDialog(
            title = stringResource(R.string.users_deactivate_title),
            text = stringResource(R.string.users_deactivate_text, user.nome),
            confirmText = stringResource(R.string.users_deactivate),
            onConfirm = onDeactivateConfirm,
            onDismiss = onDeactivateDismiss,
        )
    }
}

@Composable
private fun UserCard(
    row: UserRow,
    enabled: Boolean,
    onToggleRole: (Usuario) -> Unit,
    onDeactivate: (Usuario) -> Unit,
) {
    val user = row.usuario
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (row.isCurrentUser) stringResource(R.string.user_name_you, user.nome) else user.nome,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(text = user.email, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(
                    R.string.user_role_status,
                    stringResource(user.papel.labelRes()),
                    stringResource(user.status.labelRes()),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!user.sincronizado) {
                Text(
                    text = stringResource(R.string.user_waiting_sync),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!row.isCurrentUser && user.status != StatusUsuario.DESATIVADO) {
                TextButton(
                    onClick = { onToggleRole(user) },
                    enabled = enabled,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(if (user.papel == Papel.ADMIN) R.string.users_make_employee else R.string.users_make_admin),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                TextButton(
                    onClick = { onDeactivate(user) },
                    enabled = enabled,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(R.string.users_deactivate), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun InviteDialog(
    form: InviteForm,
    isWorking: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onRoleChange: (Papel) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.users_invite_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = stringResource(R.string.users_invite_hint), style = MaterialTheme.typography.bodyLarge)
                AppTextField(
                    value = form.nome,
                    onValueChange = onNameChange,
                    label = requiredLabel(stringResource(R.string.field_name)),
                    placeholder = stringResource(R.string.field_name_example),
                    errorText = form.nomeError?.let { stringResource(it.stringRes()) },
                    enabled = !isWorking,
                )
                AppTextField(
                    value = form.email,
                    onValueChange = onEmailChange,
                    label = requiredLabel(stringResource(R.string.field_email)),
                    placeholder = stringResource(R.string.field_email_example),
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    onImeAction = { if (form.canSend) onSend() },
                    errorText = form.emailError?.let {
                        stringResource(if (it == InviteEmailError.IN_USE) R.string.users_error_email_in_use else R.string.error_invalid_email)
                    },
                    enabled = !isWorking,
                )
                ChoiceGroup(
                    label = stringResource(R.string.users_field_role),
                    options = Papel.entries,
                    selected = form.papel,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelect = onRoleChange,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSend, enabled = form.canSend && !isWorking) {
                Text(stringResource(R.string.users_invite_send))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private val previewUsers = listOf(
    UserRow(Usuario(1, "u1", "Maria Silva", "maria@exemplo.com", Papel.ADMIN, StatusUsuario.ATIVO, sincronizado = true), isCurrentUser = true),
    UserRow(Usuario(2, "u2", "João Souza", "joao@exemplo.com", Papel.FUNCIONARIO, StatusUsuario.CONVIDADO), isCurrentUser = false),
    UserRow(Usuario(3, "u3", "Ana Lima", "ana@exemplo.com", Papel.FUNCIONARIO, StatusUsuario.DESATIVADO, sincronizado = true), isCurrentUser = false),
)

@Preview(name = "Usuários", showBackground = true, fontScale = 2f)
@Composable
private fun UsersPreview() {
    AquaGestorTheme {
        UsersContent(
            state = UsersUiState(isLoading = false, allowed = true, users = previewUsers),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onInviteClick = {}, onInviteDismiss = {}, onInviteNameChange = {}, onInviteEmailChange = {},
            onInviteRoleChange = {}, onInviteSend = {}, onToggleRole = {}, onDeactivateClick = {},
            onDeactivateConfirm = {}, onDeactivateDismiss = {},
        )
    }
}

@Preview(name = "Convite", showBackground = true, fontScale = 2f)
@Composable
private fun InvitePreview() {
    AquaGestorTheme {
        UsersContent(
            state = UsersUiState(
                isLoading = false, allowed = true, users = previewUsers,
                invite = InviteForm(nome = "Pedro", email = "pedro@", emailError = InviteEmailError.INVALID),
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onInviteClick = {}, onInviteDismiss = {}, onInviteNameChange = {}, onInviteEmailChange = {},
            onInviteRoleChange = {}, onInviteSend = {}, onToggleRole = {}, onDeactivateClick = {},
            onDeactivateConfirm = {}, onDeactivateDismiss = {},
        )
    }
}

@Preview(name = "Usuários sem permissão", showBackground = true, fontScale = 2f)
@Composable
private fun UsersDeniedPreview() {
    AquaGestorTheme {
        UsersContent(
            state = UsersUiState(isLoading = false, allowed = false),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onInviteClick = {}, onInviteDismiss = {}, onInviteNameChange = {}, onInviteEmailChange = {},
            onInviteRoleChange = {}, onInviteSend = {}, onToggleRole = {}, onDeactivateClick = {},
            onDeactivateConfirm = {}, onDeactivateDismiss = {},
        )
    }
}
