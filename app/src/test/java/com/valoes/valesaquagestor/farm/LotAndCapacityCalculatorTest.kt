package com.valoes.valesaquagestor.farm

import com.valoes.valesaquagestor.domain.calc.CapacityCalculator
import com.valoes.valesaquagestor.domain.calc.LotCalculator
import com.valoes.valesaquagestor.domain.calc.RearingCondition
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import com.valoes.valesaquagestor.domain.model.TipoTanque
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LotAndCapacityCalculatorTest {

    // Lote de 1.000 peixes de 10 g povoado há 20 dias.
    private val lote = lot(quantidadeInicial = 1000, dataPovoamento = daysAgo(20)).copy(pesoMedioInicialG = 10.0)

    @Test
    fun `sem biometria nao ha TCA nem periodo`() {
        assertNull(LotCalculator.fcrWindow(lote, emptyList()))
        assertNull(LotCalculator.estimatedFcr(lote, emptyList(), emptyList(), listOf(feeding(1, at = daysAgo(5), kg = 50.0))))
    }

    @Test
    fun `com uma biometria a TCA vai do povoamento ate ela`() {
        // 100 peixes de 50 g na amostra; 1.000 peixes vivos: ganho = 1.000 x (50 - 10) / 1000 = 40 kg.
        val biometries = listOf(biometry(1, at = daysAgo(5), kg = 5.0, fish = 100))
        val feedings = listOf(
            feeding(1, at = daysAgo(15), kg = 30.0),
            feeding(2, at = daysAgo(8), kg = 30.0),
            feeding(3, at = daysAgo(2), kg = 99.0), // depois da biometria: fora do período
        )

        val fcr = LotCalculator.estimatedFcr(lote, emptyList(), biometries, feedings)

        assertEquals(1.5, fcr!!, 1e-9)
        assertEquals(lote.dataPovoamento to daysAgo(5), LotCalculator.fcrWindow(lote, biometries))
    }

    @Test
    fun `com duas biometrias a TCA vai da anterior ate a mais recente e usa os peixes vivos`() {
        // anterior: 50 g (10 dias atrás); atual: 120 g (2 dias atrás); 100 mortos há 6 dias -> 900 vivos.
        // ganho = 900 x (120 - 50) / 1000 = 63 kg; ração entre as duas = 94,5 kg -> TCA 1,5.
        val biometries = listOf(
            biometry(2, at = daysAgo(2), kg = 12.0, fish = 100),
            biometry(1, at = daysAgo(10), kg = 5.0, fish = 100),
        )
        val movements = listOf(movement(1, quantity = 100, at = daysAgo(6), tipo = TipoMovimentacao.MORTALIDADE))
        val feedings = listOf(
            feeding(1, at = daysAgo(12), kg = 500.0), // antes da biometria anterior: fora
            feeding(2, at = daysAgo(8), kg = 50.0),
            feeding(3, at = daysAgo(4), kg = 44.5),
            feeding(4, at = daysAgo(3), kg = 10.0, suspended = true), // suspenso: não conta
            feeding(5, at = daysAgo(1), kg = 500.0), // depois da biometria atual: fora
        )

        assertEquals(1.5, LotCalculator.estimatedFcr(lote, movements, biometries, feedings)!!, 1e-9)
    }

    @Test
    fun `sem racao no periodo ou sem ganho de peso nao ha TCA`() {
        val gained = listOf(biometry(1, at = daysAgo(5), kg = 5.0, fish = 100))
        val lost = listOf(biometry(1, at = daysAgo(5), kg = 0.5, fish = 100)) // 5 g: menos que os 10 g iniciais

        assertNull(LotCalculator.estimatedFcr(lote, emptyList(), gained, emptyList()))
        assertNull(LotCalculator.estimatedFcr(lote, emptyList(), lost, listOf(feeding(1, at = daysAgo(8), kg = 10.0))))
    }

    // ---- capacidade (D1 a D3 de specs/09 já estão testados; aqui a conferência do povoamento) ----

    @Test
    fun `o povoamento no limite da capacidade nao passa e um peixe a mais passa`() {
        // D1: 225 m2, condição 1, alevinagem (limite superior 120 kg/1.000 m2), 20 g -> 1.350 peixes.
        val tanque = tank(areaM2 = 225.0)

        val atLimit = CapacityCalculator.check(tanque, FaseLote.ALEVINAGEM, fishCount = 1350, finalWeightG = 20.0)!!
        val over = CapacityCalculator.check(tanque, FaseLote.ALEVINAGEM, fishCount = 1351, finalWeightG = 20.0)!!

        assertEquals(1350, atLimit.maxFish)
        assertEquals(27.0, atLimit.capacityKg, 1e-9)
        assertEquals(RearingCondition.NO_AERATOR_NO_EXCHANGE, atLimit.condition)
        assertEquals(100.0, atLimit.range.minKg, 0.0)
        assertFalse(atLimit.exceeds)
        assertTrue(over.exceeds)
    }

    @Test
    fun `sem quantidade informada ainda nao passa da capacidade`() {
        assertFalse(CapacityCalculator.check(tank(), FaseLote.ENGORDA, fishCount = 0, finalWeightG = 500.0)!!.exceeds)
    }

    @Test
    fun `capacidade so existe para viveiro e acude com area`() {
        assertNotNull(CapacityCalculator.check(tank(tipo = TipoTanque.ACUDE), FaseLote.RECRIA, 100, 300.0))
        assertNull(CapacityCalculator.check(tank(tipo = TipoTanque.REDONDO, areaM2 = null), FaseLote.RECRIA, 100, 300.0))
        assertNull(CapacityCalculator.check(tank(tipo = TipoTanque.TANQUE_REDE), FaseLote.RECRIA, 100, 300.0))
        assertNull(CapacityCalculator.check(tank(areaM2 = null), FaseLote.RECRIA, 100, 300.0))
    }

    @Test
    fun `aerador e troca de agua mudam a condicao e a capacidade`() {
        val aerated = tank().copy(usaAerador = true, renovacaoAguaPctDia = 5.0)

        val check = CapacityCalculator.check(aerated, FaseLote.ENGORDA, 100, 500.0)!!

        assertEquals(RearingCondition.AERATOR_WITH_EXCHANGE, check.condition)
        assertEquals(2000.0, check.capacityKg, 1e-9) // 2.000 kg/1.000 m2 x 1.000 m2
    }

    @Test
    fun `peso final tipico so existe para alevinagem e recria`() {
        assertEquals(20.0..30.0, CapacityCalculator.typicalFinalWeightG(FaseLote.ALEVINAGEM))
        assertEquals(300.0..400.0, CapacityCalculator.typicalFinalWeightG(FaseLote.RECRIA))
        assertNull(CapacityCalculator.typicalFinalWeightG(FaseLote.ENGORDA))
    }
}
