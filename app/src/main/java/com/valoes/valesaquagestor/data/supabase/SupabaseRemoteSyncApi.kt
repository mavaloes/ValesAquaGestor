package com.valoes.valesaquagestor.data.supabase

import com.valoes.valesaquagestor.data.sync.RemotePage
import com.valoes.valesaquagestor.data.sync.RemoteSyncApi
import com.valoes.valesaquagestor.data.sync.SyncException
import com.valoes.valesaquagestor.data.sync.SyncFailure
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Sincronização por HTTP com o PostgREST do Supabase.
 *
 * - **Envio:** `POST /rest/v1/<tabela>?on_conflict=uuid` com `resolution=merge-duplicates`: o mesmo
 *   `uuid` enviado de novo atualiza a mesma linha (nunca cria cópia). O banco ignora versão mais velha
 *   que a guardada (gatilho em `backend/supabase/schema.sql`), então vence o maior `atualizado_em`.
 * - **Download:** `GET` das linhas com `servidor_em` depois da marca, em ordem. `servidor_em` é o
 *   relógio do servidor, que não sofre com a hora errada dos aparelhos.
 * - Chave recusada (401): renova uma vez e tenta de novo; se a renovação falhar, a sessão acabou.
 */
class SupabaseRemoteSyncApi(
    private val config: SupabaseConfig,
    private val client: OkHttpClient,
    private val tokens: AccessTokenProvider,
) : RemoteSyncApi {

    override suspend fun push(table: String, rows: List<JsonObject>) {
        if (rows.isEmpty()) return
        val body = JsonArray(rows).toString().toRequestBody(SupabaseAuthApi.JSON_MEDIA_TYPE)
        val url = "${config.url}/rest/v1/$table".toHttpUrl().newBuilder().addQueryParameter("on_conflict", "uuid").build()
        send(table = table) { token ->
            Request.Builder().url(url).post(body)
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .authorized(token)
                .build()
        }
    }

    override suspend fun pull(table: String, after: String?, limit: Int): RemotePage {
        val url = "${config.url}/rest/v1/$table".toHttpUrl().newBuilder().apply {
            addQueryParameter("select", "*")
            if (after != null) addQueryParameter("servidor_em", "gt.$after")
            addQueryParameter("order", "servidor_em.asc")
            addQueryParameter("limit", limit.toString())
        }.build()
        val text = send(table = table) { token -> Request.Builder().url(url).get().authorized(token).build() }
        val rows = try {
            SupabaseAuthApi.json.parseToJsonElement(text).jsonArray.map { it.jsonObject }
        } catch (e: Exception) {
            throw SyncException(SyncFailure.SERVER, "Resposta inesperada de $table.", e)
        }
        val last = rows.lastOrNull()?.get("servidor_em")?.let { (it as? JsonPrimitive)?.contentOrNull }
        return RemotePage(rows, last)
    }

    private fun Request.Builder.authorized(token: String): Request.Builder {
        header("apikey", config.anonKey)
        header("Authorization", "Bearer $token")
        header("Content-Type", "application/json")
        header("Accept", "application/json")
        return this
    }

    /** Executa a chamada com a chave atual e, se vier 401, uma vez com a chave renovada. Devolve o corpo da resposta. */
    private suspend fun send(table: String, build: (token: String) -> Request): String {
        var response = execute(build(tokens.accessToken()))
        if (response.code == UNAUTHORIZED) response = execute(build(tokens.accessToken(forceRefresh = true)))
        return when (response.code) {
            in 200..299 -> response.body
            UNAUTHORIZED -> throw SyncException(SyncFailure.AUTH, "Sessão recusada em $table.")
            else -> throw SyncException(SyncFailure.SERVER, "Servidor respondeu ${response.code} em $table.")
        }
    }

    private suspend fun execute(request: Request): RawResponse = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { RawResponse(it.code, it.body.string()) }
        } catch (e: IOException) {
            throw SyncException(SyncFailure.OFFLINE, cause = e)
        }
    }

    private class RawResponse(val code: Int, val body: String)

    private companion object {
        const val UNAUTHORIZED = 401
    }
}
