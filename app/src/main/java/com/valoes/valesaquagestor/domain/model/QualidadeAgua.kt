package com.valoes.valesaquagestor.domain.model

import java.time.Instant

/**
 * Medição da qualidade da água de um tanque. Todas as medidas são opcionais, mas o repositório
 * exige **pelo menos uma**.
 *
 * Guarda a amônia **total**; a tóxica é calculada por `AmmoniaCalculator` com pH e temperatura.
 * Unidades: mg/L para oxigênio, amônia, nitrito, alcalinidade e dureza.
 *
 * @property id chave local; 0 para uma medição ainda não salva.
 * @property uuid identidade na sincronização; vazio até o primeiro salvamento.
 * @property transparenciaCm transparência no disco de Secchi, em cm.
 * @property temperaturaC temperatura da água, em °C.
 * @property criadoPorUserId `userId` de quem criou o registro; `null` nos registros anteriores à Fase 08.
 */
data class QualidadeAgua(
    val id: Long = 0,
    val uuid: String = "",
    val tanqueId: Long,
    val dataHora: Instant,
    val momento: MomentoMedicao,
    val transparenciaCm: Double? = null,
    val temperaturaC: Double? = null,
    val oxigenioMgL: Double? = null,
    val ph: Double? = null,
    val amoniaTotalMgL: Double? = null,
    val nitritoMgL: Double? = null,
    val alcalinidadeMgL: Double? = null,
    val durezaMgL: Double? = null,
    val diaNublado: Boolean = false,
    val criadoPorUserId: String? = null,
) {
    /** `true` se pelo menos uma medida foi informada. */
    val temAlgumaMedida: Boolean
        get() = listOf(
            transparenciaCm, temperaturaC, oxigenioMgL, ph,
            amoniaTotalMgL, nitritoMgL, alcalinidadeMgL, durezaMgL,
        ).any { it != null }
}
