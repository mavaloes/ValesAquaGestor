package com.valoes.valesaquagestor.auth

import com.valoes.valesaquagestor.settings.FakeUserRepository
import com.valoes.valesaquagestor.data.auth.FakeAuthRepository
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.presentation.navigation.AppState
import com.valoes.valesaquagestor.presentation.navigation.AppViewModel
import com.valoes.valesaquagestor.presentation.navigation.SessionNavigation
import com.valoes.valesaquagestor.presentation.navigation.StartDestination
import com.valoes.valesaquagestor.presentation.navigation.resolveSessionNavigation
import com.valoes.valesaquagestor.presentation.navigation.resolveStartDestination
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Decisão da tela inicial (`specs/10-telas-e-navegacao.md`), com o primeiro acesso da Fase 07. */
class StartDestinationTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val session = Session("u1", "Maria", "maria@exemplo.com", Papel.ADMIN)

    @Test
    fun `sem sessao a tela inicial e o Login, concluido ou nao`() {
        assertEquals(StartDestination.LOGIN, resolveStartDestination(null, onboardingCompleted = false))
        assertEquals(StartDestination.LOGIN, resolveStartDestination(null, onboardingCompleted = true))
    }

    @Test
    fun `com sessao e primeiro acesso nao concluido a tela inicial e o Onboarding`() {
        assertEquals(StartDestination.ONBOARDING, resolveStartDestination(session, onboardingCompleted = false))
    }

    @Test
    fun `com sessao e primeiro acesso concluido a tela inicial e a Home`() {
        assertEquals(StartDestination.HOME, resolveStartDestination(session, onboardingCompleted = true))
    }

    @Test
    fun `com sessao salva e sem internet o app abre na Home`() = runBlocking {
        val repository = FakeAuthRepository(FakeSessionStore(session), FakeConnectivityChecker(online = false), FakeUserRepository())

        val destination = resolveStartDestination(repository.currentSession.first(), onboardingCompleted = true)

        assertEquals(StartDestination.HOME, destination)
    }

    @Test
    fun `o AppViewModel entrega a sessao e o primeiro acesso salvos ao abrir`() {
        val auth = FakeAuthRepository(FakeSessionStore(session), FakeConnectivityChecker(online = false), FakeUserRepository())
        val viewModel = AppViewModel(auth, FakeOnboardingRepository(completed = true))

        val ready = runBlocking { viewModel.state.first { it is AppState.Ready } }

        assertEquals(AppState.Ready(session, onboardingCompleted = true), ready)
    }

    @Test
    fun `o AppViewModel acompanha o primeiro acesso sendo concluido`() {
        val onboarding = FakeOnboardingRepository(completed = false)
        val auth = FakeAuthRepository(FakeSessionStore(session), FakeConnectivityChecker(online = false), FakeUserRepository())
        val viewModel = AppViewModel(auth, onboarding)
        assertEquals(AppState.Ready(session, onboardingCompleted = false), runBlocking { viewModel.state.first { it is AppState.Ready } })

        runBlocking { onboarding.markCompleted() }

        assertEquals(AppState.Ready(session, onboardingCompleted = true), runBlocking { viewModel.state.first { it == AppState.Ready(session, true) } })
    }

    // ---- mudanças de sessão com o app aberto ----

    @Test
    fun `sessao que acaba fora das telas de login leva ao Login`() {
        assertEquals(SessionNavigation.GO_TO_LOGIN, resolveSessionNavigation(null, onAuthScreen = false, onboardingCompleted = true))
    }

    @Test
    fun `quem entra ou se cadastra vai ao Onboarding se ainda nao fez o primeiro acesso`() {
        assertEquals(SessionNavigation.GO_TO_ONBOARDING, resolveSessionNavigation(session, onAuthScreen = true, onboardingCompleted = false))
    }

    @Test
    fun `quem entra depois do primeiro acesso vai direto a Home`() {
        assertEquals(SessionNavigation.GO_TO_HOME, resolveSessionNavigation(session, onAuthScreen = true, onboardingCompleted = true))
    }

    @Test
    fun `sem mudanca relevante a navegacao fica onde esta`() {
        assertEquals(SessionNavigation.STAY, resolveSessionNavigation(null, onAuthScreen = true, onboardingCompleted = false))
        assertEquals(SessionNavigation.STAY, resolveSessionNavigation(session, onAuthScreen = false, onboardingCompleted = true))
        assertEquals("o Onboarding e a configuração não são telas de login", SessionNavigation.STAY, resolveSessionNavigation(session, onAuthScreen = false, onboardingCompleted = false))
    }
}
