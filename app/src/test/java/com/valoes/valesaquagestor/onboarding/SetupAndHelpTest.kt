package com.valoes.valesaquagestor.onboarding

import com.valoes.valesaquagestor.auth.MainDispatcherRule
import com.valoes.valesaquagestor.domain.usecase.SetupSuggestion
import com.valoes.valesaquagestor.domain.usecase.resolveSetupSuggestion
import com.valoes.valesaquagestor.activities.FakeActivityRepository
import com.valoes.valesaquagestor.farm.FIXED_CLOCK
import com.valoes.valesaquagestor.farm.FarmFixture
import com.valoes.valesaquagestor.farm.lot
import com.valoes.valesaquagestor.farm.tank
import com.valoes.valesaquagestor.presentation.help.helpGlossary
import com.valoes.valesaquagestor.presentation.help.helpTutorials
import com.valoes.valesaquagestor.presentation.home.HomeViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Retomar a configuração da fazenda a partir da Home e o conteúdo da tela de Ajuda. */
class SetupAndHelpTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private fun suggestion(farm: FarmFixture) = runBlocking { resolveSetupSuggestion(farm.overview().first()) }

    @Test
    fun `fazenda sem tanque sugere criar o primeiro tanque`() {
        assertEquals(SetupSuggestion.CreateTank, suggestion(FarmFixture()))
    }

    @Test
    fun `tanque sem nenhum lote sugere criar o primeiro lote nele`() {
        val farm = FarmFixture(tanks = listOf(tank(2, "Viveiro B"), tank(1, "Viveiro A")))

        assertEquals(SetupSuggestion.CreateLot(tankId = 1, tankName = "Viveiro A"), suggestion(farm))
    }

    @Test
    fun `com um lote ativo em qualquer tanque a fazenda esta configurada`() {
        val farm = FarmFixture(tanks = listOf(tank(1, "Viveiro A"), tank(2, "Viveiro B")), lots = listOf(lot(1, tankId = 2)))

        assertEquals(SetupSuggestion.None, suggestion(farm))
    }

    @Test
    fun `a Home oferece o atalho e some quando a configuracao termina`() = runBlocking {
        val farm = FarmFixture()
        val viewModel = HomeViewModel(farm.overview, FIXED_CLOCK, FakeActivityRepository())
        assertEquals(SetupSuggestion.CreateTank, viewModel.state.first { !it.isLoading }.setup)

        farm.tanks.save(tank(1, "Viveiro 1"))
        assertEquals(
            SetupSuggestion.CreateLot(1, "Viveiro 1"),
            viewModel.state.first { it.setup is SetupSuggestion.CreateLot }.setup,
        )

        farm.lots.save(lot(1, tankId = 1))
        assertEquals(SetupSuggestion.None, viewModel.state.first { it.setup == SetupSuggestion.None && it.hasTanks }.setup)
    }

    @Test
    fun `a Ajuda tem os 3 passo a passo pedidos, cada um com passos numerados`() {
        assertEquals(3, helpTutorials.size)
        assertTrue(helpTutorials.all { it.steps.size >= 3 })
        assertEquals("sem textos repetidos entre os tutoriais", helpTutorials.size, helpTutorials.map { it.title }.toSet().size)
    }

    @Test
    fun `o glossario da Ajuda cobre os termos do campo, sem repetir`() {
        assertTrue(helpGlossary.size >= 12)
        assertEquals(helpGlossary.size, helpGlossary.map { it.term }.toSet().size)
    }
}
