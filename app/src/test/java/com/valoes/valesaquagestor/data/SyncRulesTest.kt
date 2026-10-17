package com.valoes.valesaquagestor.data

import android.database.sqlite.SQLiteConstraintException
import com.valoes.valesaquagestor.data.repository.MergeResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Casos G1 e G2 de `specs/09-casos-de-teste.md` na regra local (a rede entra na Fase 09). */
@RunWith(RobolectricTestRunner::class)
class SyncRulesTest {

    private lateinit var env: TestEnvironment

    @Before
    fun setUp() {
        env = TestEnvironment()
    }

    @After
    fun tearDown() = env.close()

    private suspend fun savedTankEntity(): com.valoes.valesaquagestor.data.local.entity.TanqueEntity {
        val id = env.tanques.save(env.newTank())
        return env.db.tanqueDao().getByIdIncludingDeleted(id)!!
    }

    @Test
    fun `G1 o banco recusa dois registros com o mesmo uuid`() = runBlocking {
        val entity = savedTankEntity()
        try {
            env.db.tanqueDao().insert(entity.copy(id = 0))
            fail("o índice único de uuid deveria recusar")
        } catch (expected: SQLiteConstraintException) {
            // esperado
        }
    }

    @Test
    fun `G1 o mesmo registro recebido duas vezes vira um registro so`() = runBlocking {
        val incoming = savedTankEntity().copy(id = 0, uuid = "uuid-do-servidor", atualizadoEm = 50_000L, sincronizado = true)

        assertEquals(MergeResult.INSERTED, env.tanques.mergeFromRemote(incoming))
        assertEquals(MergeResult.IGNORED, env.tanques.mergeFromRemote(incoming))

        val rows = env.db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM tanque WHERE uuid = 'uuid-do-servidor'")
        rows.use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
    }

    @Test
    fun `G2 vence o maior atualizadoEm e o id local e mantido`() = runBlocking {
        env.clock.now = 10_000L
        val local = savedTankEntity()

        val older = local.copy(nome = "Versão antiga", atualizadoEm = 9_000L)
        assertEquals(MergeResult.IGNORED, env.tanques.mergeFromRemote(older))
        assertEquals(local.nome, env.db.tanqueDao().findByUuid(local.uuid)!!.nome)

        val tie = local.copy(nome = "Empate", atualizadoEm = 10_000L)
        assertEquals("empate mantém o local", MergeResult.IGNORED, env.tanques.mergeFromRemote(tie))

        val newer = local.copy(id = 12345, nome = "Versão nova", atualizadoEm = 11_000L, sincronizado = true)
        assertEquals(MergeResult.UPDATED, env.tanques.mergeFromRemote(newer))
        val merged = env.db.tanqueDao().findByUuid(local.uuid)!!
        assertEquals("Versão nova", merged.nome)
        assertEquals(local.id, merged.id)
        assertTrue(merged.sincronizado)
    }

    @Test
    fun `registro recebido com uuid novo e inserido ja sincronizado`() = runBlocking {
        val incoming = savedTankEntity().copy(id = 0, uuid = "novo", atualizadoEm = 1L, sincronizado = true)
        assertEquals(MergeResult.INSERTED, env.tanques.mergeFromRemote(incoming))
        assertTrue(env.db.tanqueDao().findByUuid("novo")!!.sincronizado)
    }
}
