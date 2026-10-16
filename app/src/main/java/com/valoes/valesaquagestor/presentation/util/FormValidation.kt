package com.valoes.valesaquagestor.presentation.util

import androidx.annotation.StringRes
import com.valoes.valesaquagestor.R

/** Erro de um campo de formulário. A tela converte em texto com [stringRes]. */
enum class FormError {
    REQUIRED,
    INVALID_NUMBER,
    NOT_POSITIVE,
    OUT_OF_RANGE_PERCENT,
}

@StringRes
fun FormError.stringRes(): Int = when (this) {
    FormError.REQUIRED -> R.string.error_required
    FormError.INVALID_NUMBER -> R.string.error_invalid_number
    FormError.NOT_POSITIVE -> R.string.error_not_positive
    FormError.OUT_OF_RANGE_PERCENT -> R.string.error_percent_range
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

    /** Número inteiro maior que zero (ex.: quantidade de peixes). */
    fun positiveWholeNumber(text: String, required: Boolean): FormError? {
        if (text.isBlank()) return if (required) FormError.REQUIRED else null
        val value = parseWholeNumber(text) ?: return FormError.INVALID_NUMBER
        return if (value > 0) null else FormError.NOT_POSITIVE
    }

    /** Percentual de 0 a 100, inclusive (ex.: renovação de água). Sempre obrigatório. */
    fun percent(text: String): FormError? {
        if (text.isBlank()) return FormError.REQUIRED
        val value = parseDecimal(text) ?: return FormError.INVALID_NUMBER
        return if (value in 0.0..PERCENT_MAX) null else FormError.OUT_OF_RANGE_PERCENT
    }

    private const val PERCENT_MAX = 100.0
}
