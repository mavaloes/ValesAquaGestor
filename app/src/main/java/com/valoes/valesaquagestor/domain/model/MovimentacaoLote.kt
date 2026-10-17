package com.valoes.valesaquagestor.domain.model

import java.time.Instant

/**
 * Saída de peixes do lote: mortalidade, despesca parcial, despesca final ou transferência.
 *
 * @property id chave local; 0 para uma movimentação ainda não salva.
 * @property uuid identidade na sincronização; vazio até o primeiro salvamento.
 * @property quantidade número de peixes que saíram.
 * @property pesoTotalKg peso total dos peixes retirados, em kg.
 * @property tanqueDestinoId tanque de destino, só na [TipoMovimentacao.TRANSFERENCIA].
 * @property criadoPorUserId `userId` de quem criou o registro; `null` nos registros anteriores à Fase 08.
 */
data class MovimentacaoLote(
    val id: Long = 0,
    val uuid: String = "",
    val loteId: Long,
    val dataHora: Instant,
    val tipo: TipoMovimentacao,
    val quantidade: Int,
    val pesoTotalKg: Double? = null,
    val tanqueDestinoId: Long? = null,
    val observacao: String? = null,
    val criadoPorUserId: String? = null,
)
