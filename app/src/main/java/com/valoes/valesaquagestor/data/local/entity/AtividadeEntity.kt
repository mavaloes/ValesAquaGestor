package com.valoes.valesaquagestor.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Atividade diária do calendário.
 *
 * Diferente das outras tabelas, esta é **só do aparelho**: não tem `uuid` nem campos de
 * sincronização, e a exclusão apaga a linha de vez. Se um dia ela for sincronizada, será preciso
 * uma migração para acrescentar esses campos.
 *
 * @property dataEpochDia dia da atividade, em dias desde 01/01/1970 (`LocalDate.toEpochDay()`).
 * @property horaMinutos hora marcada, em minutos desde a meia-noite (0 a 1439), ou `null` se não tem hora.
 * @property observacao detalhes opcionais.
 * @property concluida `true` depois que o usuário marca como feita.
 */
@Entity(
    tableName = "atividade",
    indices = [Index(value = ["dataEpochDia"])],
)
data class AtividadeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val dataEpochDia: Long,
    val horaMinutos: Int? = null,
    val observacao: String? = null,
    val concluida: Boolean = false,
)
