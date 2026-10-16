package com.valoes.valesaquagestor.domain.model

/**
 * Situação de um usuário da fazenda.
 *
 * @property ATIVO tem acesso ao app.
 * @property CONVIDADO foi convidado pelo ADMIN e ainda não criou a conta.
 * @property DESATIVADO perdeu o acesso; o histórico dos registros que criou continua.
 */
enum class StatusUsuario {
    ATIVO,
    CONVIDADO,
    DESATIVADO,
}

/**
 * Pessoa que usa o app na fazenda (`specs/06-perfis-permissoes.md`).
 *
 * O [uuid] é o mesmo `userId` da [Session] e o valor gravado em `criadoPorUserId` nos registros.
 *
 * @property id chave local; 0 para um usuário ainda não salvo.
 * @property uuid identidade do usuário; vazio até o primeiro salvamento.
 * @property papel perfil de acesso.
 * @property sincronizado `false` enquanto houver alteração (ex.: convite) ainda não enviada ao servidor.
 */
data class Usuario(
    val id: Long = 0,
    val uuid: String = "",
    val nome: String,
    val email: String,
    val papel: Papel,
    val status: StatusUsuario = StatusUsuario.ATIVO,
    val sincronizado: Boolean = false,
)
