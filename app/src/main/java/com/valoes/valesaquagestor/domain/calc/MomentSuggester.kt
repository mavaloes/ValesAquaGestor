package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import java.time.LocalTime

/**
 * Sugere o [MomentoMedicao] pela hora do registro. É só uma sugestão: o usuário pode trocar.
 *
 * Faixas (decisão do responsável, `specs/08-fontes-e-conflitos.md`, conflito 14): manhã antes das
 * 11 h · meio-dia de 11 h até antes das 14 h · tarde de 14 h até antes das 19 h · noite a partir das 19 h.
 */
object MomentSuggester {

    /** @see specs/08-fontes-e-conflitos.md, conflito 14 — a manhã vai até antes das 11 h (a transparência é medida das 11 h às 12 h, F01 p.65). */
    const val MIDDAY_FROM_HOUR = 11

    /** @see specs/08-fontes-e-conflitos.md, conflito 14 — o meio-dia vai até antes das 14 h. */
    const val AFTERNOON_FROM_HOUR = 14

    /** @see specs/08-fontes-e-conflitos.md, conflito 14 — a tarde vai até antes das 19 h. */
    const val NIGHT_FROM_HOUR = 19

    fun suggest(time: LocalTime): MomentoMedicao = when {
        time.hour < MIDDAY_FROM_HOUR -> MomentoMedicao.MANHA
        time.hour < AFTERNOON_FROM_HOUR -> MomentoMedicao.MEIO_DIA
        time.hour < NIGHT_FROM_HOUR -> MomentoMedicao.TARDE
        else -> MomentoMedicao.NOITE
    }
}
