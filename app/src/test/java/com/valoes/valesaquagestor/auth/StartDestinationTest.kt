package com.valoes.valesaquagestor.auth

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

class StartDestinationTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val session = Session("u1", "Maria", "maria@exemplo.com", Papel.ADMIN)

    @Test
    fun `sem sessao a tela inicial e o Login`() {
        assertEquals(StartDestination.LOGIN, resolveStartDestination(null))
    }

    @Test
    fun `com sessao a tela inicial e a Home`() {
        assertEquals(StartDestination.HOME, resolveStartDestination(session))
    }

    @Test
    fun `com sessao salva e sem internet o app abre na Home`() = runBlocking {
        val repository = FakeAuthRepository(FakeSessionStore(session), FakeConnectivityChecker(online = false))

        val destination = resolveStartDestination(repository.currentSession.first())

        assertEquals(StartDestination.HOME, destination)
    }

    @Test
    fun `o AppViewModel entrega a sessao salva ao abrir`() {
        val repository = FakeAuthRepository(FakeSessionStore(session), FakeConnectivityChecker(online = false))
        val viewModel = AppViewModel(repository)

        // O stateIn só começa a ler quando alguém coleta; coleta uma vez e confere.
        val ready = runBlocking { viewModel.state.first { it is AppState.Ready } }

        assertEquals(AppState.Ready(session), ready)
    }

    @Test
    fun `sessao que acaba fora das telas de login leva ao Login`() {
        assertEquals(SessionNavigation.GO_TO_LOGIN, resolveSessionNavigation(null, onAuthScreen = false))
    }

    @Test
    fun `sessao que passa a existir nas telas de login leva a Home`() {
        assertEquals(SessionNavigation.GO_TO_HOME, resolveSessionNavigation(session, onAuthScreen = true))
    }

    @Test
    fun `sem mudanca relevante a navegacao fica onde esta`() {
        assertEquals(SessionNavigation.STAY, resolveSessionNavigation(null, onAuthScreen = true))
        assertEquals(SessionNavigation.STAY, resolveSessionNavigation(session, onAuthScreen = false))
    }
}
