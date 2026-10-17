package com.valoes.valesaquagestor.domain.model

/**
 * Tipo de estrutura de cultivo.
 *
 * Os manuais descrevem viveiro escavado, açude e tanque-rede; alvenaria e redondo vêm do prompt
 * original e foram mantidos (decisão 🔶, `specs/08-fontes-e-conflitos.md`, conflito 11).
 *
 * O tipo decide a base da capacidade: área para [ESCAVADO] e [ACUDE] (F03 p.15) e volume para os
 * demais (F12 p.14).
 *
 * @see F12 p.10-14
 */
enum class TipoTanque {
    /** Viveiro de terra escavado, o mais comum no Brasil. @see F12 p.10 */
    ESCAVADO,

    /** Represa feita com aterro; o formato depende do terreno. @see F12 p.11 */
    ACUDE,

    /** Gaiola flutuante em represa ou lago. @see F12 p.14 */
    TANQUE_REDE,

    /** Tanque de alvenaria. */
    ALVENARIA,

    /** Tanque redondo. */
    REDONDO,
}
