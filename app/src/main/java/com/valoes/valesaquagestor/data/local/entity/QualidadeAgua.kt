package com.valoes.valesaquagestor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.valoes.valesaquagestor.domain.model.MomentoMedicao

/**
 * Medição da qualidade da água de um tanque (Planilhas 2 e 3, F01 p.151-152).
 *
 * Medição diária e semanal usam a mesma tabela, por isso todas as medidas são opcionais. O
 * repositório exige **pelo menos uma**.
 *
 * Grava a amônia **total**, que é o que o kit mede (F13 p.36). A amônia tóxica (NH₃) não é gravada:
 * é calculada com pH e temperatura (`specs/03-regras-negocio.md`, seção 4.1).
 * "ppm" e "mg/L" são a mesma unidade (F07 p.7); o app usa sempre mg/L.
 *
 * FK para [Tanque] com `RESTRICT`: a exclusão é sempre lógica.
 *
 * @property tanqueId tanque medido.
 * @property dataHora momento da medição, em epoch millis.
 * @property momento período do dia; oxigênio e pH têm leitura de manhã e de tarde (F01 p.66; F13 p.27).
 * @property transparenciaCm transparência no disco de Secchi, em cm.
 * @property temperaturaC temperatura da água, em °C.
 * @property oxigenioMgL oxigênio dissolvido, em mg/L.
 * @property ph pH da água.
 * @property amoniaTotalMgL amônia total (NH₃ + NH₄⁺), em mg/L.
 * @property nitritoMgL nitrito (NO₂), em mg/L.
 * @property alcalinidadeMgL alcalinidade, em mg/L de CaCO₃.
 * @property durezaMgL dureza, em mg/L de CaCO₃.
 * @property diaNublado se o dia estava nublado (dias nublados seguidos pedem cuidado com o O₂, F01 p.72).
 * @property uuid identidade do registro na sincronização; único no banco.
 * @property atualizadoEm última alteração, em epoch millis; na sincronização vence o maior.
 * @property excluido exclusão lógica: some das listas, mas fica no banco até o servidor confirmar.
 * @property sincronizado `false` enquanto houver alteração ainda não enviada ao servidor.
 */
@Entity(
    tableName = "qualidade_agua",
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
data class QualidadeAgua(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tanqueId: Long,
    val dataHora: Long,
    val momento: MomentoMedicao,
    val transparenciaCm: Double? = null,
    val temperaturaC: Double? = null,
    val oxigenioMgL: Double? = null,
    val ph: Double? = null,
    val amoniaTotalMgL: Double? = null,
    val nitritoMgL: Double? = null,
    val alcalinidadeMgL: Double? = null,
    val durezaMgL: Double? = null,
    val diaNublado: Boolean = false,
    val uuid: String,
    val atualizadoEm: Long,
    val excluido: Boolean = false,
    val sincronizado: Boolean = false,
)
