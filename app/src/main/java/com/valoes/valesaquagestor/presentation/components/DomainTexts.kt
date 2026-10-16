package com.valoes.valesaquagestor.presentation.components

import androidx.annotation.StringRes
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.calc.FeedBlockReason
import com.valoes.valesaquagestor.domain.model.ComportamentoPeixe
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.model.MotivoSuspensao
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
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

@StringRes
fun MotivoSuspensao.labelRes(): Int = when (this) {
    MotivoSuspensao.OXIGENIO_BAIXO -> R.string.reason_oxigenio_baixo
    MotivoSuspensao.AMONIA_ALTA -> R.string.reason_amonia_alta
    MotivoSuspensao.NITRITO_ALTO -> R.string.reason_nitrito_alto
    MotivoSuspensao.TEMPERATURA -> R.string.reason_temperatura
    MotivoSuspensao.SOBRA_RACAO -> R.string.reason_sobra_racao
    MotivoSuspensao.DOENCA -> R.string.reason_doenca
    MotivoSuspensao.OUTRO -> R.string.reason_outro
}

@StringRes
fun ComportamentoPeixe.labelRes(): Int = when (this) {
    ComportamentoPeixe.VORAZ -> R.string.behavior_voraz
    ComportamentoPeixe.NORMAL -> R.string.behavior_normal
    ComportamentoPeixe.LENTO -> R.string.behavior_lento
    ComportamentoPeixe.BOQUEJANDO -> R.string.behavior_boquejando
    ComportamentoPeixe.SEM_APETITE -> R.string.behavior_sem_apetite
}

@StringRes
fun TipoMovimentacao.labelRes(): Int = when (this) {
    TipoMovimentacao.MORTALIDADE -> R.string.movement_mortalidade
    TipoMovimentacao.DESPESCA_PARCIAL -> R.string.movement_despesca_parcial
    TipoMovimentacao.DESPESCA_FINAL -> R.string.movement_despesca_final
    TipoMovimentacao.TRANSFERENCIA -> R.string.movement_transferencia
}

@StringRes
fun FeedBlockReason.labelRes(): Int = when (this) {
    FeedBlockReason.LOW_OXYGEN -> R.string.feeding_block_oxygen
    FeedBlockReason.TEMPERATURE -> R.string.feeding_block_temperature
    FeedBlockReason.TOXIC_AMMONIA -> R.string.feeding_block_ammonia
    FeedBlockReason.NITRITE -> R.string.feeding_block_nitrite
}
