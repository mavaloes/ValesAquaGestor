package com.valoes.valesaquagestor.sync

import com.valoes.valesaquagestor.auth.FakeSessionStore
import com.valoes.valesaquagestor.data.TestEnvironment
import com.valoes.valesaquagestor.data.supabase.AuthTokens
import com.valoes.valesaquagestor.data.sync.BackendInfo
import com.valoes.valesaquagestor.data.sync.PeriodicSync
import com.valoes.valesaquagestor.data.sync.SessionProfileRefresher
import com.valoes.valesaquagestor.data.sync.SyncCoordinator
import com.valoes.valesaquagestor.data.sync.SyncStateHolder
import com.valoes.valesaquagestor.data.sync.SyncStatusRepositoryImpl
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.model.StatusUsuario
import com.valoes.valesaquagestor.domain.model.Usuario
import com.valoes.valesaquagestor.domain.sync.SyncPhase
import com.valoes.valesaquagestor.domain.sync.SyncScheduler
import com.valoes.valesaquagestor.domain.sync.SyncStatus
import com.valoes.valesaquagestor.presentation.sync.SyncIndicator
import com.valoes.valesaquagestor.presentation.sync.toIndicator
import com.valoes.valesaquagestor.settings.FakePendingSyncRepository
import com.valoes.valesaquagestor.settings.FixedSessionAuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Indicador de estado, pedidos de sincronização e sessão acompanhando o servidor. */
@RunWith(RobolectricTestRunner::class)
class SyncCoordinationTest {

    private class CountingScheduler : SyncScheduler, PeriodicSync {
        var syncRequests = 0
        var periodicScheduled = 0
        var periodicCancelled = 0
        override fun requestSync() {
            syncRequests++
        }

        override fun schedule() {
            periodicScheduled++
        }

        override fun cancel() {
            periodicCancelled++
        }
    }

    private lateinit var env: TestEnvironment

    @Before
    fun setUp() {
        env = TestEnvironment()
    }

    @After
    fun tearDown() = env.close()

    // ---- Indicador ----

    @Test
    fun `o indicador mostra tudo sincronizado, sincronizando ou N registros aguardando conexao`() {
        assertEquals(SyncIndicator.Synced, SyncStatus(0, SyncPhase.IDLE).toIndicator())
        assertEquals(SyncIndicator.Syncing, SyncStatus(3, SyncPhase.RUNNING).toIndicator())
        assertEquals(SyncIndicator.Pending(3), SyncStatus(3, SyncPhase.IDLE).toIndicator())
        assertEquals(SyncIndicator.Pending(3), SyncStatus(3, SyncPhase.FAILED).toIndicator())
    }

    @Test
    fun `sem servidor configurado o indicador nao aparece`() {
        assertEquals(SyncIndicator.Hidden, SyncStatus(5, SyncPhase.IDLE, backendConfigured = false).toIndicator())
    }

    @Test
    fun `o estado junta os pendentes com a fase da sincronizacao`() = runBlocking {
        val pending = FakePendingSyncRepository(count = 2)
        val state = SyncStateHolder()
        val repository = SyncStatusRepositoryImpl(pending, state, BackendInfo(isConfigured = true))

        assertEquals(SyncStatus(2, SyncPhase.IDLE, true), repository.status.first())
        pending.count = 0
        assertEquals(0, repository.status.first().pendingCount)
    }

    // ---- Quando pedir sincronização ----

    private fun coordinator(
        auth: FixedSessionAuthRepository,
        pending: FakePendingSyncRepository,
        scheduler: CountingScheduler,
        configured: Boolean = true,
    ) = SyncCoordinator(auth, pending, scheduler, scheduler, BackendInfo(configured))

    private suspend fun settle() = delay(300)

    @Test
    fun `ao entrar na conta pede o download e agenda o periodico`() = runBlocking {
        val scheduler = CountingScheduler()
        coordinator(FixedSessionAuthRepository(FixedSessionAuthRepository.ADMIN), FakePendingSyncRepository(0), scheduler).start()
        settle()

        assertEquals(1, scheduler.syncRequests)
        assertEquals(1, scheduler.periodicScheduled)
    }

    @Test
    fun `registro novo pendente pede o envio, e zerar os pendentes nao pede outra rodada`() = runBlocking {
        val scheduler = CountingScheduler()
        val pending = FakePendingSyncRepository(0)
        coordinator(FixedSessionAuthRepository(FixedSessionAuthRepository.ADMIN), pending, scheduler).start()
        settle()
        assertEquals(1, scheduler.syncRequests)

        pending.count = 1
        settle()
        assertEquals(2, scheduler.syncRequests)

        pending.count = 2 // outro registro salvo durante o envio
        settle()
        assertEquals(3, scheduler.syncRequests)

        pending.count = 0 // envio concluído
        settle()
        assertEquals(3, scheduler.syncRequests)
    }

    @Test
    fun `sem sessao nada e pedido, mesmo com registros pendentes`() = runBlocking {
        val scheduler = CountingScheduler()
        coordinator(FixedSessionAuthRepository(null), FakePendingSyncRepository(5), scheduler).start()
        settle()

        assertEquals(0, scheduler.syncRequests)
    }

    @Test
    fun `sem servidor configurado o coordenador nao faz nada`() = runBlocking {
        val scheduler = CountingScheduler()
        coordinator(
            FixedSessionAuthRepository(FixedSessionAuthRepository.ADMIN), FakePendingSyncRepository(5), scheduler, configured = false,
        ).start()
        settle()

        assertEquals(0, scheduler.syncRequests)
        assertEquals(0, scheduler.periodicScheduled)
    }

    @Test
    fun `ao sair cancela o download periodico`() = runBlocking {
        val scheduler = CountingScheduler()
        val auth = FixedSessionAuthRepository(FixedSessionAuthRepository.ADMIN)
        coordinator(auth, FakePendingSyncRepository(0), scheduler).start()
        settle()

        auth.signOut()
        settle()

        assertEquals(1, scheduler.periodicCancelled)
    }

    // ---- Sessão acompanha o servidor ----

    private val me = Session("u1", "Maria", "maria@exemplo.com", Papel.FUNCIONARIO)

    private fun refresher(store: FakeSessionStore, tokens: FakeTokenStore) =
        SessionProfileRefresher(store, env.db.usuarioDao(), tokens)

    @Test
    fun `se o ADMIN trocou o meu perfil, a sessao acompanha depois de baixar`() = runBlocking {
        val store = FakeSessionStore(me)
        env.usuarios.save(Usuario(uuid = "u1", nome = "Maria Souza", email = "maria@exemplo.com", papel = Papel.ADMIN))

        refresher(store, FakeTokenStore()).refresh()

        assertEquals(Papel.ADMIN, store.session.first()!!.papel)
        assertEquals("Maria Souza", store.session.first()!!.nome)
    }

    @Test
    fun `conta desativada encerra a sessao e as chaves, e os registros ficam`() = runBlocking {
        val store = FakeSessionStore(me)
        val tokens = FakeTokenStore(AuthTokens("a", "r", 1))
        env.usuarios.save(
            Usuario(uuid = "u1", nome = "Maria", email = "maria@exemplo.com", papel = Papel.FUNCIONARIO, status = StatusUsuario.DESATIVADO),
        )
        val tank = env.tanques.save(env.newTank())

        refresher(store, tokens).refresh()

        assertNull(store.session.first())
        assertNull(tokens.get())
        assertNotNull(env.tanques.get(tank))
    }

    @Test
    fun `sem mudanca a sessao nao e regravada`() = runBlocking {
        val store = FakeSessionStore(me)
        env.usuarios.save(Usuario(uuid = "u1", nome = "Maria", email = "maria@exemplo.com", papel = Papel.FUNCIONARIO))

        refresher(store, FakeTokenStore()).refresh()

        assertEquals(me, store.session.first())
    }
}
