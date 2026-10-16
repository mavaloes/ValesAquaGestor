package com.valoes.valesaquagestor.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.valoes.valesaquagestor.domain.model.StatusTanque
import com.valoes.valesaquagestor.domain.model.TipoTanque

/**
 * Tanque da fazenda: viveiro escavado, açude, tanque-rede, alvenaria ou redondo.
 *
 * Para [TipoTanque.ESCAVADO] e [TipoTanque.ACUDE], [areaM2] é obrigatória, porque a capacidade é
 * calculada pela área (F03 p.15). Para os demais tipos, [volumeM3] é obrigatório (F12 p.14).
 * A validação fica no formulário e no repositório.
 *
 * A "condição de criação" (1 a 4 da tabela F03 p.15) não é gravada: é derivada de [usaAerador] e
 * [renovacaoAguaPctDia].
 *
 * @property id chave local, gerada pelo banco.
 * @property nome nome dado pelo produtor (ex.: "Viveiro 3").
 * @property areaM2 área da lâmina d'água, em m².
 * @property profundidadeMediaM profundidade média, em m. Com a área, permite calcular o volume.
 * @property volumeM3 volume de água, em m³. Valor em litros é convertido na entrada.
 * @property usaAerador se o tanque tem aerador.
 * @property aeradorPotenciaHp potência do aerador, em HP (F13 p.29).
 * @property renovacaoAguaPctDia renovação de água, em % do volume por dia.
 * @property status se o tanque está em uso.
 * @property uuid identidade do registro na sincronização; único no banco.
 * @property atualizadoEm última alteração, em epoch millis; na sincronização vence o maior.
 * @property excluido exclusão lógica: some das listas, mas fica no banco até o servidor confirmar.
 * @property sincronizado `false` enquanto houver alteração ainda não enviada ao servidor.
 */
@Entity(
    tableName = "tanque",
    indices = [Index(value = ["uuid"], unique = true)],
)
data class Tanque(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val tipo: TipoTanque,
    val areaM2: Double? = null,
    val profundidadeMediaM: Double? = null,
    val volumeM3: Double? = null,
    val usaAerador: Boolean,
    val aeradorPotenciaHp: Double? = null,
    val renovacaoAguaPctDia: Double,
    val status: StatusTanque = StatusTanque.ATIVO,
    val uuid: String,
    val atualizadoEm: Long,
    val excluido: Boolean = false,
    val sincronizado: Boolean = false,
)
