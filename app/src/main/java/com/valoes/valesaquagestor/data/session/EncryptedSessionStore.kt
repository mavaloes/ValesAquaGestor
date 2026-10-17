package com.valoes.valesaquagestor.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.Session
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Sessão no DataStore, **criptografada** pelo [SessionCipher]. No disco só existe o texto cifrado.
 * A senha nunca passa por aqui: [Session] não tem esse campo.
 */
@Singleton
class EncryptedSessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cipher: SessionCipher,
) : SessionStore {

    @Serializable
    private class SessionRecord(val userId: String, val nome: String, val email: String, val papel: String)

    override val session: Flow<Session?> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences -> preferences[KEY]?.let(::decode) }
        .distinctUntilChanged()

    override suspend fun save(session: Session) {
        val record = SessionRecord(session.userId, session.nome, session.email, session.papel.name)
        val encrypted = cipher.encrypt(json.encodeToString(SessionRecord.serializer(), record))
        dataStore.edit { it[KEY] = encrypted }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(KEY) }
    }

    /** Qualquer falha ao ler (chave perdida, texto alterado) vale como "sem sessão": pede novo login. */
    private fun decode(stored: String): Session? = try {
        val record = json.decodeFromString(SessionRecord.serializer(), cipher.decrypt(stored))
        Session(record.userId, record.nome, record.email, Papel.valueOf(record.papel))
    } catch (_: Exception) {
        null
    }

    private companion object {
        val KEY = stringPreferencesKey("session")
        val json = Json { ignoreUnknownKeys = true }
    }
}
