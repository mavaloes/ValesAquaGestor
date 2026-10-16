package com.valoes.valesaquagestor.domain.model

/**
 * Momento do dia em que a água foi medida.
 *
 * Importa porque oxigênio e pH têm leitura de manhã e de fim de tarde, e a comparação entre as duas
 * alimenta os alertas compostos (variação do pH e previsão de oxigênio).
 *
 * @see F01 p.66
 * @see F13 p.27
 */
enum class MomentoMedicao {
    MANHA,
    MEIO_DIA,
    TARDE,
    NOITE,
}
