package com.valoes.valesaquagestor.data.sync

import com.valoes.valesaquagestor.domain.repository.PendingSyncRepository
import com.valoes.valesaquagestor.domain.sync.SyncStatus
import com.valoes.valesaquagestor.domain.sync.SyncStatusRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Junta os registros pendentes com o que a sincronização está fazendo. */
@Singleton
class SyncStatusRepositoryImpl @Inject constructor(
    pending: PendingSyncRepository,
    state: SyncStateHolder,
    private val backend: BackendInfo,
) : SyncStatusRepository {

    override val status: Flow<SyncStatus> = combine(pending.observePendingCount(), state.phase) { count, phase ->
        SyncStatus(pendingCount = count, phase = phase, backendConfigured = backend.isConfigured)
    }
}

/** Se o app foi compilado com um servidor (`SUPABASE_URL`); sem ele não há o que sincronizar. */
class BackendInfo(val isConfigured: Boolean)
