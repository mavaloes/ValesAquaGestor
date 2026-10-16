package com.valoes.valesaquagestor.presentation.navigation

import com.valoes.valesaquagestor.domain.model.Session

/** Tela em que o app abre. */
enum class StartDestination {
    LOGIN,
    HOME,
}

/**
 * Decide a tela inicial a partir da sessão (`specs/10-telas-e-navegacao.md`): sem sessão, Login;
 * com sessão, Home, **mesmo sem internet**. A Fase 07 acrescenta o caso do primeiro acesso.
 */
fun resolveStartDestination(session: Session?): StartDestination =
    if (session == null) StartDestination.LOGIN else StartDestination.HOME

/** O que fazer com a navegação quando a sessão muda enquanto o app está aberto. */
enum class SessionNavigation {
    /** Nada a fazer. */
    STAY,

    /** A sessão acabou (logout ou expirou): ir para o Login e limpar a pilha. */
    GO_TO_LOGIN,

    /** O usuário entrou ou se cadastrou: ir para a Home e limpar a pilha (Voltar não retorna ao Login). */
    GO_TO_HOME,
}

/**
 * @param session sessão atual.
 * @param onAuthScreen `true` se a tela aberta é Login, Cadastro ou Esqueci a senha.
 */
fun resolveSessionNavigation(session: Session?, onAuthScreen: Boolean): SessionNavigation = when {
    session == null && !onAuthScreen -> SessionNavigation.GO_TO_LOGIN
    session != null && onAuthScreen -> SessionNavigation.GO_TO_HOME
    else -> SessionNavigation.STAY
}
