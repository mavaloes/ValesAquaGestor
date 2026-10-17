package com.valoes.valesaquagestor.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.permission.Permission
import com.valoes.valesaquagestor.domain.permission.PermissionChecker
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.PendingSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado da tela de Configurações.
 *
 * @property session quem está logado (`null` só no instante em que a sessão acaba de ser encerrada).
 * @property pendingCount registros deste aparelho que ainda não foram enviados ao servidor.
 * @property showSignOutConfirm se o aviso "Sair da conta?" está aberto.
 */
data class SettingsUiState(
    val session: Session? = null,
    val pendingCount: Int = 0,
    val showSignOutConfirm: Boolean = false,
    val isSigningOut: Boolean = false,
) {
    /** Faixas da água e agenda: só o ADMIN vê e edita (`specs/06-perfis-permissoes.md`). */
    val canManageSettings: Boolean
        get() = PermissionChecker.isAllowed(session?.papel, Permission.MANAGE_SETTINGS)
}

/** Mostra a conta, leva às configurações do ADMIN e cuida do "Sair" com o aviso de pendências. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    pendingSyncRepository: PendingSyncRepository,
) : ViewModel() {

    private val dialog = MutableStateFlow(SignOutDialog())

    val state: StateFlow<SettingsUiState> = combine(
        authRepository.currentSession,
        pendingSyncRepository.observePendingCount(),
        dialog,
    ) { session, pending, signOut ->
        SettingsUiState(
            session = session,
            pendingCount = pending,
            showSignOutConfirm = signOut.showConfirm,
            isSigningOut = signOut.isSigningOut,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

    /** Abre o aviso. O número de pendências vem do estado, então o texto já mostra a quantidade certa. */
    fun onSignOutClick() = dialog.update { it.copy(showConfirm = true) }

    fun onSignOutDismiss() = dialog.update { it.copy(showConfirm = false) }

    /** Encerra a sessão. A navegação para o Login acontece sozinha, porque a sessão deixa de existir. */
    fun onSignOutConfirm() {
        if (dialog.value.isSigningOut) return
        dialog.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            authRepository.signOut()
            dialog.update { SignOutDialog() }
        }
    }

    private data class SignOutDialog(val showConfirm: Boolean = false, val isSigningOut: Boolean = false)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
