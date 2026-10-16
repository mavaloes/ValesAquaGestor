package com.valoes.valesaquagestor.data

import com.valoes.valesaquagestor.domain.usecase.QrResolution
import com.valoes.valesaquagestor.domain.usecase.ResolveTankQr
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** O uuid que vai no QR Code encontra o tanque no banco; tanque excluído não abre. */
@RunWith(RobolectricTestRunner::class)
class TankQrRepositoryTest {

    private lateinit var env: TestEnvironment

    @Before
    fun setUp() {
        env = TestEnvironment()
    }

    @After
    fun tearDown() = env.close()

    @Test
    fun `o uuid gravado no tanque abre o proprio tanque`() = runBlocking {
        val id = env.tanques.save(env.newTank("Viveiro 1"))
        val uuid = env.tanques.get(id)!!.uuid

        assertEquals(id, env.tanques.getByUuid(uuid)!!.id)
        assertEquals(QrResolution.Found(id), ResolveTankQr(env.tanques)(uuid))
    }

    @Test
    fun `tanque excluido ou uuid desconhecido nao abre`() = runBlocking {
        val id = env.tanques.save(env.newTank("Viveiro 1"))
        val uuid = env.tanques.get(id)!!.uuid
        env.tanques.delete(id)

        assertNull(env.tanques.getByUuid(uuid))
        assertEquals(QrResolution.UnknownTank, ResolveTankQr(env.tanques)(uuid))
        assertNull(env.tanques.getByUuid("11111111-2222-3333-4444-555555555555"))
    }
}
