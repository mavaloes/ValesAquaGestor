package com.valoes.valesaquagestor.data.repository

import com.valoes.valesaquagestor.data.local.entity.ArracoamentoEntity
import com.valoes.valesaquagestor.data.local.entity.BiometriaEntity
import com.valoes.valesaquagestor.data.local.entity.LoteEntity
import com.valoes.valesaquagestor.data.local.entity.MovimentacaoLoteEntity
import com.valoes.valesaquagestor.data.local.entity.QualidadeAguaEntity
import com.valoes.valesaquagestor.data.local.entity.TanqueEntity
import com.valoes.valesaquagestor.domain.model.Arracoamento
import com.valoes.valesaquagestor.domain.model.Biometria
import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import com.valoes.valesaquagestor.domain.model.QualidadeAgua
import com.valoes.valesaquagestor.domain.model.Tanque
import java.time.Instant

/*
 * Mapeamento entity <-> modelo de domínio. Datas: epoch millis no banco, Instant no domínio.
 *
 * Os `toEntity` recebem a entity já gravada (`base`, `null` num registro novo) para manter
 * `id`, `uuid` e `excluido`. Todo salvamento grava `atualizadoEm = now` e `sincronizado = false`.
 */

private fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)

fun TanqueEntity.toDomain() = Tanque(
    id = id, uuid = uuid, nome = nome, tipo = tipo, areaM2 = areaM2,
    profundidadeMediaM = profundidadeMediaM, volumeM3 = volumeM3, usaAerador = usaAerador,
    aeradorPotenciaHp = aeradorPotenciaHp, renovacaoAguaPctDia = renovacaoAguaPctDia, status = status,
)

fun Tanque.toEntity(base: TanqueEntity?, newUuid: String, now: Long) = TanqueEntity(
    id = base?.id ?: 0, nome = nome, tipo = tipo, areaM2 = areaM2,
    profundidadeMediaM = profundidadeMediaM, volumeM3 = volumeM3, usaAerador = usaAerador,
    aeradorPotenciaHp = aeradorPotenciaHp, renovacaoAguaPctDia = renovacaoAguaPctDia, status = status,
    uuid = base?.uuid ?: newUuid, atualizadoEm = now, excluido = base?.excluido ?: false, sincronizado = false,
)

fun LoteEntity.toDomain() = Lote(
    id = id, uuid = uuid, tanqueId = tanqueId, especie = especie, fase = fase,
    quantidadeInicial = quantidadeInicial, pesoMedioInicialG = pesoMedioInicialG,
    dataPovoamento = dataPovoamento.toInstant(), pesoMedioAtualG = pesoMedioAtualG,
    origemAlevinos = origemAlevinos, status = status, dataEncerramento = dataEncerramento?.toInstant(),
)

fun Lote.toEntity(base: LoteEntity?, newUuid: String, now: Long) = LoteEntity(
    id = base?.id ?: 0, tanqueId = tanqueId, especie = especie, fase = fase,
    quantidadeInicial = quantidadeInicial, pesoMedioInicialG = pesoMedioInicialG,
    dataPovoamento = dataPovoamento.toEpochMilli(), pesoMedioAtualG = pesoMedioAtualG,
    origemAlevinos = origemAlevinos, status = status, dataEncerramento = dataEncerramento?.toEpochMilli(),
    uuid = base?.uuid ?: newUuid, atualizadoEm = now, excluido = base?.excluido ?: false, sincronizado = false,
)

fun QualidadeAguaEntity.toDomain() = QualidadeAgua(
    id = id, uuid = uuid, tanqueId = tanqueId, dataHora = dataHora.toInstant(), momento = momento,
    transparenciaCm = transparenciaCm, temperaturaC = temperaturaC, oxigenioMgL = oxigenioMgL, ph = ph,
    amoniaTotalMgL = amoniaTotalMgL, nitritoMgL = nitritoMgL, alcalinidadeMgL = alcalinidadeMgL,
    durezaMgL = durezaMgL, diaNublado = diaNublado,
)

fun QualidadeAgua.toEntity(base: QualidadeAguaEntity?, newUuid: String, now: Long) = QualidadeAguaEntity(
    id = base?.id ?: 0, tanqueId = tanqueId, dataHora = dataHora.toEpochMilli(), momento = momento,
    transparenciaCm = transparenciaCm, temperaturaC = temperaturaC, oxigenioMgL = oxigenioMgL, ph = ph,
    amoniaTotalMgL = amoniaTotalMgL, nitritoMgL = nitritoMgL, alcalinidadeMgL = alcalinidadeMgL,
    durezaMgL = durezaMgL, diaNublado = diaNublado,
    uuid = base?.uuid ?: newUuid, atualizadoEm = now, excluido = base?.excluido ?: false, sincronizado = false,
)

fun ArracoamentoEntity.toDomain() = Arracoamento(
    id = id, uuid = uuid, loteId = loteId, dataHora = dataHora.toInstant(), quantidadeKg = quantidadeKg,
    suspenso = suspenso, motivoSuspensao = motivoSuspensao, marcaRacao = marcaRacao,
    proteinaBrutaPct = proteinaBrutaPct, tamanhoPelletMm = tamanhoPelletMm,
    comportamento = comportamento, sobraRacao = sobraRacao,
)

fun Arracoamento.toEntity(base: ArracoamentoEntity?, newUuid: String, now: Long) = ArracoamentoEntity(
    id = base?.id ?: 0, loteId = loteId, dataHora = dataHora.toEpochMilli(), quantidadeKg = quantidadeKg,
    suspenso = suspenso, motivoSuspensao = motivoSuspensao, marcaRacao = marcaRacao,
    proteinaBrutaPct = proteinaBrutaPct, tamanhoPelletMm = tamanhoPelletMm,
    comportamento = comportamento, sobraRacao = sobraRacao,
    uuid = base?.uuid ?: newUuid, atualizadoEm = now, excluido = base?.excluido ?: false, sincronizado = false,
)

fun BiometriaEntity.toDomain() = Biometria(
    id = id, uuid = uuid, loteId = loteId, dataHora = dataHora.toInstant(),
    pesoAmostraKg = pesoAmostraKg, numeroPeixesAmostra = numeroPeixesAmostra, observacaoSaude = observacaoSaude,
)

fun Biometria.toEntity(base: BiometriaEntity?, newUuid: String, now: Long) = BiometriaEntity(
    id = base?.id ?: 0, loteId = loteId, dataHora = dataHora.toEpochMilli(),
    pesoAmostraKg = pesoAmostraKg, numeroPeixesAmostra = numeroPeixesAmostra, observacaoSaude = observacaoSaude,
    uuid = base?.uuid ?: newUuid, atualizadoEm = now, excluido = base?.excluido ?: false, sincronizado = false,
)

fun MovimentacaoLoteEntity.toDomain() = MovimentacaoLote(
    id = id, uuid = uuid, loteId = loteId, dataHora = dataHora.toInstant(), tipo = tipo,
    quantidade = quantidade, pesoTotalKg = pesoTotalKg, tanqueDestinoId = tanqueDestinoId, observacao = observacao,
)

fun MovimentacaoLote.toEntity(base: MovimentacaoLoteEntity?, newUuid: String, now: Long) = MovimentacaoLoteEntity(
    id = base?.id ?: 0, loteId = loteId, dataHora = dataHora.toEpochMilli(), tipo = tipo,
    quantidade = quantidade, pesoTotalKg = pesoTotalKg, tanqueDestinoId = tanqueDestinoId, observacao = observacao,
    uuid = base?.uuid ?: newUuid, atualizadoEm = now, excluido = base?.excluido ?: false, sincronizado = false,
)
