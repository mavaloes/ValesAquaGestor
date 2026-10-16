package com.valoes.valesaquagestor.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.presentation.auth.ForgotPasswordScreen
import com.valoes.valesaquagestor.presentation.auth.LoginScreen
import com.valoes.valesaquagestor.presentation.auth.SignUpScreen
import com.valoes.valesaquagestor.presentation.home.HomeScreen
import com.valoes.valesaquagestor.presentation.lot.LotDetailScreen
import com.valoes.valesaquagestor.presentation.lot.LotFormScreen
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.LocalOpenHelp
import com.valoes.valesaquagestor.presentation.help.HelpScreen
import com.valoes.valesaquagestor.presentation.main.MoreScreen
import com.valoes.valesaquagestor.presentation.main.RegisterScreen
import com.valoes.valesaquagestor.presentation.onboarding.OnboardingScreen
import com.valoes.valesaquagestor.presentation.record.BiometryScreen
import com.valoes.valesaquagestor.presentation.record.FeedingRecordScreen
import com.valoes.valesaquagestor.presentation.record.MovementScreen
import com.valoes.valesaquagestor.presentation.record.WaterRecordScreen
import com.valoes.valesaquagestor.presentation.setup.SetupFarmScreen
import com.valoes.valesaquagestor.presentation.tank.TankDetailScreen
import com.valoes.valesaquagestor.presentation.tank.TankFormScreen
import com.valoes.valesaquagestor.presentation.tank.TankListScreen
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import com.valoes.valesaquagestor.presentation.util.SaveFeedback
import com.valoes.valesaquagestor.presentation.util.stringRes
import kotlinx.coroutines.launch

/**
 * Raiz do app. Lê a sessão salva (sem internet) e só então monta a navegação, já na tela certa:
 * assim, quem tem sessão nunca vê o Login piscar.
 */
@Composable
fun AquaGestorApp(viewModel: AppViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        AppState.Loading -> LoadingScreen()
        is AppState.Ready -> AppNavigation(session = current.session, onboardingCompleted = current.onboardingCompleted)
    }
}

@Composable
private fun AppNavigation(session: Session?, onboardingCompleted: Boolean) {
    val navController = rememberNavController()
    // A tela inicial é decidida uma vez, pelo que foi lido ao abrir: sessão e primeiro acesso.
    val startDestination = remember { resolveStartDestination(session, onboardingCompleted) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val openHelp: () -> Unit = { navController.navigate(Help) }

    // Aviso de "Registro salvo" (ou "Sem conexão: dados guardados no aparelho") que continua na tela
    // para onde o formulário volta.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessages = SaveFeedback.entries.associateWith { stringResource(it.stringRes()) }
    val finishRecord: (SaveFeedback) -> Unit = { feedback ->
        navController.popBackStack()
        scope.launch { snackbarHostState.showSnackbar(savedMessages.getValue(feedback)) }
    }

    // O menu inferior só aparece nas quatro abas. As telas cuidam das próprias margens do sistema;
    // o `consumeWindowInsets` evita somar a margem de baixo duas vezes quando o menu está visível.
    CompositionLocalProvider(LocalOpenHelp provides openHelp) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { FeedbackSnackbarHost(snackbarHostState) },
            bottomBar = {
                if (currentDestination.isTopLevel()) {
                    AppBottomBar(currentDestination = currentDestination, onNavigate = navController::navigateToTab)
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = when (startDestination) {
                    StartDestination.LOGIN -> Login
                    StartDestination.ONBOARDING -> Onboarding
                    StartDestination.HOME -> Home
                },
                modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
            ) {
                composable<Login> {
                    LoginScreen(
                        onForgotPassword = { navController.navigate(ForgotPassword) },
                        onSignUp = { navController.navigate(SignUp) },
                    )
                }
                composable<SignUp> { SignUpScreen(onBackToLogin = { navController.popBackStack() }) }
                composable<ForgotPassword> { ForgotPasswordScreen(onBackToLogin = { navController.popBackStack() }) }

                composable<Home> {
                    HomeScreen(
                        onOpenTank = { navController.navigate(TankDetail(it)) },
                        onOpenLot = { navController.navigate(LotDetail(it)) },
                        onRegisterWater = { navController.navigate(WaterRecord(it)) },
                        onRegisterFeeding = { navController.navigate(FeedingRecord(it)) },
                        onRegisterBiometry = { navController.navigate(Biometry(it)) },
                        onSetupFarm = { navController.navigate(SetupFarm(it)) },
                    )
                }
                composable<Tanques> {
                    TankListScreen(
                        onOpenTank = { navController.navigate(TankDetail(it)) },
                        onNewTank = { navController.navigate(NewTank) },
                    )
                }
                composable<Registrar> {
                    RegisterScreen(
                        onRegisterWater = { navController.navigate(WaterRecord(it)) },
                        onRegisterFeeding = { navController.navigate(FeedingRecord(it)) },
                        onRegisterBiometry = { navController.navigate(Biometry(it)) },
                        onRegisterMovement = { navController.navigate(MortalityRecord(it)) },
                    )
                }
                composable<Mais> { MoreScreen(onOpenHelp = openHelp) }
                composable<Help> { HelpScreen(onBack = { navController.popBackStack() }) }

                composable<Onboarding> {
                    OnboardingScreen(
                        // Concluiu o tutorial: segue para a configuração da fazenda (que também pode ser pulada).
                        onCompleted = { navController.navigate(SetupFarm()) { popUpTo<Onboarding> { inclusive = true } } },
                        // Pulou: vai direto para a Home, que oferece retomar a configuração.
                        onSkipped = { navController.navigate(Home) { popUpTo<Onboarding> { inclusive = true } } },
                    )
                }
                composable<SetupFarm> { entry ->
                    SetupFarmScreen(
                        tankId = entry.toRoute<SetupFarm>().tankId,
                        onTankSaved = { id -> navController.navigate(SetupFarm(tankId = id)) { popUpTo<SetupFarm> { inclusive = true } } },
                        onLotSaved = {
                            scope.launch { snackbarHostState.showSnackbar(savedMessages.getValue(SaveFeedback.SAVED)) }
                            navController.navigateClearingStack(Home)
                        },
                        onSkip = { navController.navigateClearingStack(Home) },
                    )
                }

                composable<TankDetail> { entry ->
                    TankDetailScreen(
                        onBack = { navController.popBackStack() },
                        onOpenLot = { navController.navigate(LotDetail(it)) },
                        onNewLot = { navController.navigate(NewLot(it)) },
                        onRegisterWater = { navController.navigate(WaterRecord(it)) },
                        onRegisterFeeding = { navController.navigate(FeedingRecord(it)) },
                        showSavedMessage = entry.toRoute<TankDetail>().saved,
                    )
                }
                composable<LotDetail> { entry ->
                    LotDetailScreen(
                        onBack = { navController.popBackStack() },
                        onRegisterFeeding = { navController.navigate(FeedingRecord(it)) },
                        onRegisterBiometry = { navController.navigate(Biometry(it)) },
                        onRegisterMovement = { navController.navigate(MortalityRecord(it)) },
                        showSavedMessage = entry.toRoute<LotDetail>().saved,
                    )
                }
                composable<NewTank> {
                    TankFormScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { id ->
                            navController.navigate(TankDetail(id, saved = true)) { popUpTo<NewTank> { inclusive = true } }
                        },
                    )
                }
                composable<NewLot> {
                    LotFormScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { id ->
                            navController.navigate(LotDetail(id, saved = true)) { popUpTo<NewLot> { inclusive = true } }
                        },
                    )
                }
                composable<WaterRecord> {
                    WaterRecordScreen(onBack = { navController.popBackStack() }, onDone = finishRecord)
                }
                composable<FeedingRecord> {
                    FeedingRecordScreen(onBack = { navController.popBackStack() }, onDone = finishRecord)
                }
                composable<Biometry> { entry ->
                    BiometryScreen(
                        onBack = { navController.popBackStack() },
                        onDone = finishRecord,
                        // Depois da biometria, o atalho leva direto à ração (com o peso médio novo).
                        onRegisterFeeding = { feedback ->
                            scope.launch { snackbarHostState.showSnackbar(savedMessages.getValue(feedback)) }
                            navController.navigate(FeedingRecord(entry.toRoute<Biometry>().lotId)) { popUpTo<Biometry> { inclusive = true } }
                        },
                    )
                }
                composable<MortalityRecord> {
                    MovementScreen(onBack = { navController.popBackStack() }, onDone = finishRecord)
                }
            }
        }
    }

    // Quando a sessão muda com o app aberto (login, cadastro, logout), limpa a pilha de navegação.
    LaunchedEffect(session) {
        val destination = navController.currentDestination ?: return@LaunchedEffect
        val onAuthScreen = destination.hasRoute<Login>() || destination.hasRoute<SignUp>() ||
            destination.hasRoute<ForgotPassword>()
        when (resolveSessionNavigation(session, onAuthScreen, onboardingCompleted)) {
            SessionNavigation.GO_TO_LOGIN -> navController.navigateClearingStack(Login)
            SessionNavigation.GO_TO_ONBOARDING -> navController.navigateClearingStack(Onboarding)
            SessionNavigation.GO_TO_HOME -> navController.navigateClearingStack(Home)
            SessionNavigation.STAY -> Unit
        }
    }
}

/** Troca de aba: guarda e restaura o estado de cada aba, sem empilhar a mesma tela duas vezes. */
private fun NavController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo<Home> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Navega e apaga toda a pilha: o botão Voltar não retorna às telas anteriores. */
private fun NavController.navigateClearingStack(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
private fun LoadingScreen() {
    val description = stringResource(R.string.loading)
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
        }
    }
}

@Preview(name = "Carregando", showBackground = true, fontScale = 2f)
@Composable
private fun LoadingScreenPreview() {
    AquaGestorTheme { LoadingScreen() }
}
