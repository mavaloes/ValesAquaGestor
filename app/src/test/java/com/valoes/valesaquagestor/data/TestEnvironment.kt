package com.valoes.valesaquagestor.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.valoes.valesaquagestor.data.local.database.AppDatabase
import com.valoes.valesaquagestor.data.local.database.RoomTransactionRunner
import com.valoes.valesaquagestor.data.repository.BiometryRepositoryImpl
import com.valoes.valesaquagestor.data.repository.FeedingRepositoryImpl
import com.valoes.valesaquagestor.data.repository.LotMovementRepositoryImpl
import com.valoes.valesaquagestor.data.repository.LotRepositoryImpl
import com.valoes.valesaquagestor.data.repository.PendingSyncRepositoryImpl
import com.valoes.valesaquagestor.data.repository.TankRepositoryImpl
import com.valoes.valesaquagestor.data.repository.UserRepositoryImpl
import com.valoes.valesaquagestor.data.repository.WaterQualityRepositoryImpl
import com.valoes.valesaquagestor.domain.model.ComportamentoPeixe
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.model.Tanque
import com.valoes.valesaquagestor.domain.model.TipoTanque
import com.valoes.valesaquagestor.domain.repository.CurrentUserProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Relógio controlado pelo teste: `now` muda quando o teste quer. */
class MutableClock(var now: Long = 1_000_000L) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = Instant.ofEpochMilli(now)
}

/**
 * Banco Room **em memória** com os repositórios reais, para rodar no `testDebugUnitTest` via
 * Robolectric (sem emulador). Chame [close] no `@After`.
 */
class TestEnvironment {
    val clock = MutableClock()
    val db: AppDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    private val transaction = RoomTransactionRunner(db)

    /** Quem está logado: o teste troca para conferir `criadoPorUserId`. */
    var currentUserId: String? = null
    private val currentUser = CurrentUserProvider { currentUserId }
    val tanques = TankRepositoryImpl(db.tanqueDao(), transaction, clock)
    val lotes = LotRepositoryImpl(db.loteDao(), transaction, clock)
    val agua = WaterQualityRepositoryImpl(db.qualidadeAguaDao(), transaction, clock, currentUser)
    val arracoamentos = FeedingRepositoryImpl(db.arracoamentoDao(), transaction, clock, currentUser)
    val biometrias = BiometryRepositoryImpl(db.biometriaDao(), db.loteDao(), transaction, clock, currentUser)
    val movimentacoes = LotMovementRepositoryImpl(db.movimentacaoLoteDao(), db.loteDao(), transaction, clock, currentUser)
    val usuarios = UserRepositoryImpl(db.usuarioDao(), transaction, clock)
    val pendentes = PendingSyncRepositoryImpl(
        db.tanqueDao(), db.loteDao(), db.qualidadeAguaDao(), db.arracoamentoDao(),
        db.biometriaDao(), db.movimentacaoLoteDao(), db.usuarioDao(),
    )

    fun close() = db.close()

    /** Tanque escavado válido, pronto para salvar. */
    fun newTank(nome: String = "Viveiro 1") = Tanque(
        nome = nome, tipo = TipoTanque.ESCAVADO, areaM2 = 1000.0,
        usaAerador = false, renovacaoAguaPctDia = 0.0,
    )

    /** Lote de tilápia válido num tanque, pronto para salvar. */
    fun newLot(tanqueId: Long, quantidadeInicial: Int = 1000, pesoMedioG: Double = 20.0) = Lote(
        tanqueId = tanqueId, especie = Especie.TILAPIA, fase = FaseLote.ENGORDA,
        quantidadeInicial = quantidadeInicial, pesoMedioInicialG = pesoMedioG,
        dataPovoamento = Instant.ofEpochMilli(500_000L), pesoMedioAtualG = pesoMedioG,
    )

    companion object {
        val behavior = ComportamentoPeixe.NORMAL
    }
}
