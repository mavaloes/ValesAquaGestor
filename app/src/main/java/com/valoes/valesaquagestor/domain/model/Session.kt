package com.valoes.valesaquagestor.domain.model

/**
 * Sessão do usuário logado. A tela inicial é decidida a partir dela (`specs/11-autenticacao.md`).
 * Nunca guarda a senha.
 */
data class Session(
    val userId: String,
    val nome: String,
    val email: String,
    val papel: Papel,
)
