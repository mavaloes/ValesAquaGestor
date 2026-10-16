package com.valoes.valesaquagestor.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** ViewModel da aba "Mais". O "Sair" provisório encerra a sessão direto (a Fase 08 acrescenta a confirmação). */
@HiltViewModel
class MoreViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** Encerra a sessão. A navegação para o Login acontece sozinha, porque a sessão deixa de existir. */
    fun onSignOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
