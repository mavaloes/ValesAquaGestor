package com.valoes.valesaquagestor.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Guarda no aparelho se o primeiro acesso guiado já foi concluído (ou pulado). Depois disso o app
 * não mostra o tutorial de novo (`specs/10-telas-e-navegacao.md`). Funciona sem internet.
 */
interface OnboardingRepository {

    /** `true` depois que o usuário concluiu ou pulou o tutorial. Começa `false`. */
    val isCompleted: Flow<Boolean>

    /** Marca o primeiro acesso como concluído. Pular também conta como concluído. */
    suspend fun markCompleted()
}
