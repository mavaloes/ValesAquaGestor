package com.valoes.valesaquagestor.domain.model

/**
 * Perfil do usuário na fazenda (`specs/06-perfis-permissoes.md`). Quem cria a conta vira
 * [ADMIN] da própria fazenda. As permissões em si só entram na Fase 08.
 */
enum class Papel {
    ADMIN,
    FUNCIONARIO,
}
