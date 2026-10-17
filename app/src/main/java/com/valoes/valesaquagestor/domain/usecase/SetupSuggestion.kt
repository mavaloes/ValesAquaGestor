package com.valoes.valesaquagestor.domain.usecase

/**
 * O que a configuração guiada ainda tem a oferecer. A Home mostra o atalho enquanto a fazenda não
 * está configurada, para o usuário que pulou o primeiro acesso poder retomar.
 */
sealed interface SetupSuggestion {
    /** Fazenda configurada (há tanque e lote ativos). */
    data object None : SetupSuggestion

    /** Nenhum tanque ainda: começar pelo passo 1. */
    data object CreateTank : SetupSuggestion

    /** Há tanque mas nenhum lote ativo em toda a fazenda: continuar pelo passo 2, neste tanque. */
    data class CreateLot(val tankId: Long, val tankName: String) : SetupSuggestion
}

/** Decide o que sugerir a partir do retrato da fazenda. */
fun resolveSetupSuggestion(farm: FarmOverview): SetupSuggestion = when {
    farm.tanks.isEmpty() -> SetupSuggestion.CreateTank
    farm.tanks.none { it.lots.isNotEmpty() } -> {
        // O primeiro tanque por nome: o que o usuário provavelmente acabou de criar.
        val first = farm.tanks.minBy { it.tanque.nome }
        SetupSuggestion.CreateLot(first.tanque.id, first.tanque.nome)
    }

    else -> SetupSuggestion.None
}
