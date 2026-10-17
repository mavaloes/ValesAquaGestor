package com.valoes.valesaquagestor.domain.repository

/** Quem está usando o app agora; os repositórios gravam isso em `criadoPorUserId` ao criar um registro. */
fun interface CurrentUserProvider {

    /** `userId` da sessão atual, ou `null` sem sessão. */
    suspend fun currentUserId(): String?
}
