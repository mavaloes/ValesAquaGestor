package com.valoes.valesaquagestor.domain.rules

import java.time.Instant

/** O que gerou o alerta. Alimenta o texto e a unidade na tela. */
enum class AlertKind {
    OXYGEN,
    TEMPERATURE,
    PH,
    TOXIC_AMMONIA,
    NITRITE,
    TRANSPARENCY,
    ALKALINITY,
    HARDNESS,

    /** Variação do pH no dia (alerta composto). */
    PH_VARIATION,

    /** Mortalidade acumulada do ciclo (alerta composto). */
    MORTALITY,
}

/**
 * Avaliação de uma leitura de água.
 *
 * @property value valor avaliado: para [AlertKind.TOXIC_AMMONIA] é a amônia **tóxica** calculada.
 * @property measuredAt quando a leitura foi feita.
 */
data class WaterAlert(
    val kind: AlertKind,
    val value: Double,
    val status: WaterStatus,
    val measuredAt: Instant,
)

/**
 * Todas as leituras mais recentes de um tanque, já avaliadas (inclusive as ADEQUADO).
 *
 * @property alerts uma avaliação por parâmetro medido.
 * @property ammoniaNeedsPhAndTemperature `true` se há amônia total mas falta pH ou temperatura
 *   recentes para calcular a tóxica: a tela mostra "Informe pH e temperatura para calcular a amônia tóxica".
 */
data class WaterReport(
    val alerts: List<WaterAlert>,
    val ammoniaNeedsPhAndTemperature: Boolean,
) {
    /** Pior nível entre as leituras, ou `null` se não há nenhuma. */
    val worstLevel: AlertLevel? get() = alerts.maxOfOrNull { it.status.level }
}

/**
 * Alerta mostrado na Home e nas listas, com o tanque (e o lote) de onde veio.
 *
 * @property value valor que gerou o alerta (`null` quando não se aplica).
 */
data class AlertItem(
    val tankId: Long,
    val tankName: String,
    val lotId: Long?,
    val kind: AlertKind,
    val value: Double?,
    val status: WaterStatus,
    val measuredAt: Instant?,
)
