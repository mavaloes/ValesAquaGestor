package com.valoes.valesaquagestor.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.Usuario
import com.valoes.valesaquagestor.domain.permission.Permission
import com.valoes.valesaquagestor.domain.permission.PermissionChecker
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.UserRepository
import com.valoes.valesaquagestor.domain.usecase.ChangeUserRole
import com.valoes.valesaquagestor.domain.usecase.DeactivateUser
import com.valoes.valesaquagestor.domain.usecase.InviteUser
import com.valoes.valesaquagestor.domain.usecase.UseCaseError
import com.valoes.valesaquagestor.domain.usecase.UseCaseResult
import com.valoes.valesaquagestor.presentation.util.FormError
import com.valoes.valesaquagestor.presentation.util.FormValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Erro do campo e-mail do convite. */
enum class InviteEmailError {
    INVALID,
    IN_USE,
}

/** Formulário de convite. Os campos ficam como texto enquanto o usuário digita. */
data class InviteForm(
    val nome: String = "",
    val email: String = "",
    val papel: Papel = Papel.FUNCIONARIO,
    val nomeError: FormError? = null,
    val emailError: InviteEmailError? = null,
) {
    /** O botão Convidar só habilita com nome e e-mail preenchidos. */
    val canSend: Boolean get() = nome.isNotBlank() && email.isNotBlank()
}

/** Aviso que a tela mostra depois de uma ação. */
sealed interface UsersMessage {
    data class InviteCreated(val email: String) : UsersMessage

    /** Sem internet: o convite está guardado e sai quando a conexão voltar. */
    data object InvitePendingOffline : UsersMessage
    data object RoleChanged : UsersMessage
    data object Deactivated : UsersMessage
    data class Failed(val error: UseCaseError) : UsersMessage
}

/** Uma linha da lista. [isCurrentUser] marca "Você" e esconde as ações (ninguém muda o próprio perfil). */
data class UserRow(val usuario: Usuario, val isCurrentUser: Boolean)

/**
 * Estado da tela de usuários.
 *
 * @property allowed `false` quando o perfil não pode gerenciar usuários (a tela mostra o aviso).
 * @property invite formulário de convite aberto, ou `null`.
 * @property deactivating usuário que está sendo confirmado para desativar, ou `null`.
 */
data class UsersUiState(
    val isLoading: Boolean = true,
    val allowed: Boolean = false,
    val users: List<UserRow> = emptyList(),
    val invite: InviteForm? = null,
    val deactivating: Usuario? = null,
    val isWorking: Boolean = false,
    val message: UsersMessage? = null,
)

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    userRepository: UserRepository,
    private val inviteUser: InviteUser,
    private val changeUserRole: ChangeUserRole,
    private val deactivateUser: DeactivateUser,
) : ViewModel() {

    /** O que só existe na tela: formulário aberto, confirmação, aviso. A lista vem do banco. */
    private data class Local(
        val invite: InviteForm? = null,
        val deactivating: Usuario? = null,
        val isWorking: Boolean = false,
        val message: UsersMessage? = null,
    )

    private val local = MutableStateFlow(Local())

    val state: StateFlow<UsersUiState> = combine(
        authRepository.currentSession,
        userRepository.observeAll(),
        local,
    ) { session, users, local ->
        UsersUiState(
            isLoading = false,
            allowed = PermissionChecker.isAllowed(session?.papel, Permission.MANAGE_USERS),
            users = users.map { UserRow(it, isCurrentUser = it.uuid == session?.userId) },
            invite = local.invite,
            deactivating = local.deactivating,
            isWorking = local.isWorking,
            message = local.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UsersUiState())

    init {
        // Quem está logado também aparece na lista, com o perfil da sessão.
        viewModelScope.launch {
            authRepository.currentSession.first()?.let { userRepository.ensureFromSession(it) }
        }
    }

    fun onInviteClick() = local.update { it.copy(invite = InviteForm()) }

    fun onInviteDismiss() = local.update { it.copy(invite = null) }

    fun onInviteNameChange(value: String) =
        local.update { s -> s.copy(invite = s.invite?.copy(nome = value, nomeError = null)) }

    fun onInviteEmailChange(value: String) =
        local.update { s -> s.copy(invite = s.invite?.copy(email = value, emailError = null)) }

    fun onInviteRoleChange(value: Papel) = local.update { s -> s.copy(invite = s.invite?.copy(papel = value)) }

    /** Valida o formulário e convida. Ignora o toque enquanto outra ação está em andamento. */
    fun onInviteSend() {
        val current = local.value
        val form = current.invite ?: return
        if (current.isWorking) return
        val nameError = FormValidation.requiredText(form.nome)
        if (nameError != null) {
            local.update { s -> s.copy(invite = form.copy(nomeError = nameError)) }
            return
        }
        local.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            when (val result = inviteUser(form.nome, form.email, form.papel)) {
                is UseCaseResult.Success -> local.update {
                    it.copy(
                        invite = null,
                        isWorking = false,
                        message = if (result.value.pendingOffline) {
                            UsersMessage.InvitePendingOffline
                        } else {
                            UsersMessage.InviteCreated(result.value.usuario.email)
                        },
                    )
                }

                is UseCaseResult.Failure -> local.update { s ->
                    when (result.error) {
                        UseCaseError.INVALID_EMAIL ->
                            s.copy(isWorking = false, invite = form.copy(emailError = InviteEmailError.INVALID))

                        UseCaseError.EMAIL_ALREADY_IN_USE ->
                            s.copy(isWorking = false, invite = form.copy(emailError = InviteEmailError.IN_USE))

                        else -> s.copy(isWorking = false, message = UsersMessage.Failed(result.error))
                    }
                }
            }
        }
    }

    /** Troca entre administrador e funcionário. */
    fun onToggleRole(user: Usuario) {
        if (local.value.isWorking) return
        val newRole = if (user.papel == Papel.ADMIN) Papel.FUNCIONARIO else Papel.ADMIN
        local.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val message = when (val result = changeUserRole(user.uuid, newRole)) {
                is UseCaseResult.Success -> UsersMessage.RoleChanged
                is UseCaseResult.Failure -> UsersMessage.Failed(result.error)
            }
            local.update { it.copy(isWorking = false, message = message) }
        }
    }

    fun onDeactivateClick(user: Usuario) = local.update { it.copy(deactivating = user) }

    fun onDeactivateDismiss() = local.update { it.copy(deactivating = null) }

    fun onDeactivateConfirm() {
        val user = local.value.deactivating ?: return
        if (local.value.isWorking) return
        local.update { it.copy(deactivating = null, isWorking = true) }
        viewModelScope.launch {
            val message = when (val result = deactivateUser(user.uuid)) {
                is UseCaseResult.Success -> UsersMessage.Deactivated
                is UseCaseResult.Failure -> UsersMessage.Failed(result.error)
            }
            local.update { it.copy(isWorking = false, message = message) }
        }
    }

    fun onMessageShown() = local.update { it.copy(message = null) }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
