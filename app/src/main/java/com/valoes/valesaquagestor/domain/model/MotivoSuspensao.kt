package com.valoes.valesaquagestor.domain.model

/**
 * Motivo para suspender o trato. A Planilha 4 registra "ração suspensa"; o app guarda o motivo
 * como dado, não como texto livre.
 *
 * @see F01 p.153
 */
enum class MotivoSuspensao {
    OXIGENIO_BAIXO,
    AMONIA_ALTA,
    NITRITO_ALTO,
    TEMPERATURA,
    SOBRA_RACAO,
    DOENCA,
    OUTRO,
}
