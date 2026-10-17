package com.valoes.valesaquagestor.domain.repository

/** Diz se o aparelho tem internet agora. Usado para mostrar "Sem conexão" em vez de erro técnico. */
interface ConnectivityChecker {
    fun isOnline(): Boolean
}
