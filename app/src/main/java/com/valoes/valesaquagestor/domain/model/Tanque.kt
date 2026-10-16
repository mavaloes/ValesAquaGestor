package com.valoes.valesaquagestor.domain.model

/**
 * Tanque da fazenda no domínio (sem detalhes de banco).
 *
 * Para [TipoTanque.ESCAVADO] e [TipoTanque.ACUDE], [areaM2] é obrigatória (capacidade por área,
 * F03 p.15). Para os demais tipos, [volumeM3] é obrigatório (F12 p.14).
 *
 * @property id chave local; 0 para um tanque ainda não salvo.
 * @property uuid identidade na sincronização; vazio até o primeiro salvamento.
 * @property areaM2 área da lâmina d'água, em m².
 * @property profundidadeMediaM profundidade média, em m.
 * @property volumeM3 volume de água, em m³.
 * @property aeradorPotenciaHp potência do aerador, em HP.
 * @property renovacaoAguaPctDia renovação de água, em % do volume por dia.
 */
data class Tanque(
    val id: Long = 0,
    val uuid: String = "",
    val nome: String,
    val tipo: TipoTanque,
    val areaM2: Double? = null,
    val profundidadeMediaM: Double? = null,
    val volumeM3: Double? = null,
    val usaAerador: Boolean,
    val aeradorPotenciaHp: Double? = null,
    val renovacaoAguaPctDia: Double,
    val status: StatusTanque = StatusTanque.ATIVO,
)
