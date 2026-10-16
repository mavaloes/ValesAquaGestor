package com.valoes.valesaquagestor.auth

import com.valoes.valesaquagestor.data.session.SessionCipher
import com.valoes.valesaquagestor.data.session.SessionStore
import com.valoes.valesaquagestor.domain.model.Session
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.AuthResult
import com.valoes.valesaquagestor.domain.repository.ConnectivityChecker
import java.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Troca o `Dispatchers.Main` por um dispatcher de teste (o `viewModelScope` usa o Main). */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Sessão guardada só na memória. */
class FakeSessionStore(initial: Session? = null) : SessionStore {
    private val state = MutableStateFlow(initial)
    override val session: Flow<Session?> = state
    override suspend fun save(session: Session) { state.value = session }
    override suspend fun clear() { state.value = null }
}

/** Conexão controlada pelo teste. */
class FakeConnectivityChecker(var online: Boolean = true) : ConnectivityChecker {
    override fun isOnline() = online
}

/** "Criptografia" reversível só para teste: Base64 com prefixo. Falha em texto que não foi cifrado por ela. */
class FakeSessionCipher : SessionCipher {
    override fun encrypt(plainText: String) = PREFIX + Base64.getEncoder().encodeToString(plainText.toByteArray())
    override fun decrypt(cipherText: String): String {
        require(cipherText.startsWith(PREFIX)) { "texto não cifrado" }
        return String(Base64.getDecoder().decode(cipherText.removePrefix(PREFIX)))
    }

    companion object {
        const val PREFIX = "enc:"
    }
}

/**
 * [AuthRepository] roteirizado para os testes de ViewModel: registra as chamadas e devolve o
 * resultado combinado. Com [gate], a chamada fica suspensa até o teste liberar (simula a demora da rede).
 */
class ScriptedAuthRepository(
    var result: AuthResult = AuthResult.Success,
    var gate: CompletableDeferred<Unit>? = null,
) : AuthRepository {

    data class SignInCall(val email: String, val password: String)
    data class SignUpCall(val nome: String, val email: String, val password: String)

    val signInCalls = mutableListOf<SignInCall>()
    val signUpCalls = mutableListOf<SignUpCall>()
    val resetCalls = mutableListOf<String>()

    override val currentSession: Flow<Session?> = MutableStateFlow(null)

    override suspend fun signIn(email: String, password: String): AuthResult {
        signInCalls += SignInCall(email, password)
        gate?.await()
        return result
    }

    override suspend fun signUp(nome: String, email: String, password: String): AuthResult {
        signUpCalls += SignUpCall(nome, email, password)
        gate?.await()
        return result
    }

    override suspend fun sendPasswordReset(email: String): AuthResult {
        resetCalls += email
        gate?.await()
        return result
    }

    override suspend fun signOut() = Unit
}
