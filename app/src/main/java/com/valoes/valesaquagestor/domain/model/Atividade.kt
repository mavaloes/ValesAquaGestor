package com.valoes.valesaquagestor.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Atividade diária que o usuário agenda no calendário (ex.: "Limpar os bebedouros").
 *
 * É uma anotação do dia a dia: fica só no aparelho e não entra em nenhum cálculo de água, ração ou
 * biomassa.
 *
 * @property id chave local; `0` para uma atividade ainda não salva.
 * @property titulo o que fazer; obrigatório.
 * @property data dia em que a atividade deve ser feita.
 * @property hora hora marcada, ou `null` para "qualquer hora do dia".
 * @property observacao detalhes opcionais.
 * @property concluida `true` depois que o usuário marca como feita.
 */
data class Atividade(
    val id: Long = 0,
    val titulo: String,
    val data: LocalDate,
    val hora: LocalTime? = null,
    val observacao: String? = null,
    val concluida: Boolean = false,
)
