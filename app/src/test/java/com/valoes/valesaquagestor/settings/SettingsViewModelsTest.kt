package com.valoes.valesaquagestor.settings

import com.valoes.valesaquagestor.auth.FakeConnectivityChecker
import com.valoes.valesaquagestor.auth.MainDispatcherRule
import com.valoes.valesaquagestor.domain.calc.MonitoringSchedule
import com.valoes.valesaquagestor.domain.calc.ScheduleField
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.model.StatusUsuario
import com.valoes.valesaquagestor.domain.model.Usuario
import com.valoes.valesaquagestor.domain.rules.WaterRangeField
import com.valoes.valesaquagestor.domain.rules.WaterRanges
import com.valoes.valesaquagestor.domain.usecase.ChangeUserRole
import com.valoes.valesaquagestor.domain.usecase.DeactivateUser
import com.valoes.valesaquagestor.domain.usecase.InviteUser
import com.valoes.valesaquagestor.domain.usecase.ManageSettings
import com.valoes.valesaquagestor.domain.usecase.UseCaseError
import com.valoes.valesaquagestor.presentation.main.MoreViewModel
import com.valoes.valesaquagestor.presentation.settings.RangeFieldError
import com.valoes.valesaquagestor.presentation.settings.ScheduleFieldError
import com.valoes.valesaquagestor.presentation.settings.ScheduleViewModel
import com.valoes.valesaquagestor.presentation.settings.SettingsMessage
import com.valoes.valesaquagestor.presentation.settings.SettingsViewModel
import com.valoes.valesaquagestor.presentation.settings.WaterRangesViewModel
import com.valoes.valesaquagestor.presentation.users.InviteEmailError
import com.valoes.valesaquagestor.presentation.users.UsersMessage
import com.valoes.valesaquagestor.presentation.users.UsersViewModel
import com.valoes.valesaquagestor.presentation.util.FormError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** ViewModels da Fase 08: Configurações (com o aviso de pendências no "Sair"), Mais, Usuários, Faixas e Agenda. */
class SettingsViewModelsTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val admin = FixedSessionAuthRepository.ADMIN
    private val employee = FixedSessionAuthRepository.EMPLOYEE

    // ---- Configurações e logout ----

    @Test
    fun `o ADMIN ve os ajustes da fazenda e o FUNCIONARIO so a conta`() = runBlocking {
        val adminState = SettingsViewModel(FixedSessionAuthRepository(admin), FakePendingSyncRepository()).state.first { it.session != null }
        val employeeState = SettingsViewModel(FixedSessionAuthRepository(employee), FakePendingSyncRepository()).state.first { it.session != null }

        assertTrue(adminState.canManageSettings)
        assertFalse(employeeState.canManageSettings)
        assertEquals(employee.nome, employeeState.session!!.nome)
    }

    @Test
    fun `o aviso de sair mostra quantos registros ainda nao foram enviados`() = runBlocking {
        val pending = FakePendingSyncRepository(count = 3)
        val auth = FixedSessionAuthRepository(admin)
        val viewModel = SettingsViewModel(auth, pending)

        viewModel.onSignOutClick()
        val state = viewModel.state.first { it.showSignOutConfirm && it.session != null }

        assertEquals(3, state.pendingCount)
        assertEquals(0, auth.signOutCalls)

        pending.count = 0
        assertEquals(0, viewModel.state.first { it.pendingCount == 0 }.pendingCount)
    }

    @Test
    fun `cancelar o aviso nao encerra a sessao`() = runBlocking {
        val auth = FixedSessionAuthRepository(admin)
        val viewModel = SettingsViewModel(auth, FakePendingSyncRepository(count = 2))

        viewModel.onSignOutClick()
        viewModel.onSignOutDismiss()

        assertFalse(viewModel.state.first { it.session != null }.showSignOutConfirm)
        assertEquals(0, auth.signOutCalls)
    }

    @Test
    fun `confirmar o aviso encerra a sessao uma vez e fecha o aviso`() = runBlocking {
        val auth = FixedSessionAuthRepository(admin)
        val viewModel = SettingsViewModel(auth, FakePendingSyncRepository(count = 2))

        viewModel.onSignOutClick()
        viewModel.onSignOutConfirm()

        assertEquals(1, auth.signOutCalls)
        val state = viewModel.state.first { it.session == null }
        assertFalse(state.showSignOutConfirm)
        assertEquals(2, state.pendingCount)
    }

    // ---- Mais ----

    @Test
    fun `o menu Mais so mostra Usuarios para o ADMIN`() = runBlocking {
        assertTrue(MoreViewModel(FixedSessionAuthRepository(admin)).state.first { it.canManageUsers }.canManageUsers)
        assertFalse(MoreViewModel(FixedSessionAuthRepository(employee)).state.first().canManageUsers)
        assertFalse(MoreViewModel(FixedSessionAuthRepository(null)).state.first().canManageUsers)
    }

    // ---- Usuários ----

    private class UsersFixture(session: Session?, initial: List<Usuario> = emptyList(), online: Boolean = true) {
        val auth = FixedSessionAuthRepository(session)
        val users = FakeUserRepository(initial)
        val connectivity = FakeConnectivityChecker(online)
        val viewModel = UsersViewModel(
            auth, users,
            InviteUser(auth, users, connectivity), ChangeUserRole(auth, users), DeactivateUser(auth, users),
        )
    }

    private fun other(status: StatusUsuario = StatusUsuario.ATIVO) =
        Usuario(uuid = "func-2", nome = "Ana", email = "ana@exemplo.com", papel = Papel.FUNCIONARIO, status = status)

    @Test
    fun `a lista de usuarios inclui quem esta logado, marcado como voce`() = runBlocking {
        val f = UsersFixture(admin, listOf(other()))

        val state = f.viewModel.state.first { it.users.size == 2 }

        assertTrue(state.allowed)
        assertEquals(setOf(admin.userId), state.users.filter { it.isCurrentUser }.map { it.usuario.uuid }.toSet())
    }

    @Test
    fun `FUNCIONARIO nao tem permissao na tela de usuarios`() = runBlocking {
        val f = UsersFixture(employee)

        assertFalse(f.viewModel.state.first { !it.isLoading }.allowed)
    }

    @Test
    fun `convite sem nome mostra o erro no campo e nao chama o caso de uso`() = runBlocking {
        val f = UsersFixture(admin)

        f.viewModel.onInviteClick()
        f.viewModel.onInviteEmailChange("joao@exemplo.com")
        f.viewModel.onInviteSend()

        assertEquals(FormError.REQUIRED, f.viewModel.state.first { it.invite != null }.invite!!.nomeError)
        assertTrue(f.users.saved.none { it.email == "joao@exemplo.com" })
    }

    @Test
    fun `convite com e-mail invalido ou repetido mostra o erro no campo e-mail`() = runBlocking {
        val f = UsersFixture(admin, listOf(other()))
        f.viewModel.onInviteClick()
        f.viewModel.onInviteNameChange("João")

        f.viewModel.onInviteEmailChange("joao@")
        f.viewModel.onInviteSend()
        assertEquals(InviteEmailError.INVALID, f.viewModel.state.first { it.invite != null }.invite!!.emailError)

        f.viewModel.onInviteEmailChange("ana@exemplo.com")
        f.viewModel.onInviteSend()
        assertEquals(InviteEmailError.IN_USE, f.viewModel.state.first { it.invite != null }.invite!!.emailError)
    }

    @Test
    fun `convite com internet fecha o formulario e confirma o envio`() = runBlocking {
        val f = UsersFixture(admin)
        f.viewModel.onInviteClick()
        f.viewModel.onInviteNameChange("João")
        f.viewModel.onInviteEmailChange("joao@exemplo.com")

        f.viewModel.onInviteSend()

        val state = f.viewModel.state.first { it.message != null }
        assertNull(state.invite)
        assertEquals(UsersMessage.InviteCreated("joao@exemplo.com"), state.message)
        assertEquals(StatusUsuario.CONVIDADO, f.users.saved.single { it.email == "joao@exemplo.com" }.status)
        f.viewModel.onMessageShown()
        assertNull(f.viewModel.state.first().message)
    }

    @Test
    fun `convite sem internet fica pendente e avisa`() = runBlocking {
        val f = UsersFixture(admin, online = false)
        f.viewModel.onInviteClick()
        f.viewModel.onInviteNameChange("João")
        f.viewModel.onInviteEmailChange("joao@exemplo.com")

        f.viewModel.onInviteSend()

        assertEquals(UsersMessage.InvitePendingOffline, f.viewModel.state.first { it.message != null }.message)
        assertTrue(f.users.saved.any { it.email == "joao@exemplo.com" && !it.sincronizado })
    }

    @Test
    fun `FUNCIONARIO que chega a enviar um convite recebe a negacao do caso de uso`() = runBlocking {
        val f = UsersFixture(employee)
        f.viewModel.onInviteClick()
        f.viewModel.onInviteNameChange("Pedro")
        f.viewModel.onInviteEmailChange("pedro@exemplo.com")

        f.viewModel.onInviteSend()

        assertEquals(UsersMessage.Failed(UseCaseError.PERMISSION_DENIED), f.viewModel.state.first { it.message != null }.message)
        assertTrue(f.users.saved.none { it.email == "pedro@exemplo.com" })
    }

    @Test
    fun `trocar o perfil e desativar passam pelos casos de uso`() = runBlocking {
        val f = UsersFixture(admin, listOf(other()))
        val row = f.viewModel.state.first { it.users.any { r -> r.usuario.uuid == "func-2" } }.users.first { it.usuario.uuid == "func-2" }

        f.viewModel.onToggleRole(row.usuario)
        assertEquals(UsersMessage.RoleChanged, f.viewModel.state.first { it.message != null }.message)
        assertEquals(Papel.ADMIN, f.users.get("func-2")!!.papel)
        f.viewModel.onMessageShown()

        f.viewModel.onDeactivateClick(f.users.get("func-2")!!)
        assertEquals("func-2", f.viewModel.state.first { it.deactivating != null }.deactivating!!.uuid)
        f.viewModel.onDeactivateConfirm()

        assertEquals(UsersMessage.Deactivated, f.viewModel.state.first { it.message != null }.message)
        assertEquals(StatusUsuario.DESATIVADO, f.users.get("func-2")!!.status)
    }

    @Test
    fun `o ADMIN nao troca o proprio perfil nem pela tela`() = runBlocking {
        val f = UsersFixture(admin)
        val me = f.viewModel.state.first { it.users.isNotEmpty() }.users.single { it.isCurrentUser }.usuario

        f.viewModel.onToggleRole(me)

        assertEquals(UsersMessage.Failed(UseCaseError.CANNOT_CHANGE_SELF), f.viewModel.state.first { it.message != null }.message)
        assertEquals(Papel.ADMIN, f.users.get(admin.userId)!!.papel)
    }

    // ---- Faixas da água ----

    private fun rangesVm(session: Session, settings: FakeSettingsRepository = FakeSettingsRepository()): Pair<WaterRangesViewModel, FakeSettingsRepository> {
        val auth = FixedSessionAuthRepository(session)
        return WaterRangesViewModel(auth, settings, ManageSettings(auth, settings)) to settings
    }

    @Test
    fun `a tela de faixas abre com os valores de fabrica`() = runBlocking {
        val (vm, _) = rangesVm(admin)

        val state = vm.state.first { !it.isLoading }

        assertTrue(state.allowed)
        assertEquals("4", state.values.getValue(WaterRangeField.OXYGEN_ADEQUATE_MIN))
        assertEquals("0,05", state.values.getValue(WaterRangeField.AMMONIA_ADEQUATE_BELOW))
        assertEquals(WaterRangeField.entries.size, state.values.size)
        assertTrue(state.canSave)
    }

    @Test
    fun `faixas fora de ordem mostram o erro no campo e nao gravam`() = runBlocking {
        val (vm, settings) = rangesVm(admin)
        vm.state.first { !it.isLoading }

        vm.onValueChange(WaterRangeField.OXYGEN_CRITICAL_BELOW, "1")
        vm.onSave()

        val state = vm.state.first { it.errors.isNotEmpty() }
        assertEquals(RangeFieldError.ORDER, state.errors.getValue(WaterRangeField.OXYGEN_CRITICAL_BELOW))
        assertEquals(0, settings.saveCalls)
    }

    @Test
    fun `texto que nao e numero mostra erro de numero invalido`() = runBlocking {
        val (vm, settings) = rangesVm(admin)
        vm.state.first { !it.isLoading }

        vm.onValueChange(WaterRangeField.PH_ADEQUATE_MIN, "abc")
        vm.onSave()

        assertEquals(RangeFieldError.INVALID_NUMBER, vm.state.first { it.errors.isNotEmpty() }.errors.getValue(WaterRangeField.PH_ADEQUATE_MIN))
        assertEquals(0, settings.saveCalls)
    }

    @Test
    fun `faixas validas sao salvas com virgula decimal e a mensagem confirma`() = runBlocking {
        val (vm, settings) = rangesVm(admin)
        vm.state.first { !it.isLoading }

        vm.onValueChange(WaterRangeField.OXYGEN_ADEQUATE_MIN, "4,5")
        vm.onSave()

        assertEquals(SettingsMessage.Saved, vm.state.first { it.message != null }.message)
        assertEquals(4.5, settings.waterRanges.first().oxygenAdequateMinMgL, 0.0)
        vm.onMessageShown()
        assertNull(vm.state.first().message)
    }

    @Test
    fun `restaurar volta os campos e os ajustes aos valores de fabrica`() = runBlocking {
        val (vm, settings) = rangesVm(admin, FakeSettingsRepository(ranges = WaterRanges(oxygenAdequateMinMgL = 5.0)))
        assertEquals("5", vm.state.first { !it.isLoading }.values.getValue(WaterRangeField.OXYGEN_ADEQUATE_MIN))

        vm.onRestoreClick()
        assertTrue(vm.state.first().showRestoreConfirm)
        vm.onRestoreConfirm()

        val state = vm.state.first { it.message != null }
        assertEquals(SettingsMessage.Restored, state.message)
        assertEquals("4", state.values.getValue(WaterRangeField.OXYGEN_ADEQUATE_MIN))
        assertEquals(WaterRanges.DEFAULT, settings.waterRanges.first())
    }

    @Test
    fun `FUNCIONARIO nao edita as faixas`() = runBlocking {
        val (vm, settings) = rangesVm(employee)

        val state = vm.state.first { !it.isLoading }
        vm.onSave()

        assertFalse(state.allowed)
        assertFalse(state.canSave)
        assertEquals(0, settings.saveCalls)
    }

    // ---- Agenda de medições ----

    private fun scheduleVm(session: Session, settings: FakeSettingsRepository = FakeSettingsRepository()): Pair<ScheduleViewModel, FakeSettingsRepository> {
        val auth = FixedSessionAuthRepository(session)
        return ScheduleViewModel(auth, settings, ManageSettings(auth, settings)) to settings
    }

    @Test
    fun `a agenda abre com os padroes e salva os ajustes`() = runBlocking {
        val (vm, settings) = scheduleVm(admin)
        val initial = vm.state.first { !it.isLoading }
        assertEquals("6", initial.values.getValue(ScheduleField.OXYGEN_MORNING_HOUR))

        vm.onValueChange(ScheduleField.OXYGEN_MORNING_HOUR, "5")
        vm.onWeeklyDayChange(java.time.DayOfWeek.TUESDAY)
        vm.onSave()

        assertEquals(SettingsMessage.Saved, vm.state.first { it.message != null }.message)
        val saved = settings.monitoringSchedule.first()
        assertEquals(5, saved.oxygenMorningHour)
        assertEquals(java.time.DayOfWeek.TUESDAY, saved.weeklyDay)
    }

    @Test
    fun `agenda com hora impossivel ou numero invalido mostra erro e nao grava`() = runBlocking {
        val (vm, settings) = scheduleVm(admin)
        vm.state.first { !it.isLoading }

        vm.onValueChange(ScheduleField.OXYGEN_MORNING_HOUR, "25")
        vm.onValueChange(ScheduleField.ENGORDA_BIOMETRY_INTERVAL, "x")
        vm.onSave()
        assertEquals(
            ScheduleFieldError.INVALID_NUMBER,
            vm.state.first { it.errors.isNotEmpty() }.errors.getValue(ScheduleField.ENGORDA_BIOMETRY_INTERVAL),
        )

        vm.onValueChange(ScheduleField.ENGORDA_BIOMETRY_INTERVAL, "30")
        vm.onSave()
        assertEquals(
            ScheduleFieldError.INVALID_VALUE,
            vm.state.first { it.errors.isNotEmpty() }.errors.getValue(ScheduleField.OXYGEN_MORNING_HOUR),
        )
        assertEquals(0, settings.saveCalls)
    }

    @Test
    fun `restaurar a agenda volta aos padroes`() = runBlocking {
        val (vm, settings) = scheduleVm(admin, FakeSettingsRepository(schedule = MonitoringSchedule(oxygenMorningHour = 5)))
        assertEquals("5", vm.state.first { !it.isLoading }.values.getValue(ScheduleField.OXYGEN_MORNING_HOUR))

        vm.onRestoreConfirm()

        assertEquals("6", vm.state.first { it.message != null }.values.getValue(ScheduleField.OXYGEN_MORNING_HOUR))
        assertEquals(MonitoringSchedule(), settings.monitoringSchedule.first())
    }

    @Test
    fun `FUNCIONARIO nao edita a agenda`() = runBlocking {
        val (vm, settings) = scheduleVm(employee)

        assertFalse(vm.state.first { !it.isLoading }.allowed)
        vm.onSave()
        assertEquals(0, settings.saveCalls)
    }
}
