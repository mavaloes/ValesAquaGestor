package com.valoes.valesaquagestor.domain.model

/**
 * Fase do cultivo. Na fazenda real os nomes são berçário, recria e terminação (F15 p.14).
 *
 * A fase muda a capacidade do viveiro (F03 p.15) e a frequência de biometria (F01 p.77),
 * mas **não** as faixas de água (`specs/08-fontes-e-conflitos.md`, conflito 12).
 *
 * @see F03 p.16
 */
enum class FaseLote {
    ALEVINAGEM,
    RECRIA,
    ENGORDA,
}
