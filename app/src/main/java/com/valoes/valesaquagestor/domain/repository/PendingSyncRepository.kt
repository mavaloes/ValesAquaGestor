package com.valoes.valesaquagestor.domain.repository

import kotlinx.coroutines.flow.Flow

/** Quantos registros deste aparelho ainda não foram enviados ao servidor (`sincronizado = false`). */
interface PendingSyncRepository {

    /** Soma de tanques, lotes, medições, rações, biometrias, movimentações e usuários pendentes. */
    fun observePendingCount(): Flow<Int>
}
