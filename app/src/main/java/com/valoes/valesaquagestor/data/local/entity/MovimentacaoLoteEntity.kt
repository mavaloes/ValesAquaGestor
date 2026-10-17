package com.valoes.valesaquagestor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao

/**
 * Saída de peixes do lote: mortalidade, despesca parcial, despesca final ou transferência
 * (Planilha 7, F01 p.156).
 *
 * É a base da quantidade atual do lote, que nunca é gravada: quantidade inicial menos a soma de
 * todas as movimentações. Ao salvar uma [TipoMovimentacao.DESPESCA_FINAL], o repositório encerra o lote.
 *
 * As duas FKs usam `RESTRICT`: a exclusão é sempre lógica.
 *
 * @property loteId lote de onde os peixes saíram.
 * @property dataHora momento do registro, em epoch millis.
 * @property tipo tipo da saída.
 * @property quantidade número de peixes que saíram.
 * @property pesoTotalKg peso total dos peixes retirados, em kg.
 * @property tanqueDestinoId tanque de destino, só na [TipoMovimentacao.TRANSFERENCIA].
 * @property observacao observação livre (ex.: sinais observados nos peixes mortos).
 * @property criadoPorUserId `userId` de quem criou o registro (`specs/06-perfis-permissoes.md`); `null` nos
 * registros anteriores à Fase 08.
 * @property uuid identidade do registro na sincronização; único no banco.
 * @property atualizadoEm última alteração, em epoch millis; na sincronização vence o maior.
 * @property excluido exclusão lógica: some das listas, mas fica no banco até o servidor confirmar.
 * @property sincronizado `false` enquanto houver alteração ainda não enviada ao servidor.
 */
@Entity(
    tableName = "movimentacao_lote",
    foreignKeys = [
        ForeignKey(
            entity = LoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["loteId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = TanqueEntity::class,
            parentColumns = ["id"],
            childColumns = ["tanqueDestinoId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["loteId"]),
        Index(value = ["tanqueDestinoId"]),
    ],
)
data class MovimentacaoLoteEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
    val loteId: Long,
    val dataHora: Long,
    val tipo: TipoMovimentacao,
    val quantidade: Int,
    val pesoTotalKg: Double? = null,
    val tanqueDestinoId: Long? = null,
    val observacao: String? = null,
    val criadoPorUserId: String? = null,
    override val uuid: String,
    override val atualizadoEm: Long,
    override val excluido: Boolean = false,
    override val sincronizado: Boolean = false,
) : SyncEntity
