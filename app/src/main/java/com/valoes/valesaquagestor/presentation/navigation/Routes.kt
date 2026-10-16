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

/** Aba "Início": a rotina diária. */
@Serializable
data object Home

/** Aba "Tanques". */
@Serializable
data object Tanques

/** Aba "Registrar": escolher onde registrar. */
@Serializable
data object Registrar

/** Aba "Mais". */
@Serializable
data object Mais

/**
 * Detalhe do tanque. [saved] vale `true` logo depois de criar o tanque, para mostrar "Registro salvo".
 */
@Serializable
data class TankDetail(val id: Long, val saved: Boolean = false)

/** Detalhe do lote. [saved] vale `true` logo depois de criar o lote, para mostrar "Registro salvo". */
@Serializable
data class LotDetail(val id: Long, val saved: Boolean = false)

/** Formulário de novo tanque. */
@Serializable
data object NewTank

/** Formulário de novo lote no tanque [tankId]. */
@Serializable
data class NewLot(val tankId: Long)

/** Registrar água no tanque [tankId]. */
@Serializable
data class WaterRecord(val tankId: Long)

/** Registrar ração no lote [lotId]. */
@Serializable
data class FeedingRecord(val lotId: Long)

/** Biometria do lote [lotId]. */
@Serializable
data class Biometry(val lotId: Long)

/** Mortalidade e despesca do lote [lotId]. */
@Serializable
data class MortalityRecord(val lotId: Long)
