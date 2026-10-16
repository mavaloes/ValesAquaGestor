package com.valoes.valesaquagestor.domain.model

/** Situação do lote. O registro de uma despesca final encerra o lote. */
enum class StatusLote {
    /** Peixes ainda no tanque. */
    ATIVO,

    /** Lote retirado por completo (despesca final). */
    ENCERRADO,
}
