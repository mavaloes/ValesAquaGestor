package com.valoes.valesaquagestor.domain.permission

import com.valoes.valesaquagestor.domain.model.Papel

/**
 * Cada linha da tabela de `specs/06-perfis-permissoes.md` (a linha "alimentação e mortalidade" vale
 * para duas ações).
 */
enum class Permission {
    REGISTER_FEEDING,
    REGISTER_MORTALITY,
    REGISTER_WATER,
    REGISTER_BIOMETRY,

    /** Ver tanques, lotes e alertas. */
    VIEW_FARM,

    /** Convidar, trocar perfil e desativar usuários. */
    MANAGE_USERS,

    VIEW_COSTS,

    /** Editar faixas da água e agenda de medição. */
    MANAGE_SETTINGS,

    VIEW_REPORTS,
}

/**
 * Tabela de permissões por perfil (`specs/06-perfis-permissoes.md`). Kotlin puro: os casos de uso
 * chamam aqui antes de agir, então esconder o botão na tela nunca é a única barreira.
 */
object PermissionChecker {

    /** O que o FUNCIONARIO pode fazer; o ADMIN pode tudo. */
    private val employeePermissions = setOf(
        Permission.REGISTER_FEEDING,
        Permission.REGISTER_MORTALITY,
        Permission.REGISTER_WATER,
        Permission.REGISTER_BIOMETRY,
        Permission.VIEW_FARM,
    )

    /** `true` se o [papel] pode [permission]. Sem perfil (sem sessão), nada é permitido. */
    fun isAllowed(papel: Papel?, permission: Permission): Boolean = when (papel) {
        Papel.ADMIN -> true
        Papel.FUNCIONARIO -> permission in employeePermissions
        null -> false
    }
}
