package com.valoes.valesaquagestor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.StatusLote

/**
 * Lote de peixes povoado num tanque.
 *
 * Quantidade atual, biomassa, mortalidade acumulada e TCA real **não são gravadas**: são calculadas
 * a partir de [quantidadeInicial], [pesoMedioAtualG] e das [MovimentacaoLote]. Assim nenhum número
 * fica errado depois de uma edição feita offline.
 *
 * FK para [Tanque] com `RESTRICT`: a exclusão é sempre lógica, então o banco nunca deve apagar de
 * verdade um tanque que ainda tem lotes.
 *
 * @property tanqueId tanque onde o lote está.
 * @property quantidadeInicial número de peixes no povoamento.
 * @property pesoMedioInicialG peso médio no povoamento, em g.
 * @property dataPovoamento data do povoamento, em epoch millis.
 * @property pesoMedioAtualG peso médio mais recente, em g; atualizado a cada [Biometria].
 * @property origemAlevinos fornecedor ou origem dos alevinos.
 * @property status [StatusLote.ENCERRADO] depois da despesca final.
 * @property dataEncerramento data da despesca final, em epoch millis.
 * @property uuid identidade do registro na sincronização; único no banco.
 * @property atualizadoEm última alteração, em epoch millis; na sincronização vence o maior.
 * @property excluido exclusão lógica: some das listas, mas fica no banco até o servidor confirmar.
 * @property sincronizado `false` enquanto houver alteração ainda não enviada ao servidor.
 */
@Entity(
    tableName = "lote",
    foreignKeys = [
        ForeignKey(
            entity = Tanque::class,
            parentColumns = ["id"],
            childColumns = ["tanqueId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["tanqueId"]),
    ],
)
data class Lote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tanqueId: Long,
    val especie: Especie,
    val fase: FaseLote,
    val quantidadeInicial: Int,
    val pesoMedioInicialG: Double,
    val dataPovoamento: Long,
    val pesoMedioAtualG: Double,
    val origemAlevinos: String? = null,
    val status: StatusLote = StatusLote.ATIVO,
    val dataEncerramento: Long? = null,
    val uuid: String,
    val atualizadoEm: Long,
    val excluido: Boolean = false,
    val sincronizado: Boolean = false,
)
