package com.valoes.valesaquagestor.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.navigation.compose.rememberNavController
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.presentation.auth.ForgotPasswordScreen
import com.valoes.valesaquagestor.presentation.auth.LoginScreen
import com.valoes.valesaquagestor.presentation.auth.SignUpScreen
import com.valoes.valesaquagestor.presentation.home.HomeScreen
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/**
 * Raiz do app. Lê a sessão salva (sem internet) e só então monta a navegação, já na tela certa:
 * assim, quem tem sessão nunca vê o Login piscar.
 */
@Composable
fun AquaGestorApp(viewModel: AppViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        AppState.Loading -> LoadingScreen()
        is AppState.Ready -> AppNavigation(session = current.session)
    }
}

@Composable
private fun AppNavigation(session: Session?) {
    val navController = rememberNavController()
    // A tela inicial é decidida uma vez, pela sessão lida ao abrir.
    val startDestination = remember { resolveStartDestination(session) }

    NavHost(
        navController = navController,
        startDestination = when (startDestination) {
            StartDestination.LOGIN -> Login
            StartDestination.HOME -> Home
        },
    ) {
        composable<Login> {
            LoginScreen(
                onForgotPassword = { navController.navigate(ForgotPassword) },
                onSignUp = { navController.navigate(SignUp) },
            )
        }
        composable<SignUp> {
            SignUpScreen(onBackToLogin = { navController.popBackStack() })
        }
        composable<ForgotPassword> {
            ForgotPasswordScreen(onBackToLogin = { navController.popBackStack() })
        }
        composable<Home> {
            HomeScreen()
        }
    }

    // Quando a sessão muda com o app aberto (login, cadastro, logout), limpa a pilha de navegação.
    LaunchedEffect(session) {
        val destination = navController.currentDestination ?: return@LaunchedEffect
        val onAuthScreen = destination.hasRoute<Login>() || destination.hasRoute<SignUp>() ||
            destination.hasRoute<ForgotPassword>()
        when (resolveSessionNavigation(session, onAuthScreen)) {
            SessionNavigation.GO_TO_LOGIN -> navController.navigateClearingStack(Login)
            SessionNavigation.GO_TO_HOME -> navController.navigateClearingStack(Home)
            SessionNavigation.STAY -> Unit
        }
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
