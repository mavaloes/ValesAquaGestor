package com.valoes.valesaquagestor.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel da Home provisória (Fase 04). O logout com confirmação e aviso de registros não
 * enviados é da Fase 08, em Configurações; aqui o "Sair" encerra a sessão direto.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** Nome do usuário logado, ou `null` enquanto carrega. */
    val userName: StateFlow<String?> = authRepository.currentSession
        .map { it?.nome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** Encerra a sessão. A navegação para o Login acontece sozinha, porque a sessão deixa de existir. */
    fun onSignOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
