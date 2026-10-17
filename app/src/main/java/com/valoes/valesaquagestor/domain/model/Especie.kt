package com.valoes.valesaquagestor.domain.model

/**
 * Espécie de peixe do lote.
 *
 * No MVP, só [TILAPIA] tem a tabela de ração completa (F14 p.34). [TAMBAQUI] e [PIRARUCU] já têm
 * tabela nas fontes (F01 p.78-79) para uma fase seguinte. Em [OUTRA], o usuário informa o % do peso vivo.
 * As faixas de água de tilápia valem como "faixa de referência geral" para as outras espécies.
 */
enum class Especie {
    TILAPIA,
    TAMBAQUI,
    PIRARUCU,
    OUTRA,
}
