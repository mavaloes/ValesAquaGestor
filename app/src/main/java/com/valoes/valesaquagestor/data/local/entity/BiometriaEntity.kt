package com.valoes.valesaquagestor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Biometria: pesagem de uma amostra de peixes do lote (Planilha 6, F01 p.155).
 *
 * A tela pode ter várias pesagens (balde com água, peixes por balde); aqui ficam só os totais.
 * O peso médio é calculado a partir de [pesoAmostraKg] e [numeroPeixesAmostra], e ao salvar a
 * biometria o repositório atualiza [LoteEntity.pesoMedioAtualG].
 *
 * FK para [LoteEntity] com `RESTRICT`: a exclusão é sempre lógica.
 *
 * @property loteId lote pesado.
 * @property dataHora momento da pesagem, em epoch millis.
 * @property pesoAmostraKg peso total dos peixes da amostra, em kg, já sem a tara.
 * @property numeroPeixesAmostra quantidade de peixes pesados.
 * @property observacaoSaude sinais de saúde observados durante a pesagem.
 * @property uuid identidade do registro na sincronização; único no banco.
 * @property atualizadoEm última alteração, em epoch millis; na sincronização vence o maior.
 * @property excluido exclusão lógica: some das listas, mas fica no banco até o servidor confirmar.
 * @property sincronizado `false` enquanto houver alteração ainda não enviada ao servidor.
 */
@Entity(
    tableName = "biometria",
    foreignKeys = [
        ForeignKey(
            entity = LoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["loteId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["loteId"]),
    ],
)
data class BiometriaEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
    val loteId: Long,
    val dataHora: Long,
    val pesoAmostraKg: Double,
    val numeroPeixesAmostra: Int,
    val observacaoSaude: String? = null,
    override val uuid: String,
    override val atualizadoEm: Long,
    override val excluido: Boolean = false,
    override val sincronizado: Boolean = false,
) : SyncEntity
