package com.valoes.valesaquagestor.domain.rules

/**
 * Nível de alerta de um valor medido. A ordem das constantes vai do melhor ao pior.
 *
 * @see specs/03-regras-negocio.md, seção 1
 */
enum class AlertLevel {
    /** Dentro da faixa ideal. Nada a fazer. */
    ADEQUADO,

    /** Saiu do ideal. Reduzir a ração e medir de novo. */
    ATENCAO,

    /** Passou do limite de alimentação. Não alimentar e renovar a água. */
    CRITICO,

    /** Risco de mortalidade. Aeração de emergência e chamar o técnico. */
    EMERGENCIA,
    ;

    /** `true` se este nível é igual ou pior que [other]. */
    fun isAtLeast(other: AlertLevel): Boolean = this >= other
}
