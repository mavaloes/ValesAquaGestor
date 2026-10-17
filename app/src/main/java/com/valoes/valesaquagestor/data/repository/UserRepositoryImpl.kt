package com.valoes.valesaquagestor.data.repository

import com.valoes.valesaquagestor.data.local.dao.UsuarioDao
import com.valoes.valesaquagestor.data.local.database.TransactionRunner
import com.valoes.valesaquagestor.data.local.entity.UsuarioEntity
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.model.StatusUsuario
import com.valoes.valesaquagestor.domain.model.Usuario
import com.valoes.valesaquagestor.domain.repository.UserRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl @Inject constructor(
    private val dao: UsuarioDao,
    private val transaction: TransactionRunner,
    private val clock: Clock,
) : UserRepository {

    override fun observeAll(): Flow<List<Usuario>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun findByEmail(email: String): Usuario? = dao.findByEmail(normalize(email))?.toDomain()

    override suspend fun get(uuid: String): Usuario? = dao.findByUuid(uuid)?.takeUnless { it.excluido }?.toDomain()

    override suspend fun save(usuario: Usuario): String {
        val now = clock.millis()
        val email = normalize(usuario.email)
        val existing = when {
            usuario.id != 0L -> requireNotNull(dao.getByIdIncludingDeleted(usuario.id)) { "Usuário ${usuario.id} não existe." }
            usuario.uuid.isNotEmpty() -> dao.findByUuid(usuario.uuid)
            else -> null
        }
        if (existing == null) {
            val uuid = usuario.uuid.ifEmpty { UUID.randomUUID().toString() }
            dao.insert(usuario.toEntity(null, uuid, now))
            return uuid
        }
        dao.update(usuario.copy(email = email).toEntity(existing, existing.uuid, now))
        return existing.uuid
    }

    override suspend fun ensureFromSession(session: Session) {
        transaction.run {
            val byId = dao.findByUuid(session.userId)
            val current = byId ?: dao.findByEmail(normalize(session.email))
            // A sessão manda no perfil e no nome de quem está logado.
            val synced = Usuario(
                id = current?.id ?: 0,
                uuid = current?.uuid ?: session.userId,
                nome = session.nome,
                email = session.email,
                papel = session.papel,
                status = StatusUsuario.ATIVO,
            )
            if (current == null) {
                dao.insert(synced.toEntity(null, session.userId, clock.millis()))
            } else if (current.toDomain().copy(sincronizado = false) != synced.copy(uuid = current.uuid)) {
                dao.update(synced.toEntity(current, current.uuid, clock.millis()))
            }
        }
    }

    private fun normalize(email: String) = email.trim().lowercase()

    private fun Usuario.toEntity(base: UsuarioEntity?, uuid: String, now: Long) = UsuarioEntity(
        id = base?.id ?: 0, nome = nome.trim(), email = normalize(email), papel = papel, status = status,
        uuid = uuid, atualizadoEm = now, excluido = base?.excluido ?: false, sincronizado = false,
    )

    private fun UsuarioEntity.toDomain() = Usuario(
        id = id, uuid = uuid, nome = nome, email = email, papel = papel, status = status, sincronizado = sincronizado,
    )
}
