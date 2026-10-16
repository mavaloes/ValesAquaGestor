package com.valoes.valesaquagestor.presentation.settings

import androidx.annotation.StringRes
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.StatusUsuario
import com.valoes.valesaquagestor.domain.usecase.UseCaseError

/** Nome do perfil para o usuário. */
@StringRes
fun Papel.labelRes(): Int = when (this) {
    Papel.ADMIN -> R.string.role_admin
    Papel.FUNCIONARIO -> R.string.role_employee
}

/** Situação do usuário para o usuário. */
@StringRes
fun StatusUsuario.labelRes(): Int = when (this) {
    StatusUsuario.ATIVO -> R.string.user_status_active
    StatusUsuario.CONVIDADO -> R.string.user_status_invited
    StatusUsuario.DESATIVADO -> R.string.user_status_deactivated
}

/** Mensagem de um caso de uso que não fez o que foi pedido. */
@StringRes
fun UseCaseError.messageRes(): Int = when (this) {
    UseCaseError.PERMISSION_DENIED -> R.string.no_permission
    UseCaseError.INVALID_EMAIL -> R.string.error_invalid_email
    UseCaseError.INVALID_NAME -> R.string.error_required
    UseCaseError.EMAIL_ALREADY_IN_USE -> R.string.users_error_email_in_use
    UseCaseError.USER_NOT_FOUND -> R.string.users_error_not_found
    UseCaseError.CANNOT_CHANGE_SELF -> R.string.users_error_self
    UseCaseError.INVALID_VALUES -> R.string.error_range_order
}
