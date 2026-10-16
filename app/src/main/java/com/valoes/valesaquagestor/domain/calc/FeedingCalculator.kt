package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.rules.AlertLevel
import com.valoes.valesaquagestor.domain.rules.TemperatureThresholds
import com.valoes.valesaquagestor.domain.rules.WaterEvaluator
import com.valoes.valesaquagestor.domain.rules.WaterRanges

/**
 * Linha da tabela de ração da tilápia.
 *
 * @property minG peso mínimo da faixa, em g (inclusivo).
 * @property maxG peso máximo da faixa, em g (exclusivo).
 * @property pelletMm tamanho do pellet, em mm; `null` para ração em pó.
 * @property crudeProteinPct proteína bruta da ração, em %.
 * @property meals número mínimo de refeições por dia.
 * @property pctBodyWeight ração diária, em % do peso vivo.
 * @property expectedFcr taxa de conversão alimentar (TCA) esperada na faixa.
 */
data class FeedBand(
    val minG: Double,
    val maxG: Double,
    val pelletMm: ClosedFloatingPointRange<Double>?,
    val crudeProteinPct: ClosedFloatingPointRange<Double>,
    val meals: Int,
    val pctBodyWeight: Double,
    val expectedFcr: Double,
)

/**
 * Tabela de arraçoamento da tilápia, para água entre 25 e 28 °C. Faixa de peso é [mínimo, máximo):
 * 50 g pertence à faixa "50 a 150". Todos os números abaixo vêm da F14 p.34.
 *
 * @see F14 p.34
 */
object TilapiaFeedTable {
    val bands: List<FeedBand> = listOf(
        FeedBand(1.0, 5.0, null, 42.0..42.0, meals = 5, pctBodyWeight = 14.0, expectedFcr = 1.0),
        FeedBand(5.0, 10.0, 2.0..3.0, 42.0..42.0, meals = 4, pctBodyWeight = 8.0, expectedFcr = 1.0),
        FeedBand(10.0, 20.0, 2.0..3.0, 42.0..42.0, meals = 3, pctBodyWeight = 5.0, expectedFcr = 1.1),
        FeedBand(20.0, 50.0, 2.0..3.0, 42.0..42.0, meals = 3, pctBodyWeight = 4.5, expectedFcr = 1.1),
        FeedBand(50.0, 150.0, 3.0..4.0, 36.0..36.0, meals = 3, pctBodyWeight = 3.4, expectedFcr = 1.2),
        FeedBand(150.0, 250.0, 4.0..6.0, 32.0..32.0, meals = 3, pctBodyWeight = 3.0, expectedFcr = 1.3),
        FeedBand(250.0, 400.0, 4.0..6.0, 28.0..32.0, meals = 2, pctBodyWeight = 2.2, expectedFcr = 1.4),
        FeedBand(400.0, 600.0, 4.0..6.0, 28.0..32.0, meals = 2, pctBodyWeight = 1.4, expectedFcr = 1.6),
        FeedBand(600.0, 800.0, 4.0..6.0, 28.0..32.0, meals = 2, pctBodyWeight = 1.0, expectedFcr = 1.7),
        FeedBand(800.0, 1300.0, 6.0..8.0, 28.0..32.0, meals = 2, pctBodyWeight = 0.8, expectedFcr = 1.8),
        FeedBand(1300.0, 1800.0, 6.0..8.0, 28.0..32.0, meals = 2, pctBodyWeight = 0.6, expectedFcr = 2.0),
    )

    /** Faixa do peso médio informado, ou `null` se estiver abaixo de 1 g ou a partir de 1.800 g (fora da tabela). */
    fun bandFor(avgWeightG: Double): FeedBand? =
        bands.firstOrNull { avgWeightG >= it.minG && avgWeightG < it.maxG }
}

/** Leituras de água usadas nas travas da ração. Valor `null` = não medido. */
data class WaterReadings(
    val oxygenMgL: Double? = null,
    val temperatureC: Double? = null,
    val toxicAmmoniaMgL: Double? = null,
    val nitriteMgL: Double? = null,
)

/** Motivo que zera a ração sugerida (`specs/08`, conflito 13: só estes quatro parâmetros travam). */
enum class FeedBlockReason {
    LOW_OXYGEN,
    TEMPERATURE,
    TOXIC_AMMONIA,
    NITRITE,
}

/** Aviso que não zera a ração. */
enum class FeedWarning {
    /** Oxigênio entre 3 e 4 mg/L (inclui o valor exato 3,0, onde F13 e F14 divergem). */
    OXYGEN_ATTENTION,

    /** Temperatura não informada: usado o fator 1,0, que é a base da tabela. */
    TEMPERATURE_MISSING,
}

/**
 * Ração sugerida para um trato. É um **guia**: o consumo real depende da água, da ração e da
 * sanidade, e se sobrar ração a refeição seguinte deve diminuir (F14 p.34).
 *
 * @property band linha da tabela usada.
 * @property temperatureFactor fator da temperatura (0 a 1).
 * @property dailyKg ração do dia, em kg; 0 quando há [blockReasons].
 * @property perMealKg ração por refeição, em kg; 0 quando há [blockReasons].
 * @property blockReasons motivos que zeraram a ração.
 * @property warnings avisos que não zeram a ração.
 */
data class FeedSuggestion(
    val band: FeedBand,
    val temperatureFactor: Double,
    val dailyKg: Double,
    val perMealKg: Double,
    val blockReasons: List<FeedBlockReason>,
    val warnings: List<FeedWarning>,
)

/**
 * Resultado do reajuste semanal da ração.
 *
 * @property weightGainKg ganho de peso vivo estimado na semana, em kg.
 * @property adjustedBiomassKg biomassa estimada depois do ganho, em kg.
 * @property adjustedAvgWeightG peso médio estimado, em g (define a nova faixa da tabela).
 */
data class WeeklyAdjustment(
    val weightGainKg: Double,
    val adjustedBiomassKg: Double,
    val adjustedAvgWeightG: Double,
)

/** Cálculo de ração da tilápia (`specs/03-regras-negocio.md`, seções 4.2 e 4.3). */
object FeedingCalculator {

    /**
     * Fator de temperatura da água: [16, 20) → 0,6 · [20, 25) → 0,8 · [25, 30) → 1,0 ·
     * [30, 32] → 0,8 · fora de 16 a 32 °C → 0 (não fornecer). A fonte escreve números inteiros;
     * aqui as faixas são contínuas.
     *
     * @see F14 p.38
     */
    fun temperatureFactor(celsius: Double): Double = when {
        celsius < TemperatureThresholds.CRITICAL_BELOW_C -> 0.0
        celsius < 20.0 -> 0.6
        celsius < 25.0 -> 0.8
        celsius < 30.0 -> 1.0
        celsius <= TemperatureThresholds.ADEQUATE_MAX_C -> 0.8
        else -> 0.0
    }

    /** Biomassa em kg: `nPeixes × pesoMedioG / 1000`. */
    fun biomassKg(fishCount: Int, avgWeightG: Double): Double = fishCount * avgWeightG / 1000.0

    /**
     * Sugere a ração do trato.
     *
     * `racaoDiariaKg = biomassaKg × pctPV/100 × fatorTemperatura`, dividida pelo número mínimo de
     * refeições da faixa. As travas devolvem 0 kg com o motivo e nunca bloqueiam a tela. Elas
     * reaproveitam o [WaterEvaluator]: nível CRITICO ou EMERGENCIA em oxigênio, temperatura, amônia
     * tóxica ou nitrito. O oxigênio de 3,0 mg/L exato é permitido, com aviso.
     *
     * @return a sugestão, ou `null` se o peso médio estiver fora da tabela (o usuário informa o % do peso vivo).
     */
    fun suggest(
        fishCount: Int,
        avgWeightG: Double,
        water: WaterReadings = WaterReadings(),
        ranges: WaterRanges = WaterRanges.DEFAULT,
    ): FeedSuggestion? {
        val band = TilapiaFeedTable.bandFor(avgWeightG) ?: return null

        val blocks = buildList {
            water.oxygenMgL?.let { if (blocksFeeding(WaterEvaluator.evaluateOxygen(it, ranges).level)) add(FeedBlockReason.LOW_OXYGEN) }
            water.temperatureC?.let { if (blocksFeeding(WaterEvaluator.evaluateTemperature(it, ranges).level)) add(FeedBlockReason.TEMPERATURE) }
            water.toxicAmmoniaMgL?.let { if (blocksFeeding(WaterEvaluator.evaluateToxicAmmonia(it, ranges).level)) add(FeedBlockReason.TOXIC_AMMONIA) }
            water.nitriteMgL?.let { if (blocksFeeding(WaterEvaluator.evaluateNitrite(it, ranges).level)) add(FeedBlockReason.NITRITE) }
        }
        val warnings = buildList {
            val oxygen = water.oxygenMgL
            if (oxygen != null && WaterEvaluator.evaluateOxygen(oxygen, ranges).level == AlertLevel.ATENCAO) {
                add(FeedWarning.OXYGEN_ATTENTION)
            }
            if (water.temperatureC == null) add(FeedWarning.TEMPERATURE_MISSING)
        }

        val factor = water.temperatureC?.let(::temperatureFactor) ?: 1.0
        val dailyKg = if (blocks.isEmpty()) {
            biomassKg(fishCount, avgWeightG) * (band.pctBodyWeight / 100.0) * factor
        } else {
            0.0
        }
        return FeedSuggestion(band, factor, dailyKg, dailyKg / band.meals, blocks, warnings)
    }

    /**
     * Reajuste semanal: `ganho = racaoDaSemana / TCAesperada(faixa atual)`;
     * `PV ajustado = PV anterior + ganho`; peso médio ajustado = PV ajustado / nPeixes.
     * A biometria real sempre substitui esta estimativa.
     *
     * @return o reajuste, ou `null` se o peso médio atual estiver fora da tabela.
     * @see F14 p.38-41
     */
    fun weeklyAdjustment(fishCount: Int, avgWeightG: Double, feedOfferedKg: Double): WeeklyAdjustment? {
        val band = TilapiaFeedTable.bandFor(avgWeightG) ?: return null
        val gainKg = feedOfferedKg / band.expectedFcr
        val adjustedBiomassKg = biomassKg(fishCount, avgWeightG) + gainKg
        return WeeklyAdjustment(gainKg, adjustedBiomassKg, adjustedBiomassKg * 1000.0 / fishCount)
    }

    /** Ganho de peso vivo, em kg, entre duas biometrias do mesmo número de peixes. */
    fun weightGainKg(fishCount: Int, previousAvgWeightG: Double, currentAvgWeightG: Double): Double =
        fishCount * (currentAvgWeightG - previousAvgWeightG) / 1000.0

    /** TCA real: `racaoFornecidaKg / ganhoPesoKg`. `null` se não houve ganho de peso. @see F14 p.48 */
    fun realFcr(feedKg: Double, weightGainKg: Double): Double? =
        if (weightGainKg > 0.0) feedKg / weightGainKg else null

    /** Ganho de peso diário (GPD), em g/dia. `null` se [days] não for positivo. */
    fun dailyGainG(previousAvgWeightG: Double, currentAvgWeightG: Double, days: Int): Double? =
        if (days > 0) (currentAvgWeightG - previousAvgWeightG) / days else null

    private fun blocksFeeding(level: AlertLevel): Boolean = level.isAtLeast(AlertLevel.CRITICO)
}
