package com.valoes.valesaquagestor.settings

import com.valoes.valesaquagestor.domain.calc.MonitoringSchedule
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.StatusUsuario
import com.valoes.valesaquagestor.domain.model.Usuario
import com.valoes.valesaquagestor.domain.permission.Permission
import com.valoes.valesaquagestor.domain.permission.PermissionChecker
import com.valoes.valesaquagestor.domain.rules.WaterRanges
import com.valoes.valesaquagestor.domain.usecase.ChangeUserRole
import com.valoes.valesaquagestor.domain.usecase.DeactivateUser
import com.valoes.valesaquagestor.domain.usecase.InviteUser
import com.valoes.valesaquagestor.domain.usecase.ManageSettings
import com.valoes.valesaquagestor.domain.usecase.UseCaseError
import com.valoes.valesaquagestor.domain.usecase.UseCaseResult
import com.valoes.valesaquagestor.auth.FakeConnectivityChecker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tabela de `specs/06-perfis-permissoes.md`: cada permissão, para ADMIN (permitido) e FUNCIONARIO
 * (permitido ou negado), e a barreira no caso de uso: FUNCIONARIO não executa ação de ADMIN nem
 * chamando o caso de uso direto.
 */
class PermissionAndUseCaseTest {

    private val denied = UseCaseResult.Failure(UseCaseError.PERMISSION_DENIED)

    // ---- Tabela de 06 ----

    @Test
    fun `ADMIN pode todas as permissoes da tabela`() {
        Permission.entries.forEach { permission ->
            assertTrue("ADMIN deveria poder $permission", PermissionChecker.isAllowed(Papel.ADMIN, permission))
        }
    }

    @Test
    fun `FUNCIONARIO registra alimentacao, mortalidade, agua e biometria`() {
        listOf(
            Permission.REGISTER_FEEDING,
            Permission.REGISTER_MORTALITY,
            Permission.REGISTER_WATER,
            Permission.REGISTER_BIOMETRY,
        ).forEach { assertTrue("FUNCIONARIO deveria poder $it", PermissionChecker.isAllowed(Papel.FUNCIONARIO, it)) }
    }

    @Test
    fun `FUNCIONARIO ve tanques, lotes e alertas`() {
        assertTrue(PermissionChecker.isAllowed(Papel.FUNCIONARIO, Permission.VIEW_FARM))
    }

    @Test
    fun `FUNCIONARIO nao gerencia usuarios, configuracoes, custos nem relatorios`() {
        listOf(
            Permission.MANAGE_USERS,
            Permission.MANAGE_SETTINGS,
            Permission.VIEW_COSTS,
            Permission.VIEW_REPORTS,
        ).forEach { assertFalse("FUNCIONARIO nao deveria poder $it", PermissionChecker.isAllowed(Papel.FUNCIONARIO, it)) }
    }

    @Test
    fun `sem sessao nada e permitido`() {
        Permission.entries.forEach { assertFalse(PermissionChecker.isAllowed(null, it)) }
    }

    // ---- Convidar ----

    private class UsersFixture(
        session: com.valoes.valesaquagestor.domain.model.Session? = FixedSessionAuthRepository.ADMIN,
        initial: List<Usuario> = emptyList(),
        online: Boolean = true,
    ) {
        val auth = FixedSessionAuthRepository(session)
        val users = FakeUserRepository(initial)
        val connectivity = FakeConnectivityChecker(online)
        val invite = InviteUser(auth, users, connectivity)
        val changeRole = ChangeUserRole(auth, users)
        val deactivate = DeactivateUser(auth, users)
    }

    private fun employee(uuid: String = "func-2", email: String = "ana@exemplo.com") =
        Usuario(uuid = uuid, nome = "Ana", email = email, papel = Papel.FUNCIONARIO)

    @Test
    fun `o ADMIN convida um funcionario e o convite fica pendente de envio`() = runBlocking {
        val f = UsersFixture()

        val result = f.invite("João Souza", " Joao@Exemplo.com ", Papel.FUNCIONARIO)

        val outcome = (result as UseCaseResult.Success).value
        assertFalse(outcome.pendingOffline)
        val saved = f.users.saved.single()
        assertEquals("joao@exemplo.com", saved.email)
        assertEquals(StatusUsuario.CONVIDADO, saved.status)
        assertEquals(Papel.FUNCIONARIO, saved.papel)
        assertFalse(saved.sincronizado)
        assertEquals(saved.uuid, outcome.usuario.uuid)
    }

    @Test
    fun `sem internet o convite e guardado e o caso de uso avisa que ficou pendente`() = runBlocking {
        val f = UsersFixture(online = false)

        val result = f.invite("João", "joao@exemplo.com", Papel.FUNCIONARIO)

        assertTrue((result as UseCaseResult.Success).value.pendingOffline)
        assertEquals(1, f.users.saved.size)
    }

    @Test
    fun `FUNCIONARIO nao consegue convidar chamando o caso de uso direto`() = runBlocking {
        val f = UsersFixture(session = FixedSessionAuthRepository.EMPLOYEE)

        assertEquals(denied, f.invite("João", "joao@exemplo.com", Papel.ADMIN))
        assertTrue(f.users.saved.isEmpty())
    }

    @Test
    fun `sem sessao o convite tambem e negado`() = runBlocking {
        val f = UsersFixture(session = null)

        assertEquals(denied, f.invite("João", "joao@exemplo.com", Papel.FUNCIONARIO))
        assertTrue(f.users.saved.isEmpty())
    }

    @Test
    fun `convite com e-mail ou nome invalido nao grava nada`() = runBlocking {
        val f = UsersFixture()

        assertEquals(UseCaseResult.Failure(UseCaseError.INVALID_EMAIL), f.invite("João", "joao@", Papel.FUNCIONARIO))
        assertEquals(UseCaseResult.Failure(UseCaseError.INVALID_NAME), f.invite("  ", "joao@exemplo.com", Papel.FUNCIONARIO))
        assertTrue(f.users.saved.isEmpty())
    }

    @Test
    fun `nao convida e-mail que ja e de um usuario ativo ou convidado`() = runBlocking {
        val f = UsersFixture(initial = listOf(employee()))

        val result = f.invite("Outra Ana", "ANA@exemplo.com", Papel.FUNCIONARIO)

        assertEquals(UseCaseResult.Failure(UseCaseError.EMAIL_ALREADY_IN_USE), result)
        assertEquals(1, f.users.saved.size)
    }

    @Test
    fun `convidar de novo quem foi desativado reaproveita o cadastro`() = runBlocking {
        val f = UsersFixture(initial = listOf(employee().copy(status = StatusUsuario.DESATIVADO)))

        val result = f.invite("Ana Lima", "ana@exemplo.com", Papel.FUNCIONARIO)

        assertTrue(result is UseCaseResult.Success)
        val saved = f.users.saved.single()
        assertEquals("func-2", saved.uuid)
        assertEquals(StatusUsuario.CONVIDADO, saved.status)
        assertEquals("Ana Lima", saved.nome)
    }

    // ---- Trocar perfil e desativar ----

    @Test
    fun `o ADMIN troca o perfil de outro usuario`() = runBlocking {
        val f = UsersFixture(initial = listOf(employee()))

        assertEquals(UseCaseResult.Success(Unit), f.changeRole("func-2", Papel.ADMIN))
        assertEquals(Papel.ADMIN, f.users.get("func-2")!!.papel)
    }

    @Test
    fun `FUNCIONARIO nao consegue trocar perfil chamando o caso de uso direto`() = runBlocking {
        val f = UsersFixture(session = FixedSessionAuthRepository.EMPLOYEE, initial = listOf(employee()))

        assertEquals(denied, f.changeRole("func-2", Papel.ADMIN))
        assertEquals(Papel.FUNCIONARIO, f.users.get("func-2")!!.papel)
    }

    @Test
    fun `ninguem troca o proprio perfil nem desativa a si mesmo`() = runBlocking {
        val me = employee(uuid = FixedSessionAuthRepository.ADMIN.userId, email = FixedSessionAuthRepository.ADMIN.email)
            .copy(papel = Papel.ADMIN)
        val f = UsersFixture(initial = listOf(me))

        assertEquals(UseCaseResult.Failure(UseCaseError.CANNOT_CHANGE_SELF), f.changeRole(me.uuid, Papel.FUNCIONARIO))
        assertEquals(UseCaseResult.Failure(UseCaseError.CANNOT_CHANGE_SELF), f.deactivate(me.uuid))
        assertEquals(Papel.ADMIN, f.users.get(me.uuid)!!.papel)
        assertEquals(StatusUsuario.ATIVO, f.users.get(me.uuid)!!.status)
    }

    @Test
    fun `usuario que nao existe volta como nao encontrado`() = runBlocking {
        val f = UsersFixture()

        assertEquals(UseCaseResult.Failure(UseCaseError.USER_NOT_FOUND), f.changeRole("fantasma", Papel.ADMIN))
        assertEquals(UseCaseResult.Failure(UseCaseError.USER_NOT_FOUND), f.deactivate("fantasma"))
    }

    @Test
    fun `o ADMIN desativa um usuario`() = runBlocking {
        val f = UsersFixture(initial = listOf(employee()))

        assertEquals(UseCaseResult.Success(Unit), f.deactivate("func-2"))
        assertEquals(StatusUsuario.DESATIVADO, f.users.get("func-2")!!.status)
    }

    @Test
    fun `FUNCIONARIO nao consegue desativar chamando o caso de uso direto`() = runBlocking {
        val f = UsersFixture(session = FixedSessionAuthRepository.EMPLOYEE, initial = listOf(employee()))

        assertEquals(denied, f.deactivate("func-2"))
        assertEquals(StatusUsuario.ATIVO, f.users.get("func-2")!!.status)
    }

    // ---- Configurações ----

    private val customRanges = WaterRanges(oxygenAdequateMinMgL = 5.0)
    private val customSchedule = MonitoringSchedule(oxygenMorningHour = 5)

    @Test
    fun `o ADMIN salva as faixas e a agenda`() = runBlocking {
        val settings = FakeSettingsRepository()
        val manage = ManageSettings(FixedSessionAuthRepository(FixedSessionAuthRepository.ADMIN), settings)

        assertEquals(UseCaseResult.Success(Unit), manage.saveWaterRanges(customRanges))
        assertEquals(UseCaseResult.Success(Unit), manage.saveMonitoringSchedule(customSchedule))

        assertEquals(customRanges, settings.waterRanges.first())
        assertEquals(customSchedule, settings.monitoringSchedule.first())
    }

    @Test
    fun `o ADMIN restaura os valores de fabrica`() = runBlocking {
        val settings = FakeSettingsRepository(customRanges, customSchedule)
        val manage = ManageSettings(FixedSessionAuthRepository(FixedSessionAuthRepository.ADMIN), settings)

        assertEquals(UseCaseResult.Success(Unit), manage.resetWaterRanges())
        assertEquals(UseCaseResult.Success(Unit), manage.resetMonitoringSchedule())

        assertEquals(WaterRanges.DEFAULT, settings.waterRanges.first())
        assertEquals(MonitoringSchedule(), settings.monitoringSchedule.first())
    }

    @Test
    fun `FUNCIONARIO nao altera faixas nem agenda chamando o caso de uso direto`() = runBlocking {
        val settings = FakeSettingsRepository()
        val manage = ManageSettings(FixedSessionAuthRepository(FixedSessionAuthRepository.EMPLOYEE), settings)

        assertEquals(denied, manage.saveWaterRanges(customRanges))
        assertEquals(denied, manage.resetWaterRanges())
        assertEquals(denied, manage.saveMonitoringSchedule(customSchedule))
        assertEquals(denied, manage.resetMonitoringSchedule())

        assertEquals(0, settings.saveCalls)
        assertEquals(WaterRanges.DEFAULT, settings.waterRanges.first())
    }

    @Test
    fun `faixas ou agenda fora de ordem sao recusadas sem gravar`() = runBlocking {
        val settings = FakeSettingsRepository()
        val manage = ManageSettings(FixedSessionAuthRepository(FixedSessionAuthRepository.ADMIN), settings)

        val badRanges = WaterRanges(oxygenCriticalBelowMgL = 1.0, oxygenEmergencyBelowMgL = 2.0)
        val badSchedule = MonitoringSchedule(oxygenMorningHour = 18)

        assertEquals(UseCaseResult.Failure(UseCaseError.INVALID_VALUES), manage.saveWaterRanges(badRanges))
        assertEquals(UseCaseResult.Failure(UseCaseError.INVALID_VALUES), manage.saveMonitoringSchedule(badSchedule))
        assertEquals(0, settings.saveCalls)
    }
}
