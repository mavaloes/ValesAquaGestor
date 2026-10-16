package com.valoes.valesaquagestor.data.session

import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.CurrentUserProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** O usuário atual é o dono da sessão salva no aparelho (funciona sem internet). */
class SessionCurrentUserProvider @Inject constructor(
    private val auth: AuthRepository,
) : CurrentUserProvider {

    override suspend fun currentUserId(): String? = auth.currentSession.first()?.userId
}
