package com.valoes.valesaquagestor.farm

import com.valoes.valesaquagestor.domain.model.Arracoamento
import com.valoes.valesaquagestor.domain.model.Biometria
import com.valoes.valesaquagestor.domain.model.ComportamentoPeixe
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.LatestWaterReadings
import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import com.valoes.valesaquagestor.domain.model.QualidadeAgua
import com.valoes.valesaquagestor.domain.model.Reading
import com.valoes.valesaquagestor.domain.model.StatusLote
import com.valoes.valesaquagestor.domain.model.StatusTanque
import com.valoes.valesaquagestor.domain.model.Tanque
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import com.valoes.valesaquagestor.domain.model.TipoTanque
import com.valoes.valesaquagestor.domain.repository.BiometryRepository
import com.valoes.valesaquagestor.domain.repository.FeedingRepository
import com.valoes.valesaquagestor.domain.repository.LotMovementRepository
import com.valoes.valesaquagestor.domain.repository.LotRepository
import com.valoes.valesaquagestor.domain.repository.TankRepository
import com.valoes.valesaquagestor.domain.repository.WaterQualityRepository
import com.valoes.valesaquagestor.domain.usecase.ObserveFarmOverview
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Quarta-feira, 14/10/2026, 15:00 UTC: o "agora" fixo dos testes. */
val NOW: Instant = Instant.parse("2026-10-14T15:00:00Z")
val FIXED_CLOCK: Clock = Clock.fixed(NOW, ZoneOffset.UTC)

fun hoursAgo(hours: Long): Instant = NOW.minusSeconds(hours * 3600)
fun daysAgo(days: Long): Instant = NOW.minusSeconds(days * 86_400)

/** Tanque escavado de 1.000 m² sem aerador nem troca de água. */
fun tank(id: Long = 1, nome: String = "Viveiro $id", tipo: TipoTanque = TipoTanque.ESCAVADO, areaM2: Double? = 1000.0) = Tanque(
    id = id, uuid = "t$id", nome = nome, tipo = tipo, areaM2 = areaM2, volumeM3 = if (areaM2 == null) 20.0 else null,
    usaAerador = false, renovacaoAguaPctDia = 0.0, status = StatusTanque.ATIVO,
)

fun lot(
    id: Long = 1,
    tankId: Long = 1,
    especie: Especie = Especie.TILAPIA,
    fase: FaseLote = FaseLote.RECRIA,
    quantidadeInicial: Int = 1000,
    pesoMedioAtualG: Double = 250.0,
    dataPovoamento: Instant = daysAgo(4),
) = Lote(
    id = id, uuid = "l$id", tanqueId = tankId, especie = especie, fase = fase, quantidadeInicial = quantidadeInicial,
    pesoMedioInicialG = 10.0, dataPovoamento = dataPovoamento, pesoMedioAtualG = pesoMedioAtualG, status = StatusLote.ATIVO,
)

fun water(
    id: Long,
    tankId: Long = 1,
    at: Instant = NOW,
    momento: MomentoMedicao = MomentoMedicao.MANHA,
    oxigenio: Double? = null,
    ph: Double? = null,
    temperatura: Double? = null,
    amoniaTotal: Double? = null,
    nitrito: Double? = null,
    transparencia: Double? = null,
    alcalinidade: Double? = null,
    dureza: Double? = null,
) = QualidadeAgua(
    id = id, uuid = "w$id", tanqueId = tankId, dataHora = at, momento = momento, oxigenioMgL = oxigenio, ph = ph,
    temperaturaC = temperatura, amoniaTotalMgL = amoniaTotal, nitritoMgL = nitrito, transparenciaCm = transparencia,
    alcalinidadeMgL = alcalinidade, durezaMgL = dureza,
)

fun feeding(id: Long, lotId: Long = 1, at: Instant = NOW, kg: Double = 1.0, suspended: Boolean = false) = Arracoamento(
    id = id, uuid = "f$id", loteId = lotId, dataHora = at, quantidadeKg = kg, suspenso = suspended,
    comportamento = ComportamentoPeixe.NORMAL,
)

fun biometry(id: Long, lotId: Long = 1, at: Instant, kg: Double, fish: Int) = Biometria(
    id = id, uuid = "b$id", loteId = lotId, dataHora = at, pesoAmostraKg = kg, numeroPeixesAmostra = fish,
)

fun movement(id: Long, lotId: Long = 1, tipo: TipoMovimentacao = TipoMovimentacao.MORTALIDADE, quantity: Int, at: Instant = NOW) =
    MovimentacaoLote(id = id, uuid = "m$id", loteId = lotId, dataHora = at, tipo = tipo, quantidade = quantity)

class FakeTankRepository(initial: List<Tanque> = emptyList()) : TankRepository {
    val tanks = MutableStateFlow(initial)
    override fun observeActive(): Flow<List<Tanque>> = tanks.map { list -> list.filter { it.status == StatusTanque.ATIVO } }
    override fun observe(id: Long): Flow<Tanque?> = tanks.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun get(id: Long): Tanque? = tanks.value.firstOrNull { it.id == id }
    override suspend fun save(tanque: Tanque): Long {
        val id = if (tanque.id == 0L) (tanks.value.maxOfOrNull { it.id } ?: 0L) + 1 else tanque.id
        tanks.update { list -> list.filterNot { it.id == id } + tanque.copy(id = id) }
        return id
    }
    override suspend fun delete(id: Long) = tanks.update { list -> list.filterNot { it.id == id } }
}

class FakeLotRepository(initial: List<Lote> = emptyList()) : LotRepository {
    val lots = MutableStateFlow(initial)
    override fun observeActiveByTank(tankId: Long): Flow<List<Lote>> =
        lots.map { list -> list.filter { it.tanqueId == tankId && it.status == StatusLote.ATIVO } }
    override fun observeAllActive(): Flow<List<Lote>> = lots.map { list -> list.filter { it.status == StatusLote.ATIVO } }
    override fun observe(id: Long): Flow<Lote?> = lots.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun get(id: Long): Lote? = lots.value.firstOrNull { it.id == id }
    override suspend fun save(lote: Lote): Long {
        val id = if (lote.id == 0L) (lots.value.maxOfOrNull { it.id } ?: 0L) + 1 else lote.id
        lots.update { list -> list.filterNot { it.id == id } + lote.copy(id = id) }
        return id
    }
    override suspend fun delete(id: Long) = lots.update { list -> list.filterNot { it.id == id } }
}

/** Calcula a última leitura de cada parâmetro como o banco faz: a medição mais recente em que ele foi informado. */
class FakeWaterQualityRepository(initial: List<QualidadeAgua> = emptyList()) : WaterQualityRepository {
    val records = MutableStateFlow(initial)

    override fun observeLatestReadings(tankId: Long): Flow<LatestWaterReadings> = records.map { all ->
        val own = all.filter { it.tanqueId == tankId }.sortedByDescending { it.dataHora }
        fun latest(value: (QualidadeAgua) -> Double?): Reading? =
            own.firstOrNull { value(it) != null }?.let { Reading(value(it)!!, it.dataHora) }
        LatestWaterReadings(
            transparenciaCm = latest { it.transparenciaCm }, temperaturaC = latest { it.temperaturaC },
            oxigenioMgL = latest { it.oxigenioMgL }, ph = latest { it.ph }, amoniaTotalMgL = latest { it.amoniaTotalMgL },
            nitritoMgL = latest { it.nitritoMgL }, alcalinidadeMgL = latest { it.alcalinidadeMgL }, durezaMgL = latest { it.durezaMgL },
        )
    }

    override fun observeBetween(tankId: Long, from: Instant, to: Instant): Flow<List<QualidadeAgua>> = records.map { all ->
        all.filter { it.tanqueId == tankId && !it.dataHora.isBefore(from) && it.dataHora.isBefore(to) }.sortedBy { it.dataHora }
    }

    override suspend fun save(medicao: QualidadeAgua): Long {
        records.update { it + medicao }
        return medicao.id
    }

    override suspend fun delete(id: Long) = records.update { list -> list.filterNot { it.id == id } }
}

class FakeFeedingRepository(initial: List<Arracoamento> = emptyList()) : FeedingRepository {
    val feedings = MutableStateFlow(initial)
    override fun observeByLot(lotId: Long, from: Instant, to: Instant): Flow<List<Arracoamento>> = feedings.map { all ->
        all.filter { it.loteId == lotId && !it.dataHora.isBefore(from) && it.dataHora.isBefore(to) }.sortedBy { it.dataHora }
    }
    override suspend fun save(arracoamento: Arracoamento): Long {
        feedings.update { it + arracoamento }
        return arracoamento.id
    }
    override suspend fun delete(id: Long) = feedings.update { list -> list.filterNot { it.id == id } }
}

class FakeBiometryRepository(initial: List<Biometria> = emptyList()) : BiometryRepository {
    val biometries = MutableStateFlow(initial)
    override fun observeLatest(lotId: Long, limit: Int): Flow<List<Biometria>> = biometries.map { all ->
        all.filter { it.loteId == lotId }.sortedByDescending { it.dataHora }.take(limit)
    }
    override suspend fun save(biometria: Biometria): Long {
        biometries.update { it + biometria }
        return biometria.id
    }
    override suspend fun delete(id: Long) = biometries.update { list -> list.filterNot { it.id == id } }
}

class FakeLotMovementRepository(initial: List<MovimentacaoLote> = emptyList()) : LotMovementRepository {
    val movements = MutableStateFlow(initial)
    override fun observeByLot(lotId: Long): Flow<List<MovimentacaoLote>> = movements.map { all ->
        all.filter { it.loteId == lotId }.sortedBy { it.dataHora }
    }
    override suspend fun save(movimentacao: MovimentacaoLote): Long {
        movements.update { it + movimentacao }
        return movimentacao.id
    }
    override suspend fun delete(id: Long) = movements.update { list -> list.filterNot { it.id == id } }
}

/** Uma fazenda de teste completa: os repositórios falsos e o caso de uso ligados entre si. */
class FarmFixture(
    tanks: List<com.valoes.valesaquagestor.domain.model.Tanque> = emptyList(),
    lots: List<Lote> = emptyList(),
    waterRecords: List<QualidadeAgua> = emptyList(),
    feedings: List<Arracoamento> = emptyList(),
    biometries: List<Biometria> = emptyList(),
    movements: List<MovimentacaoLote> = emptyList(),
    val clock: Clock = FIXED_CLOCK,
) {
    val tanks = FakeTankRepository(tanks)
    val lots = FakeLotRepository(lots)
    val water = FakeWaterQualityRepository(waterRecords)
    val feeding = FakeFeedingRepository(feedings)
    val biometry = FakeBiometryRepository(biometries)
    val movements = FakeLotMovementRepository(movements)
    val overview = ObserveFarmOverview(this.tanks, this.lots, water, feeding, biometry, this.movements, clock)
}
