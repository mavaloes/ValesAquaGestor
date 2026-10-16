package com.valoes.valesaquagestor.domain.sync

import kotlinx.coroutines.flow.Flow

/** O que a sincronização está fazendo agora. */
enum class SyncPhase {
    /** Parada: ou terminou bem, ou ainda não rodou. */
    IDLE,

    /** Enviando e baixando registros. */
    RUNNING,

    /** A última tentativa não terminou (sem conexão ou erro do servidor); o app tenta de novo sozinho. */
    FAILED,
}

/**
 * Estado da sincronização para o indicador discreto da tela (`specs/tasks/fase-09`).
 *
 * @property pendingCount registros deste aparelho que ainda não chegaram ao servidor.
 * @property backendConfigured `false` quando o app está sem servidor (build de teste com login de mentira):
 *   não há o que sincronizar e o indicador não aparece.
 */
data class SyncStatus(
    val pendingCount: Int,
    val phase: SyncPhase,
    val backendConfigured: Boolean = true,
)

/** Estado da sincronização, sempre disponível offline. */
interface SyncStatusRepository {
    val status: Flow<SyncStatus>
}

/** Pede uma sincronização. Não precisa de internet no momento do pedido: roda quando a conexão voltar. */
fun interface SyncScheduler {
    fun requestSync()
}
