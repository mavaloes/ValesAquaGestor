package com.valoes.valesaquagestor.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Estado do app ao abrir: lendo a sessão salva, ou pronto com a sessão (que pode ser `null`). */
sealed interface AppState {
    data object Loading : AppState
    data class Ready(val session: Session?) : AppState
}

/** Lê a sessão salva no aparelho (sem internet) e a repassa para decidir a tela inicial. */
@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    val state: StateFlow<AppState> = authRepository.currentSession
        .map<Session?, AppState> { AppState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppState.Loading)

    private companion object {
        /** Tempo de espera antes de parar de ler a sessão quando a tela sai de cena (ex.: rotação). */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
