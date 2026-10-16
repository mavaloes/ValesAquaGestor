package com.valoes.valesaquagestor.domain.sensor

import java.time.Instant

/**
 * Leitura de um sensor de água. Todo campo é opcional, porque cada sensor mede só alguns parâmetros.
 * A amônia é a **total**; a tóxica continua sendo calculada com pH e temperatura.
 *
 * @property readAt momento em que o sensor mediu.
 */
data class SensorReading(
    val readAt: Instant,
    val transparenciaCm: Double? = null,
    val temperaturaC: Double? = null,
    val oxigenioMgL: Double? = null,
    val ph: Double? = null,
    val amoniaTotalMgL: Double? = null,
    val nitritoMgL: Double? = null,
    val alcalinidadeMgL: Double? = null,
    val durezaMgL: Double? = null,
) {
    /** `true` se o sensor trouxe pelo menos uma medida. */
    val hasAnyValue: Boolean
        get() = listOf(
            transparenciaCm, temperaturaC, oxigenioMgL, ph,
            amoniaTotalMgL, nitritoMgL, alcalinidadeMgL, durezaMgL,
        ).any { it != null }
}

/**
 * De onde vêm as leituras de sensor (`specs/07-automacoes.md`). O protocolo de um sensor real
 * (Bluetooth ou Wi-Fi) ainda não foi escolhido (🔶): quando for, ele entra como outra implementação
 * desta interface, sem mudar o formulário de água.
 *
 * A leitura é só um **rascunho**: o formulário a mostra editável e marcada como "vinda do sensor",
 * e quem salva o registro é sempre o usuário.
 */
interface WaterSensorSource {

    /**
     * Entrega a leitura que está esperando para o tanque [tankId] e a tira da fila (cada leitura
     * preenche o formulário uma vez). `null` quando não há sensor ou nada foi lido.
     */
    suspend fun takePendingReading(tankId: Long): SensorReading?
}
