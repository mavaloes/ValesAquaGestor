package com.valoes.valesaquagestor.data.supabase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.valoes.valesaquagestor.data.session.SessionCipher
import com.valoes.valesaquagestor.data.session.SessionStore
import com.valoes.valesaquagestor.data.sync.SyncException
import com.valoes.valesaquagestor.data.sync.SyncFailure
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Guarda as chaves de acesso ao servidor, criptografadas. Nunca guarda a senha. */
interface TokenStore {
    suspend fun get(): AuthTokens?
    suspend fun save(tokens: AuthTokens)
    suspend fun clear()
}

/** [TokenStore] no DataStore do aparelho, cifrado pelo mesmo [SessionCipher] da sessão. */
@Singleton
class EncryptedTokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cipher: SessionCipher,
) : TokenStore {

    @Serializable
    private class Record(val accessToken: String, val refreshToken: String, val expiresAtEpochSeconds: Long)

    override suspend fun get(): AuthTokens? {
        val stored = dataStore.data.first()[KEY] ?: return null
        // Qualquer falha ao ler (chave perdida, texto alterado) vale como "sem chaves": pede novo login.
        return try {
            val record = json.decodeFromString(Record.serializer(), cipher.decrypt(stored))
            AuthTokens(record.accessToken, record.refreshToken, record.expiresAtEpochSeconds)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun save(tokens: AuthTokens) {
        val record = Record(tokens.accessToken, tokens.refreshToken, tokens.expiresAtEpochSeconds)
        val encrypted = cipher.encrypt(json.encodeToString(Record.serializer(), record))
        dataStore.edit { it[KEY] = encrypted }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("auth_tokens")
        val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * Entrega a chave de acesso válida, renovando-a quando está perto de vencer ou quando o servidor a
 * recusa. Se a renovação for recusada de vez, a sessão acabou: apaga as chaves e a sessão (**os dados
 * locais ficam**) e a navegação leva ao Login (`specs/11-autenticacao.md`, "Sessão expirada").
 * Sem internet, não derruba a sessão: só falha por [SyncFailure.OFFLINE].
 */
@Singleton
class AccessTokenProvider @Inject constructor(
    private val tokens: TokenStore,
    private val api: SupabaseAuthApi,
    private val sessionStore: SessionStore,
    private val clock: Clock,
) {
    private val lock = Mutex()

    /** @param forceRefresh `true` quando o servidor acabou de recusar a chave atual. */
    suspend fun accessToken(forceRefresh: Boolean = false): String = lock.withLock {
        val current = tokens.get() ?: throw SyncException(
            SyncFailure.AUTH, "Sem sessão.",
        )
        val expiring = current.expiresAtEpochSeconds - clock.instant().epochSecond < REFRESH_MARGIN_SECONDS
        if (!forceRefresh && !expiring) return@withLock current.accessToken

        when (val result = api.refresh(current.refreshToken)) {
            is TokenResult.Ok -> {
                tokens.save(result.tokens)
                result.tokens.accessToken
            }

            TokenResult.Offline -> throw SyncException(SyncFailure.OFFLINE)
            TokenResult.InvalidCredentials -> {
                tokens.clear()
                sessionStore.clear()
                throw SyncException(SyncFailure.AUTH, "Sessão expirada.")
            }

            else -> throw SyncException(SyncFailure.SERVER)
        }
    }

    private companion object {
        /** Renova um pouco antes de vencer, para a chamada não chegar com a chave vencida (margem de engenharia). */
        const val REFRESH_MARGIN_SECONDS = 60L
    }
}
