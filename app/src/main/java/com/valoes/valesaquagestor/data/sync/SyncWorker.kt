package com.valoes.valesaquagestor.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.valoes.valesaquagestor.domain.sync.SyncScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface SyncEntryPoint {
    fun syncEngine(): SyncEngine
}

/**
 * Roda uma rodada de sincronização em segundo plano. O WorkManager só a inicia com conexão e, se ela
 * falhar por rede ou servidor, tenta de novo com espera crescente.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val engine = EntryPointAccessors.fromApplication(applicationContext, SyncEntryPoint::class.java).syncEngine()
        return when (engine.run()) {
            SyncOutcome.SUCCESS -> Result.success()
            SyncOutcome.RETRY -> Result.retry()
            // Sem sessão não há o que tentar de novo; depois do login uma nova rodada é pedida.
            SyncOutcome.AUTH_EXPIRED -> Result.success()
        }
    }
}

/** [SyncScheduler] com o WorkManager. */
@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : SyncScheduler {

    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /**
     * Uma rodada logo que houver conexão. Pedidos que chegam durante uma rodada esperam ela terminar
     * (`APPEND_OR_REPLACE`), então um registro salvo no meio do envio não fica esquecido.
     */
    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_TIME_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    /** Baixa as mudanças dos outros aparelhos de tempos em tempos, mesmo sem nada para enviar. */
    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelPeriodic() {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_NAME)
    }

    private companion object {
        const val ONE_TIME_NAME = "sync-now"
        const val PERIODIC_NAME = "sync-periodic"

        /** Primeira espera depois de uma falha; dobra a cada tentativa (o mínimo do WorkManager é 10 s). */
        const val BACKOFF_SECONDS = 30L

        /** Menor intervalo que o WorkManager aceita para tarefa periódica. */
        const val PERIODIC_MINUTES = 15L
    }
}
