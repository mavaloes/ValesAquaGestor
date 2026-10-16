package com.valoes.valesaquagestor.domain.rules

/**
 * Resultado da avaliação de um valor: o nível, o que ele significa e o que fazer.
 *
 * As chaves são identificadores do domínio, no formato `water_<parametro>_<nivel>` (mensagem) e
 * `action_water_<parametro>_<nivel>` (ação). Só a camada de apresentação converte as chaves em
 * textos de `strings.xml`; o domínio nunca guarda texto para o usuário.
 *
 * @property level nível de alerta.
 * @property messageKey chave do texto que diz o que o valor significa.
 * @property actionKey chave do texto que diz o que fazer.
 */
data class WaterStatus(
    val level: AlertLevel,
    val messageKey: String,
    val actionKey: String,
)
