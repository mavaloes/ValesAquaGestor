package com.valoes.valesaquagestor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.valoes.valesaquagestor.domain.model.ComportamentoPeixe
import com.valoes.valesaquagestor.domain.model.MotivoSuspensao

/**
 * Trato (arraçoamento) de um lote, com os campos da Planilha 4 (F01 p.153).
 *
 * FK para [Lote] com `RESTRICT`: a exclusão é sempre lógica.
 *
 * @property loteId lote alimentado.
 * @property dataHora horário do trato, em epoch millis.
 * @property quantidadeKg ração **ofertada**, em kg; vale 0 quando [suspenso].
 * @property suspenso se o trato foi suspenso.
 * @property motivoSuspensao motivo da suspensão, quando [suspenso].
 * @property marcaRacao marca da ração.
 * @property proteinaBrutaPct proteína bruta da ração, em %.
 * @property tamanhoPelletMm tamanho do pellet, em mm.
 * @property comportamento comportamento dos peixes no trato.
 * @property sobraRacao se sobrou ração (sobra pede reduzir a próxima refeição, F14 p.46).
 * @property uuid identidade do registro na sincronização; único no banco.
 * @property atualizadoEm última alteração, em epoch millis; na sincronização vence o maior.
 * @property excluido exclusão lógica: some das listas, mas fica no banco até o servidor confirmar.
 * @property sincronizado `false` enquanto houver alteração ainda não enviada ao servidor.
 */
@Entity(
    tableName = "arracoamento",
    foreignKeys = [
        ForeignKey(
            entity = Lote::class,
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
data class Arracoamento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loteId: Long,
    val dataHora: Long,
    val quantidadeKg: Double,
    val suspenso: Boolean = false,
    val motivoSuspensao: MotivoSuspensao? = null,
    val marcaRacao: String? = null,
    val proteinaBrutaPct: Double? = null,
    val tamanhoPelletMm: Double? = null,
    val comportamento: ComportamentoPeixe,
    val sobraRacao: Boolean = false,
    val uuid: String,
    val atualizadoEm: Long,
    val excluido: Boolean = false,
    val sincronizado: Boolean = false,
)
