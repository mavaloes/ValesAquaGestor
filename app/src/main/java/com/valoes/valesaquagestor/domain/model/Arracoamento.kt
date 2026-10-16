package com.valoes.valesaquagestor.domain.model

import java.time.Instant

/**
 * Trato (arraçoamento) de um lote.
 *
 * @property id chave local; 0 para um trato ainda não salvo.
 * @property uuid identidade na sincronização; vazio até o primeiro salvamento.
 * @property quantidadeKg ração **ofertada**, em kg; vale 0 quando [suspenso].
 * @property proteinaBrutaPct proteína bruta da ração, em %.
 * @property tamanhoPelletMm tamanho do pellet, em mm.
 */
data class Arracoamento(
    val id: Long = 0,
    val uuid: String = "",
    val loteId: Long,
    val dataHora: Instant,
    val quantidadeKg: Double,
    val suspenso: Boolean = false,
    val motivoSuspensao: MotivoSuspensao? = null,
    val marcaRacao: String? = null,
    val proteinaBrutaPct: Double? = null,
    val tamanhoPelletMm: Double? = null,
    val comportamento: ComportamentoPeixe,
    val sobraRacao: Boolean = false,
)
