package com.valoes.valesaquagestor.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.permission.Permission
import com.valoes.valesaquagestor.domain.permission.PermissionChecker
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Estado da aba "Mais". O menu mostra só o que o perfil pode usar: nada de item desabilitado
 * (`specs/06-perfis-permissoes.md`).
 */
data class MoreUiState(val canManageUsers: Boolean = false)

/** ViewModel da aba "Mais". O "Sair" fica nas Configurações, com o aviso de registros não enviados. */
@HiltViewModel
class MoreViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    val state: StateFlow<MoreUiState> = authRepository.currentSession
        .map { session -> MoreUiState(canManageUsers = PermissionChecker.isAllowed(session?.papel, Permission.MANAGE_USERS)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MoreUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
