package com.valoes.valesaquagestor.presentation.navigation

import com.valoes.valesaquagestor.domain.model.Session

/** Tela em que o app abre. */
enum class StartDestination {
    LOGIN,
    ONBOARDING,
    HOME,
}

/**
 * Decide a tela inicial (`specs/10-telas-e-navegacao.md`):
 * - sem sessão: Login;
 * - com sessão e primeiro acesso **não** concluído: Onboarding (que pode ser pulado);
 * - com sessão e primeiro acesso concluído: Home, **mesmo sem internet**.
 */
fun resolveStartDestination(session: Session?, onboardingCompleted: Boolean): StartDestination = when {
    session == null -> StartDestination.LOGIN
    !onboardingCompleted -> StartDestination.ONBOARDING
    else -> StartDestination.HOME
}

/** O que fazer com a navegação quando a sessão muda enquanto o app está aberto. */
enum class SessionNavigation {
    /** Nada a fazer. */
    STAY,

    /** A sessão acabou (logout ou expirou): ir para o Login e limpar a pilha. */
    GO_TO_LOGIN,

    /** O usuário entrou ou se cadastrou e ainda não fez o primeiro acesso: ir para o Onboarding e limpar a pilha. */
    GO_TO_ONBOARDING,

    /** O usuário entrou e o primeiro acesso já foi feito: ir para a Home e limpar a pilha (Voltar não retorna ao Login). */
    GO_TO_HOME,
}

/**
 * @param session sessão atual.
 * @param onAuthScreen `true` se a tela aberta é Login, Cadastro ou Esqueci a senha.
 * @param onboardingCompleted `true` se o primeiro acesso guiado já foi concluído ou pulado.
 */
fun resolveSessionNavigation(
    session: Session?,
    onAuthScreen: Boolean,
    onboardingCompleted: Boolean,
): SessionNavigation = when {
    session == null && !onAuthScreen -> SessionNavigation.GO_TO_LOGIN
    session != null && onAuthScreen -> if (onboardingCompleted) SessionNavigation.GO_TO_HOME else SessionNavigation.GO_TO_ONBOARDING
    else -> SessionNavigation.STAY
}
