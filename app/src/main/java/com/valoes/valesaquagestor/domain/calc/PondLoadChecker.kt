package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.rules.AlertLevel
import com.valoes.valesaquagestor.domain.rules.CompositeThresholds
import com.valoes.valesaquagestor.domain.rules.WaterStatus

/**
 * Carga de ração do viveiro.
 *
 * @property kgPerHaDay ração diária por hectare, em kg/ha/dia.
 * @property status ATENCAO se acima de 40 kg/ha/dia; `null` caso contrário.
 * @property requiresAmmoniaCheck `true` se acima de 60 kg/ha/dia: medir amônia total, pH e
 *   temperatura às 17 h, a cada 5 dias.
 */
data class PondLoad(
    val kgPerHaDay: Double,
    val status: WaterStatus?,
    val requiresAmmoniaCheck: Boolean,
)

/** Verifica a carga de ração por hectare (`specs/03-regras-negocio.md`, seção 3, regra 3). */
object PondLoadChecker {

    private const val M2_PER_HA = 10_000.0

    /**
     * `carga = racaoDiariaKg / (areaM2 / 10.000)`, calculada como `kg × 10.000 / área` para o
     * limite de 40 kg/ha/dia dar exatamente 40,0 (4 kg em 1.000 m² não alerta).
     *
     * @see F11 p.15
     * @see F13 p.38
     */
    fun check(dailyFeedKg: Double, areaM2: Double): PondLoad {
        val load = dailyFeedKg * M2_PER_HA / areaM2
        val status = if (load > CompositeThresholds.FEED_LOAD_ATTENTION_ABOVE_KG_HA_DAY) {
            WaterStatus(AlertLevel.ATENCAO, "water_composite_pond_load_atencao", "action_water_composite_pond_load_atencao")
        } else {
            null
        }
        return PondLoad(load, status, load > CompositeThresholds.FEED_LOAD_AMMONIA_CHECK_ABOVE_KG_HA_DAY)
    }
}
