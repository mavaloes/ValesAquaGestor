package com.valoes.valesaquagestor.presentation.util

import androidx.annotation.StringRes
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.rules.Plausibility

/** Erro de um campo de formulário. A tela converte em texto com [stringRes]. */
enum class FormError {
    REQUIRED,
    INVALID_NUMBER,
    NOT_POSITIVE,
    OUT_OF_RANGE_PERCENT,

    /** Valor impossível: número negativo. */
    NEGATIVE,

    /** Valor impossível: o pH só existe de 0 a 14. */
    PH_OUT_OF_SCALE,

    /** Valor impossível: mais peixes retirados do que existem vivos no lote. */
    EXCEEDS_ALIVE,

    /** Valor impossível: o peso com os peixes não é maior que a tara. */
    GROSS_BELOW_TARE,
}

@StringRes
fun FormError.stringRes(): Int = when (this) {
    FormError.REQUIRED -> R.string.error_required
    FormError.INVALID_NUMBER -> R.string.error_invalid_number
    FormError.NOT_POSITIVE -> R.string.error_not_positive
    FormError.OUT_OF_RANGE_PERCENT -> R.string.error_percent_range
    FormError.NEGATIVE -> R.string.error_negative
    FormError.PH_OUT_OF_SCALE -> R.string.error_ph_scale
    FormError.EXCEEDS_ALIVE -> R.string.error_exceeds_alive
    FormError.GROSS_BELOW_TARE -> R.string.error_gross_below_tare
}

/** Validação dos campos de formulário, em Kotlin puro (o ViewModel chama, a tela só mostra o erro). */
object FormValidation {

    /** Texto obrigatório (ex.: nome). */
    fun requiredText(text: String): FormError? = if (text.isBlank()) FormError.REQUIRED else null

    /**
     * Número decimal maior que zero. Vazio só é aceito se o campo não for [required].
     */
    fun positiveDecimal(text: String, required: Boolean): FormError? {
        if (text.isBlank()) return if (required) FormError.REQUIRED else null
        val value = parseDecimal(text) ?: return FormError.INVALID_NUMBER
        return if (value > 0.0) null else FormError.NOT_POSITIVE
    }

    /**
     * Número decimal igual ou maior que zero (uma medida pode ser zero). Negativo é impossível e
     * bloqueado. Vazio só é aceito se o campo não for [required].
     */
    fun nonNegativeDecimal(text: String, required: Boolean = false): FormError? {
        if (text.isBlank()) return if (required) FormError.REQUIRED else null
        val value = parseDecimal(text) ?: return FormError.INVALID_NUMBER
        return if (value >= 0.0) null else FormError.NEGATIVE
    }

    /** pH opcional: número na escala de 0 a 14. Fora dela é impossível e bloqueado. */
    fun ph(text: String): FormError? {
        if (text.isBlank()) return null
        val value = parseDecimal(text) ?: return FormError.INVALID_NUMBER
        return if (Plausibility.isPhOnScale(value)) null else FormError.PH_OUT_OF_SCALE
    }

    /** Número inteiro maior que zero (ex.: quantidade de peixes). */
    fun positiveWholeNumber(text: String, required: Boolean): FormError? {
        if (text.isBlank()) return if (required) FormError.REQUIRED else null
        val value = parseWholeNumber(text) ?: return FormError.INVALID_NUMBER
        return if (value > 0) null else FormError.NOT_POSITIVE
    }

    /** Número inteiro maior que zero e no máximo [max] (ex.: não retirar mais peixes do que há vivos). */
    fun wholeNumberAtMost(text: String, max: Int): FormError? {
        positiveWholeNumber(text, required = true)?.let { return it }
        return if (checkNotNull(parseWholeNumber(text)) <= max) null else FormError.EXCEEDS_ALIVE
    }

    /** Percentual de 0 a 100, inclusive (ex.: renovação de água). Sempre obrigatório. */
    fun percent(text: String): FormError? {
        if (text.isBlank()) return FormError.REQUIRED
        val value = parseDecimal(text) ?: return FormError.INVALID_NUMBER
        return if (value in 0.0..PERCENT_MAX) null else FormError.OUT_OF_RANGE_PERCENT
    }

    private const val PERCENT_MAX = 100.0
}
