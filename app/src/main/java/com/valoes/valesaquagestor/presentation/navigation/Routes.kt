package com.valoes.valesaquagestor.presentation.navigation

import kotlinx.serialization.Serializable

/*
 * Rotas type-safe (`specs/10-telas-e-navegacao.md`). Cada fase de telas acrescenta as suas.
 */

/** Login (grupo de autenticação, fora do menu). */
@Serializable
data object Login

/** Cadastro (grupo de autenticação). */
@Serializable
data object SignUp

/** Recuperar senha (grupo de autenticação). */
@Serializable
data object ForgotPassword

/** Home. Provisória e vazia na Fase 04; vira a rotina diária na Fase 05. */
@Serializable
data object Home
