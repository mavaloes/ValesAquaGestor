package com.valoes.valesaquagestor.domain.usecase

import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.model.Tanque
import com.valoes.valesaquagestor.domain.repository.BiometryRepository
import com.valoes.valesaquagestor.domain.repository.FeedingRepository
import com.valoes.valesaquagestor.domain.repository.LotMovementRepository
import com.valoes.valesaquagestor.domain.repository.LotRepository
import com.valoes.valesaquagestor.domain.repository.TankRepository
import com.valoes.valesaquagestor.domain.repository.WaterQualityRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Junta tanques, lotes, medições, tratos, biometrias e movimentações num único retrato da
 * fazenda, que se atualiza sozinho quando qualquer registro muda. Funciona 100% offline.
 *
 * "Hoje" vai da meia-noite até a meia-noite seguinte, no fuso do [Clock].
 */
class ObserveFarmOverview @Inject constructor(
    private val tanks: TankRepository,
    private val lots: LotRepository,
    private val water: WaterQualityRepository,
    private val feeding: FeedingRepository,
    private val biometry: BiometryRepository,
    private val movements: LotMovementRepository,
    private val clock: Clock,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<FarmOverview> =
        combine(tanks.observeActive(), lots.observeAllActive()) { activeTanks, activeLots -> activeTanks to activeLots }
            .flatMapLatest { (activeTanks, activeLots) ->
                if (activeTanks.isEmpty()) {
                    flowOf(FarmOverview(emptyList()))
                } else {
                    val day = today()
                    combine(
                        activeTanks.map { tank -> tankOverview(tank, activeLots.filter { it.tanqueId == tank.id }, day) },
                    ) { FarmOverview(it.toList()) }
                }
            }

    private fun tankOverview(tank: Tanque, tankLots: List<Lote>, day: Pair<Instant, Instant>): Flow<TankOverview> {
        val lotFlows: Flow<List<LotOverview>> =
            if (tankLots.isEmpty()) flowOf(emptyList()) else combine(tankLots.map { lotOverview(it, day) }) { it.toList() }
        return combine(
            water.observeLatestReadings(tank.id),
            water.observeBetween(tank.id, day.first, day.second),
            lotFlows,
        ) { readings, waterToday, lotOverviews -> TankOverview(tank, lotOverviews, readings, waterToday) }
    }

    private fun lotOverview(lote: Lote, day: Pair<Instant, Instant>): Flow<LotOverview> = combine(
        movements.observeByLot(lote.id),
        feeding.observeByLot(lote.id, day.first, day.second),
        biometry.observeLatest(lote.id, LATEST_BIOMETRIES),
    ) { lotMovements, feedingsToday, latest -> LotOverview(lote, lotMovements, feedingsToday, latest) }

    private fun today(): Pair<Instant, Instant> {
        val date = LocalDate.now(clock)
        return date.atStartOfDay(clock.zone).toInstant() to date.plusDays(1).atStartOfDay(clock.zone).toInstant()
    }

    private companion object {
        /** A última e a anterior: bastam para o peso atual e para o ganho de peso entre duas biometrias. */
        const val LATEST_BIOMETRIES = 2
    }
}
