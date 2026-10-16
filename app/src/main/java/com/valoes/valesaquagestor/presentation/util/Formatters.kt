package com.valoes.valesaquagestor.presentation.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

/**
 * Lê um número digitado pelo usuário. Aceita vírgula ou ponto como separador decimal (10,5 ou
 * 10.5). Devolve `null` se não for um número finito.
 */
fun parseDecimal(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

/** Lê um número inteiro digitado pelo usuário, ou `null`. */
fun parseWholeNumber(text: String): Int? = text.trim().toIntOrNull()

/** Formata um número com [decimals] casas, no padrão brasileiro (vírgula decimal, ponto de milhar). */
fun formatNumber(value: Double, decimals: Int): String =
    NumberFormat.getInstance(ptBr).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }.format(value)

/** Data e hora curtas, como "16/10 06:10". */
fun formatDateTime(instant: Instant, zone: ZoneId): String =
    DateTimeFormatter.ofPattern("dd/MM HH:mm", ptBr).format(instant.atZone(zone))

/** Data completa, como "16/10/2026". */
fun formatDate(date: LocalDate): String = DateTimeFormatter.ofPattern("dd/MM/yyyy", ptBr).format(date)

/**
 * Formata um número para **preencher um campo de texto**: vírgula decimal e **sem** separador de
 * milhar, para o [parseDecimal] conseguir ler de volta ("1.234,5" não seria lido).
 */
fun formatInput(value: Double, decimals: Int): String =
    NumberFormat.getInstance(ptBr).apply {
        isGroupingUsed = false
        minimumFractionDigits = 0
        maximumFractionDigits = decimals
    }.format(value)
