package com.valoes.valesaquagestor.settings

import com.valoes.valesaquagestor.domain.calc.MonitoringSchedule
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.model.Usuario
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.AuthResult
import com.valoes.valesaquagestor.domain.repository.PendingSyncRepository
import com.valoes.valesaquagestor.domain.repository.SettingsRepository
import com.valoes.valesaquagestor.domain.repository.UserRepository
import com.valoes.valesaquagestor.domain.rules.WaterRanges
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Faixas e agenda guardadas só na memória. */
class FakeSettingsRepository(
    ranges: WaterRanges = WaterRanges.DEFAULT,
    schedule: MonitoringSchedule = MonitoringSchedule(),
) : SettingsRepository {
    private val rangesState = MutableStateFlow(ranges)
    private val scheduleState = MutableStateFlow(schedule)
    var saveCalls = 0
        private set

    override val waterRanges: Flow<WaterRanges> = rangesState
    override val monitoringSchedule: Flow<MonitoringSchedule> = scheduleState

    override suspend fun saveWaterRanges(ranges: WaterRanges) {
        saveCalls++
        rangesState.value = ranges
    }

    override suspend fun resetWaterRanges() {
        saveCalls++
        rangesState.value = WaterRanges.DEFAULT
    }

    override suspend fun saveMonitoringSchedule(schedule: MonitoringSchedule) {
        saveCalls++
        scheduleState.value = schedule
    }

    override suspend fun resetMonitoringSchedule() {
        saveCalls++
        scheduleState.value = MonitoringSchedule()
    }
}

/** Usuários guardados só na memória. Imita o repositório real: gera `id` e `uuid` e normaliza o e-mail. */
class FakeUserRepository(initial: List<Usuario> = emptyList()) : UserRepository {
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1
    private val state = MutableStateFlow(initial.map { if (it.id == 0L) it.copy(id = nextId++) else it })

    val saved: List<Usuario> get() = state.value

    override fun observeAll(): Flow<List<Usuario>> = state.map { list -> list.sortedBy { it.nome.lowercase() } }

    override suspend fun findByEmail(email: String): Usuario? =
        state.value.firstOrNull { it.email == email.trim().lowercase() }

    override suspend fun get(uuid: String): Usuario? = state.value.firstOrNull { it.uuid == uuid }

    override suspend fun save(usuario: Usuario): String {
        val existing = when {
            usuario.id != 0L -> state.value.firstOrNull { it.id == usuario.id }
            usuario.uuid.isNotEmpty() -> state.value.firstOrNull { it.uuid == usuario.uuid }
            else -> null
        }
        val stored = if (existing == null) {
            usuario.copy(id = nextId++, uuid = usuario.uuid.ifEmpty { UUID.randomUUID().toString() }, sincronizado = false)
        } else {
            usuario.copy(uuid = existing.uuid, sincronizado = false)
        }.copy(email = usuario.email.trim().lowercase())
        state.value = state.value.filterNot { it.id == stored.id } + stored
        return stored.uuid
    }

    override suspend fun ensureFromSession(session: Session) {
        if (state.value.any { it.uuid == session.userId }) return
        save(Usuario(uuid = session.userId, nome = session.nome, email = session.email, papel = session.papel))
    }
}

/** [AuthRepository] com sessão fixa, para testar casos de uso e ViewModels. Registra os `signOut`. */
class FixedSessionAuthRepository(session: Session?) : AuthRepository {
    private val state = MutableStateFlow(session)
    var signOutCalls = 0
        private set

    override val currentSession: Flow<Session?> = state

    override suspend fun signIn(email: String, password: String): AuthResult = AuthResult.Success

    override suspend fun signUp(nome: String, email: String, password: String): AuthResult = AuthResult.Success

    override suspend fun sendPasswordReset(email: String): AuthResult = AuthResult.Success

    override suspend fun signOut() {
        signOutCalls++
        state.value = null
    }

    companion object {
        val ADMIN = Session("admin-1", "Maria Silva", "maria@exemplo.com", Papel.ADMIN)
        val EMPLOYEE = Session("func-1", "João Souza", "joao@exemplo.com", Papel.FUNCIONARIO)
    }
}

/** Número de registros pendentes controlado pelo teste. */
class FakePendingSyncRepository(count: Int = 0) : PendingSyncRepository {
    private val state = MutableStateFlow(count)
    var count: Int
        get() = state.value
        set(value) {
            state.value = value
        }

    override fun observePendingCount(): Flow<Int> = state
}
