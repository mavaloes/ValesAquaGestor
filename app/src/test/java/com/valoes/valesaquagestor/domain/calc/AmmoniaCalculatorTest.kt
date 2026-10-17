package com.valoes.valesaquagestor.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Test

/** Casos A1 a A3 de `specs/09-casos-de-teste.md`. */
class AmmoniaCalculatorTest {

    @Test
    fun `A1 pH 7 e 30 graus`() {
        assertEquals(0.01185, AmmoniaCalculator.toxicMgL(1.5, 7.0, 30.0), 0.0002)
    }

    @Test
    fun `A2 pH 9 e 30 graus`() {
        assertEquals(0.6642, AmmoniaCalculator.toxicMgL(1.5, 9.0, 30.0), 0.005)
    }

    /** Tabela F13 p.37: % de NH₃ na amônia total. Linhas = pH; colunas = 24, 26, 28, 30, 32 °C. */
    @Test
    fun `A3 formula confere com todos os nos da tabela da F13`() {
        val temperatures = listOf(24.0, 26.0, 28.0, 30.0, 32.0)
        val table = mapOf(
            6.5 to listOf(0.17, 0.19, 0.22, 0.25, 0.29),
            7.0 to listOf(0.52, 0.60, 0.69, 0.79, 0.90),
            7.5 to listOf(1.63, 1.87, 2.14, 2.45, 2.80),
            8.0 to listOf(4.97, 5.68, 6.48, 7.36, 8.35),
            8.5 to listOf(14.18, 16.00, 17.96, 20.08, 22.36),
            9.0 to listOf(34.32, 37.58, 40.91, 44.28, 47.66),
            9.5 to listOf(62.30, 65.56, 68.65, 71.53, 74.22),
            10.0 to listOf(83.94, 85.76, 87.38, 88.82, 90.10),
        )
        table.forEach { (ph, fractions) ->
            fractions.forEachIndexed { index, expectedPct ->
                val actual = AmmoniaCalculator.nh3FractionPct(ph, temperatures[index])
                assertEquals("pH $ph, ${temperatures[index]} °C", expectedPct, actual, 0.3)
            }
        }
    }

    @Test
    fun `amonia total zero da toxica zero`() {
        assertEquals(0.0, AmmoniaCalculator.toxicMgL(0.0, 8.0, 28.0), 0.0)
    }
}
