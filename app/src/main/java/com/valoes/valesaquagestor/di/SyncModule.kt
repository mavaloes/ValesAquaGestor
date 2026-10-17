package com.valoes.valesaquagestor.di

import com.valoes.valesaquagestor.BuildConfig
import com.valoes.valesaquagestor.data.local.database.AppDatabase
import com.valoes.valesaquagestor.data.supabase.AccessTokenProvider
import com.valoes.valesaquagestor.data.supabase.EncryptedTokenStore
import com.valoes.valesaquagestor.data.supabase.SupabaseAuthApi
import com.valoes.valesaquagestor.data.supabase.SupabaseConfig
import com.valoes.valesaquagestor.data.supabase.SupabaseRemoteSyncApi
import com.valoes.valesaquagestor.data.supabase.TokenStore
import com.valoes.valesaquagestor.data.sync.BackendInfo
import com.valoes.valesaquagestor.data.sync.DataStoreSyncCursorStore
import com.valoes.valesaquagestor.data.sync.OwnProfileRefresher
import com.valoes.valesaquagestor.data.sync.PeriodicSync
import com.valoes.valesaquagestor.data.sync.RemotePage
import com.valoes.valesaquagestor.data.sync.RemoteSyncApi
import com.valoes.valesaquagestor.data.sync.SessionProfileRefresher
import com.valoes.valesaquagestor.data.sync.SyncCursorStore
import com.valoes.valesaquagestor.data.sync.SyncException
import com.valoes.valesaquagestor.data.sync.SyncFailure
import com.valoes.valesaquagestor.data.sync.SyncStatusRepositoryImpl
import com.valoes.valesaquagestor.data.sync.SyncTable
import com.valoes.valesaquagestor.data.sync.WorkManagerPeriodicSync
import com.valoes.valesaquagestor.data.sync.WorkManagerSyncScheduler
import com.valoes.valesaquagestor.data.sync.buildSyncTables
import com.valoes.valesaquagestor.domain.sync.SyncScheduler
import com.valoes.valesaquagestor.domain.sync.SyncStatusRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient

/** Liga a sincronização às suas implementações (Fase 09). */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncBindingsModule {

    @Binds
    abstract fun bindTokenStore(impl: EncryptedTokenStore): TokenStore

    @Binds
    abstract fun bindCursorStore(impl: DataStoreSyncCursorStore): SyncCursorStore

    @Binds
    abstract fun bindProfileRefresher(impl: SessionProfileRefresher): OwnProfileRefresher

    @Binds
    abstract fun bindScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    @Binds
    abstract fun bindPeriodicSync(impl: WorkManagerPeriodicSync): PeriodicSync

    @Binds
    abstract fun bindSyncStatus(impl: SyncStatusRepositoryImpl): SyncStatusRepository
}

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    /** Tempos de espera curtos: com sinal fraco é melhor falhar e tentar de novo do que travar a rodada. */
    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideSupabaseConfig(): SupabaseConfig = SupabaseConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)

    @Provides
    @Singleton
    fun provideBackendInfo(config: SupabaseConfig): BackendInfo = BackendInfo(config.isConfigured)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideSupabaseAuthApi(config: SupabaseConfig, client: OkHttpClient, clock: Clock): SupabaseAuthApi =
        SupabaseAuthApi(config, client, clock)

    @Provides
    @Singleton
    fun provideRemoteSyncApi(config: SupabaseConfig, client: OkHttpClient, tokens: AccessTokenProvider): RemoteSyncApi =
        if (config.isConfigured) SupabaseRemoteSyncApi(config, client, tokens) else NoBackendSyncApi

    @Provides
    @Singleton
    fun provideSyncTables(database: AppDatabase): List<SyncTable> = buildSyncTables(database)
}

/** Sem servidor configurado (build com login de mentira) não há para onde sincronizar. */
private object NoBackendSyncApi : RemoteSyncApi {
    override suspend fun push(table: String, rows: List<JsonObject>) =
        throw SyncException(SyncFailure.SERVER, "Servidor não configurado.")

    override suspend fun pull(table: String, after: String?, limit: Int): RemotePage =
        throw SyncException(SyncFailure.SERVER, "Servidor não configurado.")
}
