package com.valoes.valesaquagestor.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.StatusUsuario

/**
 * Usuário da fazenda (`specs/06-perfis-permissoes.md`). O [uuid] é o `userId` da sessão e o valor
 * gravado em `criadoPorUserId` nos registros.
 *
 * O e-mail é único (sempre em minúsculas): convidar de novo alguém desativado reaproveita a linha.
 * Nunca guarda senha.
 *
 * @property nome nome de quem usa o app.
 * @property email e-mail de login, em minúsculas e sem espaços.
 * @property papel perfil de acesso.
 * @property status se está ativo, convidado ou desativado.
 * @property uuid identidade do registro na sincronização; único no banco.
 * @property atualizadoEm última alteração, em epoch millis; na sincronização vence o maior.
 * @property excluido exclusão lógica: some das listas, mas fica no banco até o servidor confirmar.
 * @property sincronizado `false` enquanto houver alteração (ex.: convite) ainda não enviada ao servidor.
 */
@Entity(
    tableName = "usuario",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["email"], unique = true),
    ],
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
    val nome: String,
    val email: String,
    val papel: Papel,
    val status: StatusUsuario,
    override val uuid: String,
    override val atualizadoEm: Long,
    override val excluido: Boolean = false,
    override val sincronizado: Boolean = false,
) : SyncEntity
