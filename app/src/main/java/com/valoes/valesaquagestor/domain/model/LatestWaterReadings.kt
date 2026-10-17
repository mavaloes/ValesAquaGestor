package com.valoes.valesaquagestor.domain.model

import java.time.Instant

/**
 * Última leitura de um parâmetro da água.
 *
 * @property value valor medido, na unidade do parâmetro (ver [QualidadeAgua]).
 * @property dataHora quando foi medido.
 */
data class Reading(val value: Double, val dataHora: Instant)

/**
 * Última leitura de cada parâmetro de um tanque. Cada parâmetro vem da medição mais recente em que
 * ele foi informado (a amônia semanal pode ser mais antiga que o oxigênio de hoje). `null` = nunca medido.
 * A amônia é a **total**; a tóxica é calculada por `AmmoniaCalculator`.
 */
data class LatestWaterReadings(
    val transparenciaCm: Reading? = null,
    val temperaturaC: Reading? = null,
    val oxigenioMgL: Reading? = null,
    val ph: Reading? = null,
    val amoniaTotalMgL: Reading? = null,
    val nitritoMgL: Reading? = null,
    val alcalinidadeMgL: Reading? = null,
    val durezaMgL: Reading? = null,
)
