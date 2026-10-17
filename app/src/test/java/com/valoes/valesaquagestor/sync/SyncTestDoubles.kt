package com.valoes.valesaquagestor.sync

import com.valoes.valesaquagestor.data.TestEnvironment
import com.valoes.valesaquagestor.data.supabase.AuthTokens
import com.valoes.valesaquagestor.data.supabase.TokenStore
import com.valoes.valesaquagestor.data.sync.RemotePage
import com.valoes.valesaquagestor.data.sync.RemoteSyncApi
import com.valoes.valesaquagestor.data.sync.SyncCursorStore
import com.valoes.valesaquagestor.data.sync.SyncEngine
import com.valoes.valesaquagestor.data.sync.SyncException
import com.valoes.valesaquagestor.data.sync.SyncFailure
import com.valoes.valesaquagestor.data.sync.SyncStateHolder
import com.valoes.valesaquagestor.data.sync.buildSyncTables
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Marcas de download guardadas só na memória. */
class InMemoryCursorStore : SyncCursorStore {
    val values = mutableMapOf<String, String>()
    override suspend fun get(table: String): String? = values[table]
    override suspend fun set(table: String, cursor: String) {
        values[table] = cursor
    }
}

/** Chaves de acesso guardadas só na memória. */
class FakeTokenStore(var tokens: AuthTokens? = null) : TokenStore {
    override suspend fun get(): AuthTokens? = tokens
    override suspend fun save(tokens: AuthTokens) {
        this.tokens = tokens
    }

    override suspend fun clear() {
        tokens = null
    }
}

/**
 * Servidor de mentira que se comporta como o `backend/supabase/schema.sql`:
 *
 * - envio é *upsert* por `uuid` (nunca duplica);
 * - versão com `atualizado_em` menor ou igual à guardada é ignorada (vence o maior);
 * - recusa filho cujo pai ainda não está lá (chave estrangeira);
 * - cada gravação recebe um `servidor_em` crescente, por onde o download pagina.
 *
 * Também sabe falhar: sem conexão, sessão recusada e queda no meio do envio.
 */
class FakeRemoteServer : RemoteSyncApi {

    val tables = mutableMapOf<String, MutableMap<String, JsonObject>>()
    var offline = false
    var authExpired = false

    /** Número (a partir de 1) da chamada de envio que cai como falta de conexão, antes de gravar. */
    var dropPushCall: Int? = null
    var pushCalls = 0
        private set

    /** Chamado depois que o servidor aceitou um envio e antes de responder (a edição "no meio do caminho"). */
    var afterAccepting: (suspend (table: String) -> Unit)? = null

    /** Marcas `after` pedidas em cada download, na ordem. */
    val pullRequests = mutableListOf<String?>()

    private var tick = 0L
    private val base = Instant.parse("2026-01-01T00:00:00Z")

    private val parents = mapOf(
        "lote" to listOf("tanque_uuid" to "tanque"),
        "qualidade_agua" to listOf("tanque_uuid" to "tanque"),
        "arracoamento" to listOf("lote_uuid" to "lote"),
        "biometria" to listOf("lote_uuid" to "lote"),
        "movimentacao_lote" to listOf("lote_uuid" to "lote", "tanque_destino_uuid" to "tanque"),
    )

    fun rows(table: String): List<JsonObject> = tables[table]?.values?.toList().orEmpty()

    fun row(table: String, uuid: String): JsonObject? = tables[table]?.get(uuid)

    override suspend fun push(table: String, rows: List<JsonObject>) {
        pushCalls++
        if (offline || pushCalls == dropPushCall) throw SyncException(SyncFailure.OFFLINE)
        if (authExpired) throw SyncException(SyncFailure.AUTH)
        rows.forEach { row ->
            parents[table]?.forEach { (column, parent) ->
                val reference = (row[column] as? JsonPrimitive)?.contentOrNull
                if (reference != null && tables[parent]?.containsKey(reference) != true) {
                    throw SyncException(SyncFailure.SERVER, "chave estrangeira: $parent/$reference não existe")
                }
            }
        }
        val store = tables.getOrPut(table) { mutableMapOf() }
        rows.forEach { row ->
            val uuid = checkNotNull((row["uuid"] as JsonPrimitive).contentOrNull)
            val existing = store[uuid]
            val incomingVersion = (row["atualizado_em"] as JsonPrimitive).content.toLong()
            val storedVersion = existing?.let { (it["atualizado_em"] as JsonPrimitive).content.toLong() }
            if (storedVersion == null || incomingVersion > storedVersion) {
                tick++
                val serverTime = base.plusMillis(tick).atOffset(ZoneOffset.UTC).toString()
                store[uuid] = JsonObject(row + ("servidor_em" to JsonPrimitive(serverTime)))
            }
        }
        afterAccepting?.invoke(table)
    }

    override suspend fun pull(table: String, after: String?, limit: Int): RemotePage {
        pullRequests += after
        if (offline) throw SyncException(SyncFailure.OFFLINE)
        if (authExpired) throw SyncException(SyncFailure.AUTH)
        val threshold = after?.let(OffsetDateTime::parse)
        val page = rows(table)
            .filter { row -> threshold == null || OffsetDateTime.parse(serverTimeOf(row)).isAfter(threshold) }
            .sortedBy { OffsetDateTime.parse(serverTimeOf(row = it)) }
            .take(limit)
        return RemotePage(page, page.lastOrNull()?.let(::serverTimeOf))
    }

    private fun serverTimeOf(row: JsonObject): String = (row["servidor_em"] as JsonPrimitive).content
}

/** Um aparelho: banco local em memória (Robolectric) + motor de sincronização ligado ao [server]. */
class Device(val server: FakeRemoteServer) {
    val env = TestEnvironment()
    val cursors = InMemoryCursorStore()
    val state = SyncStateHolder()
    val engine = SyncEngine(buildSyncTables(env.db), server, cursors, state) { }

    fun close() = env.close()
}
