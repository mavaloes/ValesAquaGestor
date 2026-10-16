package com.valoes.valesaquagestor.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.valoes.valesaquagestor.BuildConfig
import com.valoes.valesaquagestor.data.auth.FakeAuthRepository
import com.valoes.valesaquagestor.data.connectivity.AndroidConnectivityChecker
import com.valoes.valesaquagestor.data.session.EncryptedSessionStore
import com.valoes.valesaquagestor.data.session.KeystoreSessionCipher
import com.valoes.valesaquagestor.data.session.SessionCipher
import com.valoes.valesaquagestor.data.session.SessionStore
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.ConnectivityChecker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/** Liga sessão e verificação de conexão às suas implementações. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindingsModule {

    @Binds
    abstract fun bindSessionStore(impl: EncryptedSessionStore): SessionStore

    @Binds
    abstract fun bindSessionCipher(impl: KeystoreSessionCipher): SessionCipher

    @Binds
    abstract fun bindConnectivityChecker(impl: AndroidConnectivityChecker): ConnectivityChecker
}

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    /** Preferências do app (sessão criptografada agora; "onboarding concluído" na Fase 07). */
    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("app_preferences") }

    /** Escolhe a implementação pela flag de build `USE_FAKE_AUTH`. */
    @Provides
    @Singleton
    fun provideAuthRepository(fake: Provider<FakeAuthRepository>): AuthRepository {
        check(BuildConfig.USE_FAKE_AUTH) { "Backend de autenticação ainda não configurado (Fase 09)." }
        return fake.get()
    }
}
