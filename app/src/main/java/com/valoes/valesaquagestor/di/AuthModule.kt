package com.valoes.valesaquagestor.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.valoes.valesaquagestor.BuildConfig
import com.valoes.valesaquagestor.data.auth.FakeAuthRepository
import com.valoes.valesaquagestor.data.connectivity.AndroidConnectivityChecker
import com.valoes.valesaquagestor.data.onboarding.DataStoreOnboardingRepository
import com.valoes.valesaquagestor.data.session.EncryptedSessionStore
import com.valoes.valesaquagestor.data.session.KeystoreSessionCipher
import com.valoes.valesaquagestor.data.session.SessionCipher
import com.valoes.valesaquagestor.data.session.SessionCurrentUserProvider
import com.valoes.valesaquagestor.data.session.SessionStore
import com.valoes.valesaquagestor.data.supabase.SupabaseAuthRepository
import com.valoes.valesaquagestor.data.settings.DataStoreSettingsRepository
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.ConnectivityChecker
import com.valoes.valesaquagestor.domain.repository.CurrentUserProvider
import com.valoes.valesaquagestor.domain.repository.OnboardingRepository
import com.valoes.valesaquagestor.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/** Liga sessão, verificação de conexão, primeiro acesso, usuário atual e configurações às suas implementações. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindingsModule {

    @Binds
    abstract fun bindSessionStore(impl: EncryptedSessionStore): SessionStore

    @Binds
    abstract fun bindSessionCipher(impl: KeystoreSessionCipher): SessionCipher

    @Binds
    abstract fun bindConnectivityChecker(impl: AndroidConnectivityChecker): ConnectivityChecker

    @Binds
    abstract fun bindOnboardingRepository(impl: DataStoreOnboardingRepository): OnboardingRepository

    @Binds
    abstract fun bindCurrentUserProvider(impl: SessionCurrentUserProvider): CurrentUserProvider

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    /** Preferências do app (sessão criptografada agora; "onboarding concluído" na Fase 07). */
    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("app_preferences") }

    /**
     * Escolhe a implementação pela flag de build `USE_FAKE_AUTH`: sem `supabase.url` e `supabase.anonKey`
     * em `local.properties`, usa o login de mentira; com eles, o Supabase de verdade.
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        fake: Provider<FakeAuthRepository>,
        supabase: Provider<SupabaseAuthRepository>,
    ): AuthRepository = if (BuildConfig.USE_FAKE_AUTH) fake.get() else supabase.get()
}
