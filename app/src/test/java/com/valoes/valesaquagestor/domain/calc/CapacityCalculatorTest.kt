package com.valoes.valesaquagestor.domain.calc

import com.valoes.valesaquagestor.domain.model.FaseLote
import org.junit.Assert.assertEquals
import org.junit.Test

/** Casos D1 a D3 de `specs/09-casos-de-teste.md`. */
class CapacityCalculatorTest {

    private val condition1 = RearingCondition.NO_AERATOR_NO_EXCHANGE

    @Test
    fun `D1 alevinagem em 225 m2`() {
        val capacity = CapacityCalculator.capacityKg(225.0, condition1, FaseLote.ALEVINAGEM)
        assertEquals(27.0, capacity, 1e-6)
        assertEquals(1350, CapacityCalculator.maxFish(capacity, finalWeightG = 20.0))
    }

    @Test
    fun `D2 recria em 1500 m2`() {
        val capacity = CapacityCalculator.capacityKg(1500.0, condition1, FaseLote.RECRIA)
        assertEquals(375.0, capacity, 1e-6)
        assertEquals(1250, CapacityCalculator.maxFish(capacity, finalWeightG = 300.0))
    }

    @Test
    fun `D3 engorda em 4025 m2 arredonda para baixo`() {
        val capacity = CapacityCalculator.capacityKg(4025.0, condition1, FaseLote.ENGORDA)
        assertEquals(2415.0, capacity, 1e-6)
        assertEquals(1207, CapacityCalculator.maxFish(capacity, finalWeightG = 2000.0))
    }

    @Test
    fun `limite inferior e mais conservador`() {
        assertEquals(22.5, CapacityCalculator.capacityKg(225.0, condition1, FaseLote.ALEVINAGEM, useUpperLimit = false), 1e-6)
    }

    @Test
    fun `condicao de criacao deriva de aerador e troca de agua`() {
        assertEquals(RearingCondition.NO_AERATOR_NO_EXCHANGE, CapacityCalculator.condition(false, 4.9))
        assertEquals(RearingCondition.AERATOR_NO_EXCHANGE, CapacityCalculator.condition(true, 0.0))
        assertEquals(RearingCondition.NO_AERATOR_WITH_EXCHANGE, CapacityCalculator.condition(false, 5.0))
        assertEquals(RearingCondition.AERATOR_WITH_EXCHANGE, CapacityCalculator.condition(true, 10.0))
    }

    @Test
    fun `tabela da condicao 4 na engorda`() {
        val range = CapacityCalculator.productivityRange(RearingCondition.AERATOR_WITH_EXCHANGE, FaseLote.ENGORDA)
        assertEquals(1000.0, range.minKg, 0.0)
        assertEquals(2000.0, range.maxKg, 0.0)
    }
}
