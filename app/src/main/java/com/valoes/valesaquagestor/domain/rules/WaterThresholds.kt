package com.valoes.valesaquagestor.domain.rules

/*
 * Faixas de água para tilápia em viveiro escavado (specs/03-regras-negocio.md, seção 2).
 * Toda constante cita a fonte. Se mudar um valor, mude primeiro o spec 08.
 * Limites: inclusivos no mínimo da faixa; nos limites superiores de pH e temperatura, o valor exato
 * fica na faixa melhor.
 */

/** Oxigênio dissolvido, em mg/L. */
object OxygenThresholds {
    /** @see F01 p.59 — adequado a partir de 4,0 (limite inclusivo). */
    const val ADEQUATE_MIN_MG_L = 4.0

    /** @see F13 p.27 — abaixo de 3 não alimentar (3,0 ainda é ATENCAO). */
    const val CRITICAL_BELOW_MG_L = 3.0

    /** @see F13 p.27 — abaixo de 2 há risco de mortalidade. */
    const val EMERGENCY_BELOW_MG_L = 2.0
}

/** pH. */
object PhThresholds {
    /** @see F11 p.20 — início da faixa adequada (🔶 `08`, conflito 2). */
    const val ADEQUATE_MIN = 6.5

    /** @see F15 p.10 — fim da faixa adequada, inclusivo (🔶 `08`, conflito 2). */
    const val ADEQUATE_MAX = 8.5

    /** @see F11 p.20 — abaixo disto é CRITICO. */
    const val ATTENTION_MIN = 6.0

    /** @see F13 p.37 — acima disto é CRITICO; a fração tóxica da amônia passa de 34% (🔶 `08`, conflito 2). */
    const val ATTENTION_MAX = 9.0

    /** @see F07 p.7 — abaixo disto é EMERGENCIA. */
    const val CRITICAL_MIN = 4.0

    /** @see F07 p.7 — acima disto é EMERGENCIA. */
    const val CRITICAL_MAX = 11.0
}

/** Temperatura da água, em °C. */
object TemperatureThresholds {
    /** @see F01 p.70 — adequado a partir de 26 °C. */
    const val ADEQUATE_MIN_C = 26.0

    /** @see F14 p.38 — adequado até 32 °C, inclusivo; acima disto não fornecer ração. */
    const val ADEQUATE_MAX_C = 32.0

    /** @see F14 p.38 — abaixo de 16 °C não fornecer ração. */
    const val CRITICAL_BELOW_C = 16.0
}

/** Amônia **tóxica** (NH₃), em mg/L. Nunca compare a amônia total com estes limites. */
object AmmoniaThresholds {
    /** @see F11 p.26 — adequado abaixo de 0,05. */
    const val ADEQUATE_BELOW_MG_L = 0.05

    /** @see F13 p.38 — limite para alimentar; a partir daqui é CRITICO. */
    const val CRITICAL_FROM_MG_L = 0.2

    /** @see F11 p.26 — risco de morte a partir de 0,5 (🔶 `08`, conflito 4: a F13 p.38 diz 1,0). */
    const val EMERGENCY_FROM_MG_L = 0.5
}

/** Nitrito (NO₂), em mg/L. */
object NitriteThresholds {
    /** @see F01 p.59 — adequado abaixo de 0,03. */
    const val ADEQUATE_BELOW_MG_L = 0.03

    /** @see F13 p.41 — limite para alimentar; a partir daqui é CRITICO. */
    const val CRITICAL_FROM_MG_L = 0.3

    /** @see F13 p.41 — limite de mortalidade. */
    const val EMERGENCY_FROM_MG_L = 0.7
}

/** Transparência no disco de Secchi, em cm. */
object TransparencyThresholds {
    /** @see F07 p.6 — início da faixa adequada (🔶 `08`, conflito 6). */
    const val ADEQUATE_MIN_CM = 30.0

    /** @see F07 p.6 — fim da faixa adequada, inclusivo (🔶 `08`, conflito 6). */
    const val ADEQUATE_MAX_CM = 45.0

    /** @see F01 p.59 — abaixo disto é CRITICO. */
    const val CRITICAL_BELOW_CM = 20.0

    /** @see F10 p.2 — acima disto é CRITICO. */
    const val CRITICAL_ABOVE_CM = 60.0
}

/** Alcalinidade, em mg/L de CaCO₃. */
object AlkalinityThresholds {
    /** @see F01 p.59 — adequado a partir de 20. */
    const val ADEQUATE_MIN_MG_L = 20.0

    /** @see F13 p.35 — abaixo disto é CRITICO. */
    const val CRITICAL_BELOW_MG_L = 10.0
}

/** Dureza, em mg/L de CaCO₃. */
object HardnessThresholds {
    /** @see F10 p.2 — adequado a partir de 20. */
    const val ADEQUATE_MIN_MG_L = 20.0
}

/** Limites dos alertas compostos (`specs/03-regras-negocio.md`, seção 3). */
object CompositeThresholds {
    /** @see F01 p.66 — variação do pH no dia acima de 2 unidades é ATENCAO. */
    const val PH_DAILY_VARIATION_ABOVE = 2.0

    /** @see F11 p.15 — carga de ração acima de 40 kg/ha/dia é ATENCAO. */
    const val FEED_LOAD_ATTENTION_ABOVE_KG_HA_DAY = 40.0

    /** @see F13 p.38 — acima de 60 kg/ha/dia exige medir a amônia. */
    const val FEED_LOAD_AMMONIA_CHECK_ABOVE_KG_HA_DAY = 60.0

    /** @see F13 p.38 — com carga alta, medir a amônia a cada 5 dias. */
    const val AMMONIA_CHECK_INTERVAL_DAYS = 5

    /** @see F13 p.38 — com carga alta, medir a amônia às 17 h. */
    const val AMMONIA_CHECK_HOUR = 17

    /** @see F02 p.69 — mortalidade acumulada do ciclo acima de 10% é ATENCAO. */
    const val MORTALITY_ATTENTION_ABOVE_PCT = 10.0
}
