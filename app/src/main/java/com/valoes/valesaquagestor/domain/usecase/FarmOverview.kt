package com.valoes.valesaquagestor.domain.usecase

import com.valoes.valesaquagestor.domain.calc.LotCalculator
import com.valoes.valesaquagestor.domain.model.Arracoamento
import com.valoes.valesaquagestor.domain.model.Biometria
import com.valoes.valesaquagestor.domain.model.LatestWaterReadings
import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import com.valoes.valesaquagestor.domain.model.QualidadeAgua
import com.valoes.valesaquagestor.domain.model.Tanque

/**
 * Tudo o que a Home e as listas precisam saber de um lote ativo. Os números derivados (peixes
 * vivos, biomassa, mortalidade) são calculados aqui, nunca gravados.
 *
 * @property feedingsToday tratos registrados hoje.
 * @property latestBiometries as duas biometrias mais recentes, da mais nova à mais antiga.
 */
data class LotOverview(
    val lote: Lote,
    val movements: List<MovimentacaoLote>,
    val feedingsToday: List<Arracoamento>,
    val latestBiometries: List<Biometria>,
) {
    val currentQuantity: Int get() = LotCalculator.currentQuantity(lote, movements)
    val biomassKg: Double get() = LotCalculator.biomassKg(lote, movements)
    val mortalityPct: Double get() = LotCalculator.accumulatedMortalityPct(lote, movements)
}

/**
 * Um tanque ativo com seus lotes ativos e as medições de água.
 *
 * @property readings última leitura de cada parâmetro.
 * @property waterToday medições feitas hoje, da mais antiga à mais nova.
 */
data class TankOverview(
    val tanque: Tanque,
    val lots: List<LotOverview>,
    val readings: LatestWaterReadings,
    val waterToday: List<QualidadeAgua>,
)

/** Retrato da fazenda: todos os tanques ativos. */
data class FarmOverview(val tanks: List<TankOverview>)
