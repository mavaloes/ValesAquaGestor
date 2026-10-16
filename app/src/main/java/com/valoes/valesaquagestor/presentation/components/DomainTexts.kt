package com.valoes.valesaquagestor.presentation.components

import androidx.annotation.StringRes
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.model.TipoTanque

/** Textos dos enums do domínio. O domínio não guarda texto para o usuário. */

@StringRes
fun TipoTanque.labelRes(): Int = when (this) {
    TipoTanque.ESCAVADO -> R.string.tank_type_escavado
    TipoTanque.ACUDE -> R.string.tank_type_acude
    TipoTanque.TANQUE_REDE -> R.string.tank_type_tanque_rede
    TipoTanque.ALVENARIA -> R.string.tank_type_alvenaria
    TipoTanque.REDONDO -> R.string.tank_type_redondo
}

@StringRes
fun Especie.labelRes(): Int = when (this) {
    Especie.TILAPIA -> R.string.species_tilapia
    Especie.TAMBAQUI -> R.string.species_tambaqui
    Especie.PIRARUCU -> R.string.species_pirarucu
    Especie.OUTRA -> R.string.species_other
}

@StringRes
fun FaseLote.labelRes(): Int = when (this) {
    FaseLote.ALEVINAGEM -> R.string.phase_alevinagem
    FaseLote.RECRIA -> R.string.phase_recria
    FaseLote.ENGORDA -> R.string.phase_engorda
}

@StringRes
fun MomentoMedicao.labelRes(): Int = when (this) {
    MomentoMedicao.MANHA -> R.string.moment_manha
    MomentoMedicao.MEIO_DIA -> R.string.moment_meio_dia
    MomentoMedicao.TARDE -> R.string.moment_tarde
    MomentoMedicao.NOITE -> R.string.moment_noite
}
