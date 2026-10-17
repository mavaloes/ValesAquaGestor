package com.valoes.valesaquagestor.data.sensor

import com.valoes.valesaquagestor.domain.sensor.SensorReading
import com.valoes.valesaquagestor.domain.sensor.WaterSensorSource
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Primeira implementação de [WaterSensorSource]: a leitura é **digitada** (entregue por quem a tem em
 * mãos, via [submit]) e fica guardada na memória até o formulário de água do tanque pegá-la. Sem
 * nenhuma leitura entregue, não há sensor e o formulário funciona como sempre.
 *
 * Nenhum modelo de sensor real nesta fase; Bluetooth e Wi-Fi entram depois como outras implementações.
 */
@Singleton
class ManualWaterSensorSource @Inject constructor() : WaterSensorSource {

    private val pending = ConcurrentHashMap<Long, SensorReading>()

    /** Deixa [reading] esperando o formulário de água do tanque [tankId]; uma leitura nova substitui a antiga. */
    fun submit(tankId: Long, reading: SensorReading) {
        if (reading.hasAnyValue) pending[tankId] = reading
    }

    override suspend fun takePendingReading(tankId: Long): SensorReading? = pending.remove(tankId)
}
