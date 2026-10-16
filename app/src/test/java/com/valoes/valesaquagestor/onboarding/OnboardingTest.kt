package com.valoes.valesaquagestor.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.valoes.valesaquagestor.auth.FakeOnboardingRepository
import com.valoes.valesaquagestor.auth.MainDispatcherRule
import com.valoes.valesaquagestor.data.onboarding.DataStoreOnboardingRepository
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.presentation.navigation.StartDestination
import com.valoes.valesaquagestor.presentation.navigation.resolveStartDestination
import com.valoes.valesaquagestor.presentation.onboarding.OnboardingExit
import com.valoes.valesaquagestor.presentation.onboarding.OnboardingViewModel
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OnboardingTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val repository = FakeOnboardingRepository()
    private val viewModel = OnboardingViewModel(repository)
    private val session = Session("u1", "Maria", "maria@exemplo.com", Papel.ADMIN)

    private fun completed() = runBlocking { repository.isCompleted.first() }

    // ---- as telas do tutorial ----

    @Test
    fun `o tutorial tem no maximo 3 telas curtas`() {
        assertTrue(OnboardingViewModel.PAGE_COUNT in 1..3)
    }

    @Test
    fun `comeca na primeira tela, sem sair e sem ter salvo concluido`() {
        val state = viewModel.state.value

        assertEquals(0, state.page)
        assertTrue(state.isFirstPage)
        assertFalse(state.isLastPage)
        assertNull(state.exit)
        assertFalse(completed())
    }

    @Test
    fun `proximo avanca uma tela e voltar retorna`() {
        viewModel.onNext()
        assertEquals(1, viewModel.state.value.page)

        viewModel.onPrevious()
        assertEquals(0, viewModel.state.value.page)

        viewModel.onPrevious()
        assertEquals("na primeira tela voltar não faz nada", 0, viewModel.state.value.page)
    }

    @Test
    fun `na ultima tela o proximo conclui, salva concluido e segue para a configuracao`() {
        repeat(OnboardingViewModel.PAGE_COUNT - 1) { viewModel.onNext() }
        assertTrue(viewModel.state.value.isLastPage)
        assertFalse("ainda não concluiu", completed())

        viewModel.onNext()

        assertEquals(OnboardingExit.COMPLETED, viewModel.state.value.exit)
        assertTrue(completed())
    }

    // ---- Pular ----

    @Test
    fun `pular na primeira tela salva concluido e vai direto para a Home`() {
        viewModel.onSkip()

        assertEquals(OnboardingExit.SKIPPED, viewModel.state.value.exit)
        assertTrue("Pular salva como concluído", completed())
    }

    @Test
    fun `pular funciona em qualquer tela do tutorial`() {
        viewModel.onNext()
        viewModel.onNext()

        viewModel.onSkip()

        assertEquals(OnboardingExit.SKIPPED, viewModel.state.value.exit)
        assertTrue(completed())
    }

    @Test
    fun `depois de concluido o tutorial nao aparece de novo`() {
        assertEquals(StartDestination.ONBOARDING, resolveStartDestination(session, completed()))

        viewModel.onSkip()

        assertEquals(StartDestination.HOME, resolveStartDestination(session, completed()))
    }

    @Test
    fun `um segundo toque durante a saida nao muda o resultado`() {
        viewModel.onSkip()
        viewModel.onNext()
        viewModel.onSkip()

        assertEquals(OnboardingExit.SKIPPED, viewModel.state.value.exit)
    }

    // ---- guardado no aparelho ----

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var file: File

    @Before
    fun openDataStore() {
        file = File(folder.root, "prefs.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
    }

    @After
    fun closeDataStore() = scope.cancel()

    @Test
    fun `no aparelho comeca como nao concluido e guarda quando marca`() = runBlocking {
        val store = DataStoreOnboardingRepository(dataStore)
        assertFalse(store.isCompleted.first())

        store.markCompleted()

        assertTrue(store.isCompleted.first())
        // Outra instância sobre o mesmo arquivo (como ao reabrir o app) continua vendo "concluído".
        assertTrue(DataStoreOnboardingRepository(dataStore).isCompleted.first())
    }

    @Test
    fun `marcar duas vezes continua concluido`() = runBlocking {
        val store = DataStoreOnboardingRepository(dataStore)

        store.markCompleted()
        store.markCompleted()

        assertTrue(store.isCompleted.first())
    }
}
