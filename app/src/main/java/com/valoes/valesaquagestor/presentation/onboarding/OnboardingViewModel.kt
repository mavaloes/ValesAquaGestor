package com.valoes.valesaquagestor.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Como o usuário saiu do tutorial. */
enum class OnboardingExit {
    /** Chegou à última tela e tocou em "Começar": segue para a configuração da fazenda. */
    COMPLETED,

    /** Tocou em "Pular": vai direto para a Home. */
    SKIPPED,
}

/**
 * Estado do tutorial de primeiro acesso.
 *
 * @property page tela atual, de 0 a [OnboardingViewModel.PAGE_COUNT] − 1.
 * @property exit preenchido depois que o "concluído" foi salvo no aparelho: a tela então navega.
 */
data class OnboardingUiState(
    val page: Int = 0,
    val exit: OnboardingExit? = null,
) {
    val isFirstPage: Boolean get() = page == 0
    val isLastPage: Boolean get() = page == OnboardingViewModel.PAGE_COUNT - 1
}

/**
 * Tutorial de no máximo 3 telas curtas, com "Pular" sempre visível (`specs/04-requisitos-ux.md`).
 * Concluir **ou** pular salva "concluído" no aparelho e o tutorial não aparece de novo.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboarding: OnboardingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    /** "Próximo"; na última tela é "Começar" e conclui o tutorial. */
    fun onNext() {
        if (_state.value.isLastPage) finish(OnboardingExit.COMPLETED) else _state.update { it.copy(page = it.page + 1) }
    }

    /** "Voltar" para a tela anterior do tutorial. Na primeira tela não faz nada. */
    fun onPrevious() = _state.update { if (it.isFirstPage) it else it.copy(page = it.page - 1) }

    /** "Pular": salva "concluído" e vai direto para a Home. */
    fun onSkip() = finish(OnboardingExit.SKIPPED)

    private fun finish(exit: OnboardingExit) {
        if (_state.value.exit != null) return
        viewModelScope.launch {
            onboarding.markCompleted()
            _state.update { it.copy(exit = exit) }
        }
    }

    companion object {
        /** Número de telas do tutorial: no máximo 3 (`specs/04`). */
        const val PAGE_COUNT = 3
    }
}
