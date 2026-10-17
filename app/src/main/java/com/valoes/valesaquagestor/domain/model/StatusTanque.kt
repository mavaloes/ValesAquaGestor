package com.valoes.valesaquagestor.domain.model

/** Situação do tanque na fazenda. Tanque inativo não aparece na rotina diária. */
enum class StatusTanque {
    /** Em uso. */
    ATIVO,

    /** Fora de uso (vazio, em manutenção ou desativado). */
    INATIVO,
}
