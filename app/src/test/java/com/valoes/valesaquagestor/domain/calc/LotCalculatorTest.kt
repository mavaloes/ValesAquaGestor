package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.Lote
import com.valoes.valesaquagestor.domain.model.MovimentacaoLote
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class LotCalculatorTest {

    private val lote = Lote(
        tanqueId = 1, especie = Especie.TILAPIA, fase = FaseLote.RECRIA, quantidadeInicial = 1000,
        pesoMedioInicialG = 10.0, dataPovoamento = Instant.EPOCH, pesoMedioAtualG = 250.0,
    )

    private fun movement(tipo: TipoMovimentacao, quantity: Int) =
        MovimentacaoLote(loteId = 1, dataHora = Instant.EPOCH, tipo = tipo, quantidade = quantity)

    @Test
    fun `sem movimentacoes a quantidade atual e a inicial`() {
        assertEquals(1000, LotCalculator.currentQuantity(lote, emptyList()))
        assertEquals(0.0, LotCalculator.accumulatedMortalityPct(lote, emptyList()), 0.0)
    }

    @Test
    fun `quantidade atual desconta todos os tipos de saida`() {
        val movements = listOf(
            movement(TipoMovimentacao.MORTALIDADE, 40),
            movement(TipoMovimentacao.DESPESCA_PARCIAL, 100),
            movement(TipoMovimentacao.TRANSFERENCIA, 60),
            movement(TipoMovimentacao.DESPESCA_FINAL, 800),
        )
        assertEquals(0, LotCalculator.currentQuantity(lote, movements))
    }

    @Test
    fun `biomassa usa a quantidade atual e o peso medio atual`() {
        val movements = listOf(movement(TipoMovimentacao.MORTALIDADE, 200))
        assertEquals(200.0, LotCalculator.biomassKg(lote, movements), 1e-9)
    }

    @Test
    fun `mortalidade acumulada conta so mortes`() {
        val movements = listOf(
            movement(TipoMovimentacao.MORTALIDADE, 101),
            movement(TipoMovimentacao.DESPESCA_PARCIAL, 300),
        )
        assertEquals(101, LotCalculator.deadCount(movements))
        assertEquals(10.1, LotCalculator.accumulatedMortalityPct(lote, movements), 1e-9)
    }
}
