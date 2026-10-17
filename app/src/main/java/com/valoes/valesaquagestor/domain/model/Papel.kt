package com.valoes.valesaquagestor.domain.model

/**
 * Perfil do usuário na fazenda (`specs/06-perfis-permissoes.md`). Quem cria a conta vira
 * [ADMIN] da própria fazenda. O que cada perfil pode fazer está em `PermissionChecker`.
 */
enum class Papel {
    ADMIN,
    FUNCIONARIO,
}
