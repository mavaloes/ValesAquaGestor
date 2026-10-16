package com.valoes.valesaquagestor.domain.usecase

import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.model.StatusUsuario
import com.valoes.valesaquagestor.domain.model.Usuario
import com.valoes.valesaquagestor.domain.permission.Permission
import com.valoes.valesaquagestor.domain.permission.PermissionChecker
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.ConnectivityChecker
import com.valoes.valesaquagestor.domain.repository.UserRepository
import com.valoes.valesaquagestor.domain.validation.AuthValidator
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Resultado do convite.
 *
 * @property pendingOffline `true` se o aparelho estava sem internet: o convite fica guardado e só
 * chega à pessoa quando houver conexão (a tela avisa).
 */
data class InviteOutcome(val usuario: Usuario, val pendingOffline: Boolean)

/** Sessão de quem está pedindo a ação, ou `null` sem login. Lida na hora, nunca guardada. */
private suspend fun AuthRepository.session(): Session? = currentSession.first()

/**
 * Convida um funcionário por e-mail (`specs/06-perfis-permissoes.md`). Só o ADMIN pode: a checagem
 * é feita aqui, no caso de uso, e não apenas escondendo o botão na tela.
 *
 * Sem internet o convite é guardado como pendente (`sincronizado = false`) e a tela avisa.
 */
class InviteUser @Inject constructor(
    private val auth: AuthRepository,
    private val users: UserRepository,
    private val connectivity: ConnectivityChecker,
) {
    suspend operator fun invoke(nome: String, email: String, papel: Papel): UseCaseResult<InviteOutcome> {
        if (!PermissionChecker.isAllowed(auth.session()?.papel, Permission.MANAGE_USERS)) {
            return UseCaseResult.Failure(UseCaseError.PERMISSION_DENIED)
        }
        if (nome.isBlank()) return UseCaseResult.Failure(UseCaseError.INVALID_NAME)
        if (AuthValidator.validateEmail(email) != null) return UseCaseResult.Failure(UseCaseError.INVALID_EMAIL)

        val existing = users.findByEmail(email)
        if (existing != null && existing.status != StatusUsuario.DESATIVADO) {
            return UseCaseResult.Failure(UseCaseError.EMAIL_ALREADY_IN_USE)
        }
        // Convidar de novo quem foi desativado reaproveita o cadastro dele (o e-mail é único).
        val invited = (existing ?: Usuario(nome = nome.trim(), email = email.trim(), papel = papel))
            .copy(nome = nome.trim(), papel = papel, status = StatusUsuario.CONVIDADO)
        val saved = invited.copy(uuid = users.save(invited))
        return UseCaseResult.Success(InviteOutcome(saved, pendingOffline = !connectivity.isOnline()))
    }
}

/** Troca o perfil de um usuário. Só o ADMIN; ninguém troca o próprio perfil. */
class ChangeUserRole @Inject constructor(
    private val auth: AuthRepository,
    private val users: UserRepository,
) {
    suspend operator fun invoke(uuid: String, papel: Papel): UseCaseResult<Unit> {
        val session = auth.session()
        if (!PermissionChecker.isAllowed(session?.papel, Permission.MANAGE_USERS)) {
            return UseCaseResult.Failure(UseCaseError.PERMISSION_DENIED)
        }
        if (uuid == session?.userId) return UseCaseResult.Failure(UseCaseError.CANNOT_CHANGE_SELF)
        val user = users.get(uuid) ?: return UseCaseResult.Failure(UseCaseError.USER_NOT_FOUND)
        users.save(user.copy(papel = papel))
        return UseCaseResult.Success(Unit)
    }
}

/**
 * Desativa um usuário: ele perde o acesso, mas os registros que criou continuam. Só o ADMIN;
 * ninguém desativa a si mesmo.
 */
class DeactivateUser @Inject constructor(
    private val auth: AuthRepository,
    private val users: UserRepository,
) {
    suspend operator fun invoke(uuid: String): UseCaseResult<Unit> {
        val session = auth.session()
        if (!PermissionChecker.isAllowed(session?.papel, Permission.MANAGE_USERS)) {
            return UseCaseResult.Failure(UseCaseError.PERMISSION_DENIED)
        }
        if (uuid == session?.userId) return UseCaseResult.Failure(UseCaseError.CANNOT_CHANGE_SELF)
        val user = users.get(uuid) ?: return UseCaseResult.Failure(UseCaseError.USER_NOT_FOUND)
        users.save(user.copy(status = StatusUsuario.DESATIVADO))
        return UseCaseResult.Success(Unit)
    }
}
