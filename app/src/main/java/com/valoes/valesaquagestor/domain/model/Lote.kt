package com.valoes.valesaquagestor.domain.model

import java.time.Instant

/**
 * Lote de peixes povoado num tanque.
 *
 * Quantidade atual, biomassa e mortalidade **não são campos**: são calculadas por `LotCalculator`
 * a partir de [quantidadeInicial], [pesoMedioAtualG] e das [MovimentacaoLote].
 *
 * @property id chave local; 0 para um lote ainda não salvo.
 * @property uuid identidade na sincronização; vazio até o primeiro salvamento.
 * @property quantidadeInicial número de peixes no povoamento.
 * @property pesoMedioInicialG peso médio no povoamento, em g.
 * @property pesoMedioAtualG peso médio mais recente, em g; o repositório o atualiza a cada biometria.
 */
data class Lote(
    val id: Long = 0,
    val uuid: String = "",
    val tanqueId: Long,
    val especie: Especie,
    val fase: FaseLote,
    val quantidadeInicial: Int,
    val pesoMedioInicialG: Double,
    val dataPovoamento: Instant,
    val pesoMedioAtualG: Double,
    val origemAlevinos: String? = null,
    val status: StatusLote = StatusLote.ATIVO,
    val dataEncerramento: Instant? = null,
)
