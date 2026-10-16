package com.valoes.valesaquagestor.data.sync

import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.PendingSyncRepository
import com.valoes.valesaquagestor.domain.sync.SyncScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Decide quando pedir sincronização, sem nenhuma tela precisar lembrar disso:
 *
 * - ao entrar na conta (baixa o que os outros aparelhos fizeram);
 * - sempre que a quantidade de registros pendentes muda e ainda há pendentes (novo registro ou envio parcial).
 *
 * Sem sessão não há o que sincronizar. Sem servidor configurado, não faz nada.
 */
@Singleton
class SyncCoordinator @Inject constructor(
    private val auth: AuthRepository,
    private val pending: PendingSyncRepository,
    private val scheduler: SyncScheduler,
    private val periodic: PeriodicSync,
    private val backend: BackendInfo,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        if (!backend.isConfigured) return
        scope.launch {
            var wasSignedIn = false
            combine(auth.currentSession.map { it != null }.distinctUntilChanged(), pending.observePendingCount()) { signedIn, count ->
                signedIn to count
            }.distinctUntilChanged().collect { (signedIn, count) ->
                if (!signedIn) {
                    periodic.cancel()
                } else {
                    if (!wasSignedIn) periodic.schedule()
                    if (!wasSignedIn || count > 0) scheduler.requestSync()
                }
                wasSignedIn = signedIn
            }
        }
    }
}

/** Download periódico das mudanças dos outros aparelhos. */
interface PeriodicSync {
    fun schedule()
    fun cancel()
}

@Singleton
class WorkManagerPeriodicSync @Inject constructor(
    private val scheduler: WorkManagerSyncScheduler,
) : PeriodicSync {
    override fun schedule() = scheduler.schedulePeriodic()
    override fun cancel() = scheduler.cancelPeriodic()
}
