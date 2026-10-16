package com.valoes.valesaquagestor.data.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.valoes.valesaquagestor.domain.repository.OnboardingRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * "Primeiro acesso concluído" no DataStore do aparelho. Vale para o aparelho, não para a conta:
 * quem entrar com outra conta no mesmo aparelho não vê o tutorial de novo.
 */
@Singleton
class DataStoreOnboardingRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : OnboardingRepository {

    override val isCompleted: Flow<Boolean> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences -> preferences[KEY] ?: false }
        .distinctUntilChanged()

    override suspend fun markCompleted() {
        dataStore.edit { it[KEY] = true }
    }

    private companion object {
        val KEY = booleanPreferencesKey("onboarding_completed")
    }
}
