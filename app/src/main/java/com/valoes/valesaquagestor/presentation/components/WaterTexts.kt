package com.valoes.valesaquagestor.presentation.components

import androidx.annotation.StringRes
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.rules.WaterStatus

// ARQUIVO GERADO por gen_water_texts.py junto com res/values/strings_water.xml.
// Converte as chaves do domínio (`WaterStatus.messageKey`/`actionKey`) em textos de `strings.xml`.

/** Texto que diz o que o valor significa. */
@StringRes
fun WaterStatus.messageRes(): Int = when (messageKey) {
        "water_oxygen_adequado" -> R.string.water_oxygen_adequado
        "water_oxygen_atencao" -> R.string.water_oxygen_atencao
        "water_oxygen_critico" -> R.string.water_oxygen_critico
        "water_oxygen_emergencia" -> R.string.water_oxygen_emergencia
        "water_ph_adequado" -> R.string.water_ph_adequado
        "water_ph_low_atencao" -> R.string.water_ph_low_atencao
        "water_ph_low_critico" -> R.string.water_ph_low_critico
        "water_ph_low_emergencia" -> R.string.water_ph_low_emergencia
        "water_ph_high_atencao" -> R.string.water_ph_high_atencao
        "water_ph_high_critico" -> R.string.water_ph_high_critico
        "water_ph_high_emergencia" -> R.string.water_ph_high_emergencia
        "water_temperature_adequado" -> R.string.water_temperature_adequado
        "water_temperature_low_atencao" -> R.string.water_temperature_low_atencao
        "water_temperature_low_critico" -> R.string.water_temperature_low_critico
        "water_temperature_high_critico" -> R.string.water_temperature_high_critico
        "water_ammonia_adequado" -> R.string.water_ammonia_adequado
        "water_ammonia_atencao" -> R.string.water_ammonia_atencao
        "water_ammonia_critico" -> R.string.water_ammonia_critico
        "water_ammonia_emergencia" -> R.string.water_ammonia_emergencia
        "water_nitrite_adequado" -> R.string.water_nitrite_adequado
        "water_nitrite_atencao" -> R.string.water_nitrite_atencao
        "water_nitrite_critico" -> R.string.water_nitrite_critico
        "water_nitrite_emergencia" -> R.string.water_nitrite_emergencia
        "water_transparency_adequado" -> R.string.water_transparency_adequado
        "water_transparency_low_atencao" -> R.string.water_transparency_low_atencao
        "water_transparency_low_critico" -> R.string.water_transparency_low_critico
        "water_transparency_high_atencao" -> R.string.water_transparency_high_atencao
        "water_transparency_high_critico" -> R.string.water_transparency_high_critico
        "water_alkalinity_adequado" -> R.string.water_alkalinity_adequado
        "water_alkalinity_atencao" -> R.string.water_alkalinity_atencao
        "water_alkalinity_critico" -> R.string.water_alkalinity_critico
        "water_hardness_adequado" -> R.string.water_hardness_adequado
        "water_hardness_atencao" -> R.string.water_hardness_atencao
        "water_composite_ph_variation_atencao" -> R.string.water_composite_ph_variation_atencao
        "water_composite_mortality_atencao" -> R.string.water_composite_mortality_atencao
        "water_composite_pond_load_atencao" -> R.string.water_composite_pond_load_atencao
    else -> error("Texto não cadastrado para $messageKey")
}

/** Texto que diz o que fazer. */
@StringRes
fun WaterStatus.actionRes(): Int = when (actionKey) {
        "action_water_oxygen_adequado" -> R.string.action_water_oxygen_adequado
        "action_water_oxygen_atencao" -> R.string.action_water_oxygen_atencao
        "action_water_oxygen_critico" -> R.string.action_water_oxygen_critico
        "action_water_oxygen_emergencia" -> R.string.action_water_oxygen_emergencia
        "action_water_ph_adequado" -> R.string.action_water_ph_adequado
        "action_water_ph_low_atencao" -> R.string.action_water_ph_low_atencao
        "action_water_ph_low_critico" -> R.string.action_water_ph_low_critico
        "action_water_ph_low_emergencia" -> R.string.action_water_ph_low_emergencia
        "action_water_ph_high_atencao" -> R.string.action_water_ph_high_atencao
        "action_water_ph_high_critico" -> R.string.action_water_ph_high_critico
        "action_water_ph_high_emergencia" -> R.string.action_water_ph_high_emergencia
        "action_water_temperature_adequado" -> R.string.action_water_temperature_adequado
        "action_water_temperature_low_atencao" -> R.string.action_water_temperature_low_atencao
        "action_water_temperature_low_critico" -> R.string.action_water_temperature_low_critico
        "action_water_temperature_high_critico" -> R.string.action_water_temperature_high_critico
        "action_water_ammonia_adequado" -> R.string.action_water_ammonia_adequado
        "action_water_ammonia_atencao" -> R.string.action_water_ammonia_atencao
        "action_water_ammonia_critico" -> R.string.action_water_ammonia_critico
        "action_water_ammonia_emergencia" -> R.string.action_water_ammonia_emergencia
        "action_water_nitrite_adequado" -> R.string.action_water_nitrite_adequado
        "action_water_nitrite_atencao" -> R.string.action_water_nitrite_atencao
        "action_water_nitrite_critico" -> R.string.action_water_nitrite_critico
        "action_water_nitrite_emergencia" -> R.string.action_water_nitrite_emergencia
        "action_water_transparency_adequado" -> R.string.action_water_transparency_adequado
        "action_water_transparency_low_atencao" -> R.string.action_water_transparency_low_atencao
        "action_water_transparency_low_critico" -> R.string.action_water_transparency_low_critico
        "action_water_transparency_high_atencao" -> R.string.action_water_transparency_high_atencao
        "action_water_transparency_high_critico" -> R.string.action_water_transparency_high_critico
        "action_water_alkalinity_adequado" -> R.string.action_water_alkalinity_adequado
        "action_water_alkalinity_atencao" -> R.string.action_water_alkalinity_atencao
        "action_water_alkalinity_critico" -> R.string.action_water_alkalinity_critico
        "action_water_hardness_adequado" -> R.string.action_water_hardness_adequado
        "action_water_hardness_atencao" -> R.string.action_water_hardness_atencao
        "action_water_composite_ph_variation_atencao" -> R.string.action_water_composite_ph_variation_atencao
        "action_water_composite_mortality_atencao" -> R.string.action_water_composite_mortality_atencao
        "action_water_composite_pond_load_atencao" -> R.string.action_water_composite_pond_load_atencao
    else -> error("Texto não cadastrado para $actionKey")
}
