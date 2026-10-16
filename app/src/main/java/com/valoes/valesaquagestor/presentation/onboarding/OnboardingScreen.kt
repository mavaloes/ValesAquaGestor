package com.valoes.valesaquagestor.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/** Uma tela do tutorial: ícone, título e texto curto. */
private class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val text: Int,
    val icon: ImageVector,
)

private val pages = listOf(
    OnboardingPage(R.string.onboarding_page1_title, R.string.onboarding_page1_text, Icons.Default.Info),
    OnboardingPage(R.string.onboarding_page2_title, R.string.onboarding_page2_text, Icons.Default.Check),
    OnboardingPage(R.string.onboarding_page3_title, R.string.onboarding_page3_text, Icons.Default.Home),
)

/**
 * Tutorial de primeiro acesso: 3 telas curtas e o botão "Pular" sempre visível.
 *
 * @param onCompleted chegou ao fim: segue para a configuração da fazenda.
 * @param onSkipped pulou: vai direto para a Home.
 */
@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    onSkipped: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.exit) {
        when (state.exit) {
            OnboardingExit.COMPLETED -> onCompleted()
            OnboardingExit.SKIPPED -> onSkipped()
            null -> Unit
        }
    }
    // Voltar do sistema volta uma tela do tutorial (na primeira, deixa o comportamento padrão).
    BackHandler(enabled = !state.isFirstPage, onBack = viewModel::onPrevious)
    OnboardingContent(
        state = state,
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onSkip = viewModel::onSkip,
    )
}

@Composable
internal fun OnboardingContent(
    state: OnboardingUiState,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
) {
    val page = pages[state.page]
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // "Pular" fica sempre no topo, em qualquer tela do tutorial.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.onboarding_page_counter, state.page + 1, OnboardingViewModel.PAGE_COUNT),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(text = stringResource(R.string.onboarding_skip), style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
            // A figura é só enfeite: o título logo abaixo diz o mesmo, então o leitor de tela a ignora.
            Icon(
                imageVector = page.icon,
                contentDescription = stringResource(page.title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.CenterHorizontally)
                    .clearAndSetSemantics { },
            )
            Text(
                text = stringResource(page.title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().semantics { heading() },
            )
            Text(
                text = stringResource(page.text),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            LargeButton(
                text = stringResource(if (state.isLastPage) R.string.onboarding_start else R.string.onboarding_next),
                icon = if (state.isLastPage) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                onClick = onNext,
            )
            if (!state.isFirstPage) {
                LargeButton(
                    text = stringResource(R.string.onboarding_back),
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onPrevious,
                )
            }
        }
    }
}

@Preview(name = "Tutorial, tela 1", showBackground = true, fontScale = 2f)
@Composable
private fun OnboardingFirstPreview() {
    AquaGestorTheme { OnboardingContent(OnboardingUiState(page = 0), onNext = {}, onPrevious = {}, onSkip = {}) }
}

@Preview(name = "Tutorial, tela 3", showBackground = true, fontScale = 2f)
@Composable
private fun OnboardingLastPreview() {
    AquaGestorTheme { OnboardingContent(OnboardingUiState(page = 2), onNext = {}, onPrevious = {}, onSkip = {}) }
}
