package com.valoes.valesaquagestor.domain.rules

import com.valoes.valesaquagestor.domain.calc.AmmoniaCalculator
import com.valoes.valesaquagestor.domain.model.LatestWaterReadings
import com.valoes.valesaquagestor.domain.model.Reading
import java.time.Duration

/**
 * Avalia as últimas leituras de um tanque, parâmetro por parâmetro, com o [WaterEvaluator].
 *
 * A amônia guardada é a **total**: a tóxica é calculada com o pH e a temperatura. Se a medição de
 * amônia não veio com pH ou temperatura, usa a última leitura do mesmo tanque nas 24 h em volta
 * dela; sem isso, não calcula e avisa (`ammoniaNeedsPhAndTemperature`).
 *
 * @see specs/03-regras-negocio.md, seção 4.1
 */
object WaterReadingsEvaluator {

    /** @see specs/03-regras-negocio.md, seção 4.1 — pH e temperatura valem se medidos até 24 h da amônia. */
    const val PH_TEMPERATURE_MAX_AGE_HOURS = 24L

    fun evaluate(readings: LatestWaterReadings): WaterReport {
        val ammonia = readings.amoniaTotalMgL
        val toxic = ammonia?.let { toxicAlert(it, readings.ph, readings.temperaturaC) }

        val alerts = listOfNotNull(
            readings.oxigenioMgL?.let { alert(AlertKind.OXYGEN, it, WaterEvaluator.evaluateOxygen(it.value)) },
            readings.temperaturaC?.let { alert(AlertKind.TEMPERATURE, it, WaterEvaluator.evaluateTemperature(it.value)) },
            readings.ph?.let { alert(AlertKind.PH, it, WaterEvaluator.evaluatePh(it.value)) },
            toxic,
            readings.nitritoMgL?.let { alert(AlertKind.NITRITE, it, WaterEvaluator.evaluateNitrite(it.value)) },
            readings.transparenciaCm?.let { alert(AlertKind.TRANSPARENCY, it, WaterEvaluator.evaluateTransparency(it.value)) },
            readings.alcalinidadeMgL?.let { alert(AlertKind.ALKALINITY, it, WaterEvaluator.evaluateAlkalinity(it.value)) },
            readings.durezaMgL?.let { alert(AlertKind.HARDNESS, it, WaterEvaluator.evaluateHardness(it.value)) },
        )
        return WaterReport(alerts, ammoniaNeedsPhAndTemperature = ammonia != null && toxic == null)
    }

    private fun toxicAlert(total: Reading, ph: Reading?, temperature: Reading?): WaterAlert? {
        val validPh = ph?.takeIf { isRecentEnough(it, total) } ?: return null
        val validTemperature = temperature?.takeIf { isRecentEnough(it, total) } ?: return null
        val toxic = AmmoniaCalculator.toxicMgL(total.value, validPh.value, validTemperature.value)
        return WaterAlert(AlertKind.TOXIC_AMMONIA, toxic, WaterEvaluator.evaluateToxicAmmonia(toxic), total.dataHora)
    }

    private fun isRecentEnough(reading: Reading, ammonia: Reading): Boolean =
        Duration.between(reading.dataHora, ammonia.dataHora).abs() <= Duration.ofHours(PH_TEMPERATURE_MAX_AGE_HOURS)

    private fun alert(kind: AlertKind, reading: Reading, status: WaterStatus) =
        WaterAlert(kind, reading.value, status, reading.dataHora)
}
