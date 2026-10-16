package com.valoes.valesaquagestor.data.supabase

import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.Session
import java.io.IOException
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Endereço e chave pública do projeto Supabase. A chave pública (`anon`) pode ir no app; o acesso é decidido pelas regras do banco. */
data class SupabaseConfig(val url: String, val anonKey: String) {
    val isConfigured: Boolean get() = url.isNotBlank() && anonKey.isNotBlank()
}

/** Chaves de acesso da sessão no servidor. Guardadas só criptografadas (ver [TokenStore]). */
data class AuthTokens(val accessToken: String, val refreshToken: String, val expiresAtEpochSeconds: Long)

/** Resposta do servidor de login. A tela nunca vê o texto cru: só estes casos. */
sealed interface TokenResult {
    data class Ok(val tokens: AuthTokens, val authUserId: String) : TokenResult
    data object InvalidCredentials : TokenResult
    data object EmailTaken : TokenResult
    data object TooManyAttempts : TokenResult
    data object Offline : TokenResult
    data object Unexpected : TokenResult
}

/** Resposta da consulta do perfil de quem entrou. */
sealed interface ProfileResult {
    data class Found(val session: Session) : ProfileResult

    /** O usuário não tem acesso a nenhuma fazenda (ainda não criado ou desativado). */
    data object NoAccess : ProfileResult
    data object Offline : ProfileResult
    data object Unexpected : ProfileResult
}

/**
 * Login por e-mail e senha no Supabase Auth (GoTrue) e consulta do perfil. Só traduz HTTP: as regras
 * do app (mensagens genéricas, sessão salva criptografada) ficam no [SupabaseAuthRepository].
 * A senha só passa daqui para o servidor; nunca é guardada nem registrada.
 */
class SupabaseAuthApi(
    private val config: SupabaseConfig,
    private val client: OkHttpClient,
    private val clock: Clock,
) {

    suspend fun signIn(email: String, password: String): TokenResult = tokenRequest(
        path = "token", query = "grant_type" to "password",
        body = JsonObject(mapOf("email" to JsonPrimitive(email), "password" to JsonPrimitive(password))),
    )

    suspend fun signUp(nome: String, email: String, password: String): TokenResult = tokenRequest(
        path = "signup", query = null,
        body = JsonObject(
            mapOf(
                "email" to JsonPrimitive(email),
                "password" to JsonPrimitive(password),
                "data" to JsonObject(mapOf("nome" to JsonPrimitive(nome))),
            ),
        ),
    )

    suspend fun refresh(refreshToken: String): TokenResult = tokenRequest(
        path = "token", query = "grant_type" to "refresh_token",
        body = JsonObject(mapOf("refresh_token" to JsonPrimitive(refreshToken))),
    )

    /** Pede o e-mail com o link de nova senha. `true` se o servidor aceitou o pedido, exista a conta ou não. */
    suspend fun recover(email: String): RecoverResult {
        val response = execute(authRequest("recover", null, null, JsonObject(mapOf("email" to JsonPrimitive(email)))))
            ?: return RecoverResult.OFFLINE
        return when {
            response.code in 200..299 -> RecoverResult.SENT
            response.code == TOO_MANY_REQUESTS -> RecoverResult.TOO_MANY
            else -> RecoverResult.UNEXPECTED
        }
    }

    /** Perfil de quem acabou de entrar: o `uuid` do usuário na fazenda, o nome e o papel. */
    suspend fun fetchProfile(accessToken: String, authUserId: String): ProfileResult {
        val url = baseUrl("rest/v1/usuario").newBuilder()
            .addQueryParameter("auth_user_id", "eq.$authUserId")
            .addQueryParameter("select", "uuid,nome,email,papel,status")
            .addQueryParameter("limit", "1")
            .build()
        val request = Request.Builder().url(url).get().headers(accessToken).build()
        val response = execute(request) ?: return ProfileResult.Offline
        if (response.code !in 200..299) return ProfileResult.Unexpected
        val row = runCatching { json.parseToJsonElement(response.body).jsonArray.firstOrNull()?.jsonObject }.getOrNull()
            ?: return ProfileResult.NoAccess
        return runCatching {
            if (row.string("status") != "ATIVO") {
                ProfileResult.NoAccess
            } else {
                ProfileResult.Found(
                    Session(
                        userId = checkNotNull(row.string("uuid")),
                        nome = checkNotNull(row.string("nome")),
                        email = checkNotNull(row.string("email")),
                        papel = Papel.valueOf(checkNotNull(row.string("papel"))),
                    ),
                )
            }
        }.getOrElse { ProfileResult.Unexpected }
    }

    enum class RecoverResult { SENT, TOO_MANY, OFFLINE, UNEXPECTED }

    private suspend fun tokenRequest(path: String, query: Pair<String, String>?, body: JsonObject): TokenResult {
        val response = execute(authRequest(path, query, null, body)) ?: return TokenResult.Offline
        return when (response.code) {
            in 200..299 -> parseTokens(response.body)
            TOO_MANY_REQUESTS -> TokenResult.TooManyAttempts
            400, 401, 422 -> classifyError(response.body)
            else -> TokenResult.Unexpected
        }
    }

    /** Erro do login: e-mail já usado, credencial errada ou refresh recusado (todos viram o mesmo "não entrou"). */
    private fun classifyError(body: String): TokenResult {
        val code = runCatching { json.parseToJsonElement(body).jsonObject.let { it.string("error_code") ?: it.string("error") } }.getOrNull()
        return when (code) {
            "user_already_exists", "email_exists" -> TokenResult.EmailTaken
            "over_request_rate_limit", "over_email_send_rate_limit" -> TokenResult.TooManyAttempts
            else -> TokenResult.InvalidCredentials
        }
    }

    private fun parseTokens(body: String): TokenResult = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        val access = root.string("access_token")
        val refresh = root.string("refresh_token")
        val userId = root["user"]?.jsonObject?.string("id")
        if (access == null || refresh == null || userId == null) {
            // Cadastro com "confirmar e-mail" ligado no servidor devolve só o usuário, sem sessão.
            TokenResult.Unexpected
        } else {
            val expiresIn = root["expires_in"]?.jsonPrimitive?.longOrNull ?: DEFAULT_EXPIRES_IN_SECONDS
            TokenResult.Ok(AuthTokens(access, refresh, clock.instant().epochSecond + expiresIn), userId)
        }
    }.getOrElse { TokenResult.Unexpected }

    private fun authRequest(path: String, query: Pair<String, String>?, accessToken: String?, body: JsonObject): Request {
        val url = baseUrl("auth/v1/$path").newBuilder().apply { query?.let { addQueryParameter(it.first, it.second) } }.build()
        return Request.Builder().url(url)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .headers(accessToken)
            .build()
    }

    private fun Request.Builder.headers(accessToken: String?): Request.Builder {
        header("apikey", config.anonKey)
        header("Authorization", "Bearer ${accessToken ?: config.anonKey}")
        header("Accept", "application/json")
        return this
    }

    private fun baseUrl(path: String): HttpUrl = "${config.url}/$path".toHttpUrl()

    /** `null` = sem conexão (qualquer falha de rede). */
    private suspend fun execute(request: Request): RawResponse? = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { RawResponse(it.code, it.body.string()) }
        } catch (_: IOException) {
            null
        }
    }

    private class RawResponse(val code: Int, val body: String)

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

    internal companion object {
        const val TOO_MANY_REQUESTS = 429
        const val DEFAULT_EXPIRES_IN_SECONDS = 3600L
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }
    }
}
