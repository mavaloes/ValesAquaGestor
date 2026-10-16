package com.valoes.valesaquagestor.domain.model

import java.time.Instant

/**
 * Biometria: pesagem de uma amostra de peixes do lote.
 *
 * @property id chave local; 0 para uma biometria ainda não salva.
 * @property uuid identidade na sincronização; vazio até o primeiro salvamento.
 * @property pesoAmostraKg peso total dos peixes da amostra, em kg, já sem a tara.
 * @property numeroPeixesAmostra quantidade de peixes pesados.
 * @property criadoPorUserId `userId` de quem criou o registro; `null` nos registros anteriores à Fase 08.
 */
data class Biometria(
    val id: Long = 0,
    val uuid: String = "",
    val loteId: Long,
    val dataHora: Instant,
    val pesoAmostraKg: Double,
    val numeroPeixesAmostra: Int,
    val observacaoSaude: String? = null,
    val criadoPorUserId: String? = null,
) {
    /** Peso médio da amostra, em g: `pesoAmostraKg / numeroPeixesAmostra × 1000`. */
    val pesoMedioG: Double
        get() = pesoAmostraKg / numeroPeixesAmostra * 1000.0
}
