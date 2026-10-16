package com.valoes.valesaquagestor.sync

import com.valoes.valesaquagestor.auth.FakeSessionStore
import com.valoes.valesaquagestor.data.supabase.AccessTokenProvider
import com.valoes.valesaquagestor.data.supabase.AuthTokens
import com.valoes.valesaquagestor.data.supabase.SupabaseAuthApi
import com.valoes.valesaquagestor.data.supabase.SupabaseAuthRepository
import com.valoes.valesaquagestor.data.supabase.SupabaseConfig
import com.valoes.valesaquagestor.data.supabase.SupabaseRemoteSyncApi
import com.valoes.valesaquagestor.data.sync.SyncException
import com.valoes.valesaquagestor.data.sync.SyncFailure
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.repository.AuthError
import com.valoes.valesaquagestor.domain.repository.AuthResult
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** Conversa HTTP com o Supabase, contra um servidor HTTP simulado (nenhum servidor real). */
class SupabaseHttpTest {

    private lateinit var http: MockWebServer
    private lateinit var config: SupabaseConfig
    private val client = OkHttpClient()
    private val now = Instant.parse("2026-10-16T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val tokens = FakeTokenStore(AuthTokens("acesso-1", "renovacao-1", now.epochSecond + 3600))
    private val sessionStore = FakeSessionStore(Session("u1", "Maria", "maria@exemplo.com", Papel.ADMIN))

    @Before
    fun setUp() {
        http = MockWebServer()
        http.start()
        config = SupabaseConfig(http.url("/").toString().trimEnd('/'), "chave-anon")
    }

    @After
    fun tearDown() {
        runCatching { http.shutdown() }
    }

    private fun authApi() = SupabaseAuthApi(config, client, clock)
    private fun provider() = AccessTokenProvider(tokens, authApi(), sessionStore, clock)
    private fun syncApi() = SupabaseRemoteSyncApi(config, client, provider())

    private fun reply(code: Int, body: String = "") = MockResponse().setResponseCode(code).setBody(body)

    private val tokenBody = """{"access_token":"acesso-2","refresh_token":"renovacao-2","expires_in":3600,"user":{"id":"auth-1"}}"""
    private val profileBody = """[{"uuid":"user-uuid","nome":"Maria Silva","email":"maria@exemplo.com","papel":"ADMIN","status":"ATIVO"}]"""

    private fun row(uuid: String, version: Long, vararg extra: Pair<String, String>) = JsonObject(
        mapOf("uuid" to JsonPrimitive(uuid), "atualizado_em" to JsonPrimitive(version)) + extra.associate { it.first to JsonPrimitive(it.second) },
    )

    // ---- Envio ----

    @Test
    fun `o envio faz upsert por uuid com a chave de acesso e devolve sem erro`() = runBlocking {
        http.enqueue(reply(201))

        syncApi().push("tanque", listOf(row("t-1", 5, "nome" to "Viveiro 1"), row("t-2", 6)))

        val request = http.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/rest/v1/tanque?on_conflict=uuid", request.path)
        assertEquals("chave-anon", request.getHeader("apikey"))
        assertEquals("Bearer acesso-1", request.getHeader("Authorization"))
        assertTrue(request.getHeader("Prefer")!!.contains("resolution=merge-duplicates"))
        val sent = Json.parseToJsonElement(request.body.readUtf8()).jsonArray
        assertEquals(listOf("t-1", "t-2"), sent.map { (it.jsonObject["uuid"] as JsonPrimitive).content })
    }

    @Test
    fun `envio de lista vazia nao chama o servidor`() = runBlocking {
        syncApi().push("tanque", emptyList())

        assertEquals(0, http.requestCount)
    }

    @Test
    fun `chave recusada e renovada uma vez e a chamada e repetida com a chave nova`() = runBlocking {
        http.enqueue(reply(401))
        http.enqueue(reply(200, tokenBody)) // renovação
        http.enqueue(reply(201))

        syncApi().push("tanque", listOf(row("t-1", 5)))

        http.takeRequest()
        val refresh = http.takeRequest()
        assertEquals("/auth/v1/token?grant_type=refresh_token", refresh.path)
        val retried = http.takeRequest()
        assertEquals("Bearer acesso-2", retried.getHeader("Authorization"))
        assertEquals("acesso-2", tokens.tokens!!.accessToken)
    }

    @Test
    fun `renovacao recusada de vez encerra a sessao sem apagar nada e pede novo login`() = runBlocking {
        http.enqueue(reply(401))
        http.enqueue(reply(400, """{"error_code":"refresh_token_not_found"}"""))

        try {
            syncApi().push("tanque", listOf(row("t-1", 5)))
            fail("deveria pedir novo login")
        } catch (e: SyncException) {
            assertEquals(SyncFailure.AUTH, e.failure)
        }

        assertNull(tokens.tokens)
        assertNull(sessionStore.session.first())
    }

    @Test
    fun `chave perto de vencer e renovada antes da chamada`() = runBlocking {
        tokens.tokens = AuthTokens("acesso-velho", "renovacao-1", now.epochSecond + 10)
        http.enqueue(reply(200, tokenBody))
        http.enqueue(reply(201))

        syncApi().push("tanque", listOf(row("t-1", 5)))

        assertEquals("/auth/v1/token?grant_type=refresh_token", http.takeRequest().path)
        assertEquals("Bearer acesso-2", http.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `sem internet a renovacao falha como sem conexao e nao derruba a sessao`() = runBlocking {
        tokens.tokens = AuthTokens("acesso-velho", "renovacao-1", now.epochSecond - 1)
        http.shutdown()

        try {
            syncApi().push("tanque", listOf(row("t-1", 5)))
            fail("deveria falhar por falta de conexão")
        } catch (e: SyncException) {
            assertEquals(SyncFailure.OFFLINE, e.failure)
        }

        assertNotNull(tokens.tokens)
        assertNotNull(sessionStore.session.first())
    }

    @Test
    fun `erro do servidor e falta de conexao no envio viram falhas de tentar de novo`() = runBlocking {
        http.enqueue(reply(500))
        try {
            syncApi().push("tanque", listOf(row("t-1", 5)))
            fail()
        } catch (e: SyncException) {
            assertEquals(SyncFailure.SERVER, e.failure)
        }

        http.shutdown()
        try {
            syncApi().push("tanque", listOf(row("t-1", 5)))
            fail()
        } catch (e: SyncException) {
            assertEquals(SyncFailure.OFFLINE, e.failure)
        }
    }

    // ---- Download ----

    @Test
    fun `o download pede o que mudou depois da marca, em ordem, e devolve a ultima marca`() = runBlocking {
        val rows = """[{"uuid":"t-1","atualizado_em":5,"servidor_em":"2026-10-16T12:00:00.100+00:00"},
            {"uuid":"t-2","atualizado_em":6,"servidor_em":"2026-10-16T12:00:00.200+00:00"}]"""
        http.enqueue(reply(200, rows))

        val page = syncApi().pull("tanque", "2026-10-16T11:59:00Z", 500)

        val request = http.takeRequest()
        assertEquals("GET", request.method)
        val url = request.requestUrl!!
        assertEquals("/rest/v1/tanque", url.encodedPath)
        assertEquals("gt.2026-10-16T11:59:00Z", url.queryParameter("servidor_em"))
        assertEquals("servidor_em.asc", url.queryParameter("order"))
        assertEquals("500", url.queryParameter("limit"))
        assertEquals(2, page.rows.size)
        assertEquals("2026-10-16T12:00:00.200+00:00", page.lastCursor)
    }

    @Test
    fun `o primeiro download nao filtra por marca e pagina vazia nao tem marca`() = runBlocking {
        http.enqueue(reply(200, "[]"))

        val page = syncApi().pull("lote", null, 500)

        assertNull(http.takeRequest().requestUrl!!.queryParameter("servidor_em"))
        assertTrue(page.rows.isEmpty())
        assertNull(page.lastCursor)
    }

    @Test
    fun `resposta que nao e uma lista vira falha do servidor`() = runBlocking {
        http.enqueue(reply(200, "isto não é json"))

        try {
            syncApi().pull("tanque", null, 500)
            fail()
        } catch (e: SyncException) {
            assertEquals(SyncFailure.SERVER, e.failure)
        }
    }

    // ---- Login ----

    private fun repository() = SupabaseAuthRepository(authApi(), tokens, sessionStore)

    @Test
    fun `login certo guarda a sessao com o perfil do servidor e as chaves`() = runBlocking {
        tokens.tokens = null
        sessionStore.clear()
        http.enqueue(reply(200, tokenBody))
        http.enqueue(reply(200, profileBody))

        val result = repository().signIn("  Maria@Exemplo.com ", "Maria1234")

        assertEquals(AuthResult.Success, result)
        val login = http.takeRequest()
        assertEquals("/auth/v1/token?grant_type=password", login.path)
        val sent = Json.parseToJsonElement(login.body.readUtf8()).jsonObject
        assertEquals("maria@exemplo.com", (sent["email"] as JsonPrimitive).content)
        val profile = http.takeRequest()
        assertEquals("Bearer acesso-2", profile.getHeader("Authorization"))
        assertEquals("eq.auth-1", profile.requestUrl!!.queryParameter("auth_user_id"))
        assertEquals(Session("user-uuid", "Maria Silva", "maria@exemplo.com", Papel.ADMIN), sessionStore.session.first())
        assertEquals("acesso-2", tokens.tokens!!.accessToken)
        assertEquals(now.epochSecond + 3600, tokens.tokens!!.expiresAtEpochSeconds)
    }

    @Test
    fun `senha errada da o erro generico e nao guarda nada`() = runBlocking {
        tokens.tokens = null
        sessionStore.clear()
        http.enqueue(reply(400, """{"error_code":"invalid_credentials","msg":"Invalid login credentials"}"""))

        val result = repository().signIn("maria@exemplo.com", "errada")

        assertEquals(AuthResult.Failure(AuthError.INVALID_CREDENTIALS), result)
        assertNull(tokens.tokens)
        assertNull(sessionStore.session.first())
    }

    @Test
    fun `conta desativada entra com a senha certa mas recebe o mesmo erro generico`() = runBlocking {
        tokens.tokens = null
        sessionStore.clear()
        http.enqueue(reply(200, tokenBody))
        http.enqueue(reply(200, "[]")) // as regras de acesso não mostram usuário desativado

        val result = repository().signIn("maria@exemplo.com", "Maria1234")

        assertEquals(AuthResult.Failure(AuthError.INVALID_CREDENTIALS), result)
        assertNull(tokens.tokens)
        assertNull(sessionStore.session.first())
    }

    @Test
    fun `sem internet o login diz que nao ha conexao`() = runBlocking {
        http.shutdown()

        assertEquals(AuthResult.Failure(AuthError.NO_CONNECTION), repository().signIn("maria@exemplo.com", "Maria1234"))
        assertEquals(AuthResult.Failure(AuthError.NO_CONNECTION), repository().signUp("Maria", "maria@exemplo.com", "Maria1234"))
        assertEquals(AuthResult.Failure(AuthError.NO_CONNECTION), repository().sendPasswordReset("maria@exemplo.com"))
    }

    @Test
    fun `muitas tentativas viram a mensagem de tentar mais tarde`() = runBlocking {
        http.enqueue(reply(429, """{"error_code":"over_request_rate_limit"}"""))

        assertEquals(AuthResult.Failure(AuthError.TOO_MANY_ATTEMPTS), repository().signIn("maria@exemplo.com", "x"))
    }

    @Test
    fun `cadastro manda o nome, abre a sessao do perfil criado e e-mail repetido e recusado`() = runBlocking {
        tokens.tokens = null
        sessionStore.clear()
        http.enqueue(reply(200, tokenBody))
        http.enqueue(reply(200, profileBody))

        assertEquals(AuthResult.Success, repository().signUp(" Maria Silva ", "Maria@Exemplo.com", "Maria1234"))
        val signup = http.takeRequest()
        assertEquals("/auth/v1/signup", signup.path)
        val sent = Json.parseToJsonElement(signup.body.readUtf8()).jsonObject
        assertEquals("Maria Silva", ((sent["data"] as JsonObject)["nome"] as JsonPrimitive).content)

        http.enqueue(reply(422, """{"error_code":"user_already_exists"}"""))
        assertEquals(
            AuthResult.Failure(AuthError.EMAIL_ALREADY_REGISTERED),
            repository().signUp("Outra", "maria@exemplo.com", "Maria1234"),
        )
    }

    @Test
    fun `cadastro com confirmacao de e-mail ligada no servidor nao abre sessao`() = runBlocking {
        tokens.tokens = null
        sessionStore.clear()
        http.enqueue(reply(200, """{"id":"auth-1","email":"maria@exemplo.com"}""")) // usuário sem sessão

        assertEquals(AuthResult.Failure(AuthError.UNKNOWN), repository().signUp("Maria", "maria@exemplo.com", "Maria1234"))
        assertNull(sessionStore.session.first())
    }

    @Test
    fun `recuperar senha responde igual para e-mail cadastrado ou nao`() = runBlocking {
        http.enqueue(reply(200, "{}"))
        http.enqueue(reply(200, "{}"))

        assertEquals(AuthResult.Success, repository().sendPasswordReset("maria@exemplo.com"))
        assertEquals(AuthResult.Success, repository().sendPasswordReset("ninguem@exemplo.com"))
        assertEquals("/auth/v1/recover", http.takeRequest().path)
    }

    @Test
    fun `sair funciona sem internet e apaga sessao e chaves, nunca os registros`() = runBlocking {
        http.shutdown()

        repository().signOut()

        assertNull(tokens.tokens)
        assertNull(sessionStore.session.first())
    }
}
