package com.valoes.valesaquagestor.domain.repository

import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

/**
 * Usuários da fazenda. As consultas nunca trazem registros excluídos. Quem pode alterar o quê é
 * decidido nos casos de uso (`InviteUser`, `ChangeUserRole`, `DeactivateUser`), não aqui.
 */
interface UserRepository {

    /** Todos os usuários, ordenados por nome. */
    fun observeAll(): Flow<List<Usuario>>

    /** Um usuário pelo e-mail (sem diferenciar maiúsculas), ou `null`. */
    suspend fun findByEmail(email: String): Usuario?

    /** Um usuário pelo `uuid`, ou `null`. */
    suspend fun get(uuid: String): Usuario?

    /**
     * Salva o usuário (novo se `id == 0`) e devolve o `uuid`. Gera o `uuid` quando vazio, normaliza o
     * e-mail e marca como não sincronizado.
     */
    suspend fun save(usuario: Usuario): String

    /** Garante que quem está logado existe na lista de usuários, com o perfil da [session]. */
    suspend fun ensureFromSession(session: Session)
}
