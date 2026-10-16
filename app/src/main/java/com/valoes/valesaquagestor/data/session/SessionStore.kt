package com.valoes.valesaquagestor.data.session

import com.valoes.valesaquagestor.domain.model.Session
import kotlinx.coroutines.flow.Flow

/** Guarda a sessão no aparelho, criptografada. Nunca guarda senha. */
interface SessionStore {

    /** Sessão salva, ou `null`. Funciona sem internet. Sessão ilegível (chave perdida) conta como `null`. */
    val session: Flow<Session?>

    suspend fun save(session: Session)

    suspend fun clear()
}
