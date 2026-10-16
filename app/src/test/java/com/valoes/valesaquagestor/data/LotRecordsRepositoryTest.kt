package com.valoes.valesaquagestor.data

import android.database.sqlite.SQLiteConstraintException
import com.valoes.valesaquagestor.domain.calc.LotCalculator
import com.valoes.valesaquagestor.domain.model.Biometria
import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import com.valoes.valesaquagestor.domain.model.StatusLote
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LotRecordsRepositoryTest {

    private lateinit var env: TestEnvironment

    @Before
    fun setUp() {
        env = TestEnvironment()
    }

    @After
    fun tearDown() = env.close()

    private fun biometry(lotId: Long, at: Long, kg: Double, fish: Int) = Biometria(
        loteId = lotId, dataHora = Instant.ofEpochMilli(at), pesoAmostraKg = kg, numeroPeixesAmostra = fish,
    )

    private fun movement(lotId: Long, tipo: TipoMovimentacao, quantity: Int, at: Long = 2_000L) = MovimentacaoLote(
        loteId = lotId, dataHora = Instant.ofEpochMilli(at), tipo = tipo, quantidade = quantity,
    )

    private suspend fun lotWithTank(quantity: Int = 1000, weightG: Double = 20.0): Long =
        env.lotes.save(env.newLot(env.tanques.save(env.newTank()), quantity, weightG))

    @Test
    fun `salvar biometria atualiza o peso medio do lote na mesma transacao`() = runBlocking {
        val lot = lotWithTank()
        env.clock.now = 8_000L

        env.biometrias.save(biometry(lot, at = 3_000L, kg = 2.5, fish = 50))

        val updated = env.lotes.get(lot)!!
        assertEquals(50.0, updated.pesoMedioAtualG, 1e-9)
        val row = env.db.loteDao().getByIdIncludingDeleted(lot)!!
        assertEquals(8_000L, row.atualizadoEm)
        assertFalse("o lote alterado precisa ser reenviado", row.sincronizado)
    }

    @Test
    fun `biometria com data antiga nao sobrescreve o peso de uma pesagem mais nova`() = runBlocking {
        val lot = lotWithTank()
        env.biometrias.save(biometry(lot, at = 5_000L, kg = 5.0, fish = 50))
        env.biometrias.save(biometry(lot, at = 1_000L, kg = 1.0, fish = 50))

        assertEquals(100.0, env.lotes.get(lot)!!.pesoMedioAtualG, 1e-9)
        assertEquals(2, env.biometrias.observeLatest(lot, 10).first().size)
    }

    @Test
    fun `ultimas biometrias vem da mais nova para a mais antiga, com limite`() = runBlocking {
        val lot = lotWithTank()
        env.biometrias.save(biometry(lot, 1_000L, 1.0, 10))
        env.biometrias.save(biometry(lot, 3_000L, 3.0, 10))
        env.biometrias.save(biometry(lot, 2_000L, 2.0, 10))
        val removed = env.biometrias.save(biometry(lot, 4_000L, 4.0, 10))
        env.biometrias.delete(removed)

        val latest = env.biometrias.observeLatest(lot, 2).first()

        assertEquals(listOf(Instant.ofEpochMilli(3_000L), Instant.ofEpochMilli(2_000L)), latest.map { it.dataHora })
    }

    @Test
    fun `biometria invalida e recusada e nao altera o lote`() = runBlocking {
        val lot = lotWithTank()
        for (bad in listOf(biometry(lot, 1_000L, 2.0, 0), biometry(lot, 1_000L, 0.0, 10))) {
            try {
                env.biometrias.save(bad)
                fail("deveria recusar")
            } catch (expected: IllegalArgumentException) {
                // esperado
            }
        }
        assertEquals(20.0, env.lotes.get(lot)!!.pesoMedioAtualG, 0.0)
    }

    @Test
    fun `quantidade atual depois de mortes, despesca parcial e transferencia`() = runBlocking {
        val lot = lotWithTank(quantity = 1000)
        env.movimentacoes.save(movement(lot, TipoMovimentacao.MORTALIDADE, 30))
        env.movimentacoes.save(movement(lot, TipoMovimentacao.MORTALIDADE, 20))
        env.movimentacoes.save(movement(lot, TipoMovimentacao.DESPESCA_PARCIAL, 200))
        env.movimentacoes.save(movement(lot, TipoMovimentacao.TRANSFERENCIA, 50))
        val removed = env.movimentacoes.save(movement(lot, TipoMovimentacao.MORTALIDADE, 500))
        env.movimentacoes.delete(removed)

        val movements = env.movimentacoes.observeByLot(lot).first()
        val loteModel = env.lotes.get(lot)!!

        assertEquals(700, LotCalculator.currentQuantity(loteModel, movements))
        assertEquals(5.0, LotCalculator.accumulatedMortalityPct(loteModel, movements), 1e-9)
        assertEquals(StatusLote.ATIVO, loteModel.status)
    }

    @Test
    fun `despesca final encerra o lote na mesma transacao`() = runBlocking {
        val lot = lotWithTank(quantity = 100)
        env.clock.now = 9_000L

        env.movimentacoes.save(movement(lot, TipoMovimentacao.DESPESCA_FINAL, 100, at = 6_000L))

        val closed = env.lotes.get(lot)!!
        assertEquals(StatusLote.ENCERRADO, closed.status)
        assertEquals(Instant.ofEpochMilli(6_000L), closed.dataEncerramento)
        assertEquals(0, LotCalculator.currentQuantity(closed, env.movimentacoes.observeByLot(lot).first()))
        assertTrue(env.lotes.observeAllActive().first().isEmpty())
    }

    @Test
    fun `despesca parcial e mortalidade nao encerram o lote`() = runBlocking {
        val lot = lotWithTank()
        env.movimentacoes.save(movement(lot, TipoMovimentacao.DESPESCA_PARCIAL, 10))
        env.movimentacoes.save(movement(lot, TipoMovimentacao.MORTALIDADE, 10))
        assertEquals(StatusLote.ATIVO, env.lotes.get(lot)!!.status)
        assertNull(env.lotes.get(lot)!!.dataEncerramento)
    }

    @Test
    fun `movimentacao de lote inexistente falha e nada e gravado`() = runBlocking {
        try {
            env.movimentacoes.save(movement(lotId = 999, tipo = TipoMovimentacao.DESPESCA_FINAL, quantity = 5))
            fail("deveria recusar")
        } catch (expected: SQLiteConstraintException) {
            // FK
        }
        assertTrue(env.db.movimentacaoLoteDao().getPending().isEmpty())
    }

    @Test
    fun `movimentacao sem quantidade e recusada`() = runBlocking {
        val lot = lotWithTank()
        try {
            env.movimentacoes.save(movement(lot, TipoMovimentacao.MORTALIDADE, 0))
            fail("deveria recusar")
        } catch (expected: IllegalArgumentException) {
            // esperado
        }
    }
}
