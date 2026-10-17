package com.valoes.valesaquagestor.domain.model

/**
 * Comportamento dos peixes observado no trato (Planilha 4).
 *
 * [BOQUEJANDO] corresponde a "peixes boquejando na superfície", sinal de falta de oxigênio.
 *
 * @see F01 p.153
 */
enum class ComportamentoPeixe {
    VORAZ,
    NORMAL,
    LENTO,
    BOQUEJANDO,
    SEM_APETITE,
}
