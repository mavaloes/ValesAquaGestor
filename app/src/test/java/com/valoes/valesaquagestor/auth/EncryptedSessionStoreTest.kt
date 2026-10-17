package com.valoes.valesaquagestor.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.valoes.valesaquagestor.data.session.EncryptedSessionStore
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.Session
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EncryptedSessionStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: EncryptedSessionStore

    private val session = Session("u1", "Maria Silva", "maria@exemplo.com", Papel.FUNCIONARIO)
    private val key = stringPreferencesKey("session")

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "test.preferences_pb") }
        store = EncryptedSessionStore(dataStore, FakeSessionCipher())
    }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `sem nada salvo nao ha sessao`() = runBlocking {
        assertNull(store.session.first())
    }

    @Test
    fun `salva e le a sessao`() = runBlocking {
        store.save(session)

        assertEquals(session, store.session.first())
    }

    @Test
    fun `no disco fica so o texto cifrado`() = runBlocking {
        store.save(session)

        val stored = dataStore.data.first()[key]
        assertNotNull(stored)
        assertTrue(stored!!.startsWith(FakeSessionCipher.PREFIX))
        assertFalse("o e-mail não pode aparecer em texto puro", stored.contains("maria@exemplo.com"))
        assertFalse(stored.contains("Maria Silva"))
    }

    @Test
    fun `limpar apaga a sessao`() = runBlocking {
        store.save(session)

        store.clear()

        assertNull(store.session.first())
        assertNull(dataStore.data.first()[key])
    }

    @Test
    fun `texto ilegivel vale como sem sessao`() = runBlocking {
        dataStore.edit { it[key] = "lixo-que-nao-foi-cifrado" }

        assertNull(store.session.first())
    }

    @Test
    fun `texto cifrado com conteudo invalido vale como sem sessao`() = runBlocking {
        dataStore.edit { it[key] = FakeSessionCipher().encrypt("isto não é json") }

        assertNull(store.session.first())
    }

    @Test
    fun `papel desconhecido vale como sem sessao`() = runBlocking {
        val json = """{"userId":"u1","nome":"M","email":"m@x.com","papel":"SUPERUSUARIO"}"""
        dataStore.edit { it[key] = FakeSessionCipher().encrypt(json) }

        assertNull(store.session.first())
    }
}
