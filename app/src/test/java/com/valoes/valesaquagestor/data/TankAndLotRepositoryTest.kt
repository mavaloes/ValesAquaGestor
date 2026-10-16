package com.valoes.valesaquagestor.data

import android.database.sqlite.SQLiteConstraintException
import com.valoes.valesaquagestor.domain.model.StatusLote
import com.valoes.valesaquagestor.domain.model.StatusTanque
import com.valoes.valesaquagestor.domain.model.TipoTanque
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TankAndLotRepositoryTest {

    private lateinit var env: TestEnvironment

    @Before
    fun setUp() {
        env = TestEnvironment()
    }

    @After
    fun tearDown() = env.close()

    @Test
    fun `salvar tanque novo preenche uuid, atualizadoEm e sincronizado false`() = runBlocking {
        env.clock.now = 5_000L
        val id = env.tanques.save(env.newTank())
        val entity = env.db.tanqueDao().getByIdIncludingDeleted(id)!!
        assertTrue(entity.uuid.isNotBlank())
        assertEquals(5_000L, entity.atualizadoEm)
        assertFalse(entity.sincronizado)
        assertFalse(entity.excluido)
    }

    @Test
    fun `editar tanque mantem o uuid e renova atualizadoEm`() = runBlocking {
        env.clock.now = 5_000L
        val id = env.tanques.save(env.newTank())
        val first = env.db.tanqueDao().getByIdIncludingDeleted(id)!!
        env.db.tanqueDao().update(first.copy(sincronizado = true))

        env.clock.now = 9_000L
        env.tanques.save(env.tanques.get(id)!!.copy(nome = "Viveiro 1B"))

        val edited = env.db.tanqueDao().getByIdIncludingDeleted(id)!!
        assertEquals(first.uuid, edited.uuid)
        assertEquals(9_000L, edited.atualizadoEm)
        assertFalse("editar volta a marcar como não sincronizado", edited.sincronizado)
        assertEquals("Viveiro 1B", env.tanques.get(id)!!.nome)
    }

    @Test
    fun `viveiro sem area e tanque redondo sem volume sao recusados`() = runBlocking {
        assertThrowsIllegalArgument { env.tanques.save(env.newTank().copy(areaM2 = null)) }
        assertThrowsIllegalArgument {
            env.tanques.save(env.newTank().copy(tipo = TipoTanque.REDONDO, areaM2 = null, volumeM3 = null))
        }
        env.tanques.save(env.newTank().copy(tipo = TipoTanque.REDONDO, areaM2 = null, volumeM3 = 20.0))
        assertEquals(1, env.tanques.observeActive().first().size)
    }

    @Test
    fun `lista de tanques ativos nao traz inativos nem excluidos`() = runBlocking {
        env.tanques.save(env.newTank("B"))
        env.tanques.save(env.newTank("A"))
        env.tanques.save(env.newTank("Inativo").copy(status = StatusTanque.INATIVO))
        val removed = env.tanques.save(env.newTank("Excluido"))
        env.tanques.delete(removed)

        assertEquals(listOf("A", "B"), env.tanques.observeActive().first().map { it.nome })
        assertNull(env.tanques.get(removed))
        assertNull(env.tanques.observe(removed).first())
    }

    @Test
    fun `G3 registro excluido e nao sincronizado some da lista mas continua no banco e na fila`() = runBlocking {
        val id = env.tanques.save(env.newTank())
        env.db.tanqueDao().update(env.db.tanqueDao().getByIdIncludingDeleted(id)!!.copy(sincronizado = true))
        assertTrue(env.db.tanqueDao().getPending().isEmpty())

        env.clock.now = 7_000L
        env.tanques.delete(id)

        assertTrue(env.tanques.observeActive().first().isEmpty())
        val row = env.db.tanqueDao().getByIdIncludingDeleted(id)
        assertNotNull("a linha não pode sumir do banco", row)
        assertTrue(row!!.excluido)
        assertEquals(7_000L, row.atualizadoEm)
        assertEquals("a exclusão precisa ser enviada ao servidor", listOf(id), env.db.tanqueDao().getPending().map { it.id })
    }

    @Test
    fun `lotes ativos de um tanque nao trazem encerrados, excluidos nem de outro tanque`() = runBlocking {
        val tankA = env.tanques.save(env.newTank("A"))
        val tankB = env.tanques.save(env.newTank("B"))
        val active = env.lotes.save(env.newLot(tankA))
        env.lotes.save(env.newLot(tankA).copy(status = StatusLote.ENCERRADO))
        val removed = env.lotes.save(env.newLot(tankA))
        env.lotes.delete(removed)
        env.lotes.save(env.newLot(tankB))

        assertEquals(listOf(active), env.lotes.observeActiveByTank(tankA).first().map { it.id })
        assertEquals(2, env.lotes.observeAllActive().first().size)
        assertNull(env.lotes.get(removed))
    }

    @Test
    fun `lote exige tanque existente e o tanque com lote nao e apagado pelo banco`() = runBlocking {
        try {
            env.lotes.save(env.newLot(tanqueId = 999))
            fail("deveria recusar um lote sem tanque")
        } catch (expected: SQLiteConstraintException) {
            // FK com índice e RESTRICT
        }
        val tank = env.tanques.save(env.newTank())
        env.lotes.save(env.newLot(tank))
        try {
            env.db.openHelper.writableDatabase.execSQL("DELETE FROM tanque WHERE id = $tank")
            fail("RESTRICT deveria impedir apagar o tanque com lote")
        } catch (expected: android.database.SQLException) {
            // esperado
        }
    }

    private suspend fun assertThrowsIllegalArgument(block: suspend () -> Unit) {
        try {
            block()
            fail("deveria lançar IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // esperado
        }
    }
}
