package com.valoes.valesaquagestor.presentation.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.calc.MeasuredParameter
import com.valoes.valesaquagestor.domain.rules.AlertKind
import com.valoes.valesaquagestor.presentation.util.formatNumber

/** Nome do que gerou o alerta (ex.: "Oxigênio"). */
@StringRes
fun AlertKind.labelRes(): Int = when (this) {
    AlertKind.OXYGEN -> R.string.kind_oxygen
    AlertKind.TEMPERATURE -> R.string.kind_temperature
    AlertKind.PH -> R.string.kind_ph
    AlertKind.TOXIC_AMMONIA -> R.string.kind_toxic_ammonia
    AlertKind.NITRITE -> R.string.kind_nitrite
    AlertKind.TRANSPARENCY -> R.string.kind_transparency
    AlertKind.ALKALINITY -> R.string.kind_alkalinity
    AlertKind.HARDNESS -> R.string.kind_hardness
    AlertKind.PH_VARIATION -> R.string.kind_ph_variation
    AlertKind.MORTALITY -> R.string.kind_mortality
}

/** Unidade do valor; `null` para o pH, que não tem unidade. */
@StringRes
private fun AlertKind.unitRes(): Int? = when (this) {
    AlertKind.OXYGEN, AlertKind.TOXIC_AMMONIA, AlertKind.NITRITE, AlertKind.ALKALINITY, AlertKind.HARDNESS -> R.string.unit_mg_l
    AlertKind.TEMPERATURE -> R.string.unit_celsius
    AlertKind.TRANSPARENCY -> R.string.unit_cm
    AlertKind.MORTALITY -> R.string.unit_percent
    AlertKind.PH, AlertKind.PH_VARIATION -> null
}

/** Casas decimais de cada valor: a amônia tóxica é pequena e precisa de mais casas. */
private fun AlertKind.decimals(): Int = when (this) {
    AlertKind.TOXIC_AMMONIA -> 3
    AlertKind.NITRITE -> 2
    AlertKind.OXYGEN, AlertKind.TEMPERATURE, AlertKind.PH, AlertKind.PH_VARIATION, AlertKind.MORTALITY -> 1
    AlertKind.TRANSPARENCY, AlertKind.ALKALINITY, AlertKind.HARDNESS -> 0
}

/** Valor com unidade, no padrão brasileiro (ex.: "3,5 mg/L"). */
@Composable
fun alertValueText(kind: AlertKind, value: Double): String {
    val number = formatNumber(value, kind.decimals())
    val unit = kind.unitRes() ?: return number
    return stringResource(R.string.value_with_unit, number, stringResource(unit))
}

/** Nome do parâmetro da agenda (ex.: "Amônia total"), para as tarefas da Home. */
@StringRes
fun MeasuredParameter.labelRes(): Int = when (this) {
    MeasuredParameter.OXYGEN -> R.string.kind_oxygen
    MeasuredParameter.TEMPERATURE -> R.string.kind_temperature
    MeasuredParameter.TRANSPARENCY -> R.string.kind_transparency
    MeasuredParameter.PH -> R.string.kind_ph
    MeasuredParameter.TOTAL_AMMONIA -> R.string.kind_total_ammonia
    MeasuredParameter.NITRITE -> R.string.kind_nitrite
    MeasuredParameter.ALKALINITY -> R.string.kind_alkalinity
    MeasuredParameter.HARDNESS -> R.string.kind_hardness
}
