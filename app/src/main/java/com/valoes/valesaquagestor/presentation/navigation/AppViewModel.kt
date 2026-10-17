package com.valoes.valesaquagestor.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Estado do app ao abrir: lendo o que está salvo no aparelho, ou pronto com a sessão (que pode ser
 * `null`) e a informação de que o primeiro acesso já foi concluído.
 */
sealed interface AppState {
    data object Loading : AppState
    data class Ready(val session: Session?, val onboardingCompleted: Boolean) : AppState
}

/** Lê a sessão e o "primeiro acesso concluído" salvos no aparelho (sem internet) para decidir a tela inicial. */
@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
    onboardingRepository: OnboardingRepository,
) : ViewModel() {

    val state: StateFlow<AppState> = combine(
        authRepository.currentSession,
        onboardingRepository.isCompleted,
    ) { session, completed -> AppState.Ready(session, completed) as AppState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppState.Loading)

    private companion object {
        /** Tempo de espera antes de parar de ler quando a tela sai de cena (ex.: rotação). */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
