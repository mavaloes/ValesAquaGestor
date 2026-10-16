package com.valoes.valesaquagestor.data.local.database

import androidx.room.withTransaction
import javax.inject.Inject

/**
 * Executa um bloco numa única transação do banco: ou tudo é gravado, ou nada.
 * Existe como interface para os repositórios não dependerem diretamente do [AppDatabase].
 */
interface TransactionRunner {
    suspend fun <R> run(block: suspend () -> R): R
}

/** Implementação com a transação do Room. */
class RoomTransactionRunner @Inject constructor(
    private val database: AppDatabase,
) : TransactionRunner {
    override suspend fun <R> run(block: suspend () -> R): R = database.withTransaction { block() }
}
