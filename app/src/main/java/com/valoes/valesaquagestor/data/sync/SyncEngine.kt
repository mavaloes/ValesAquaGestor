package com.valoes.valesaquagestor.data.sync

import android.database.sqlite.SQLiteException
import com.valoes.valesaquagestor.domain.sync.SyncPhase
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Marca onde cada tabela parou de baixar (`servidor_em` da última mudança vista). */
interface SyncCursorStore {
    suspend fun get(table: String): String?
    suspend fun set(table: String, cursor: String)
}

/** Atualiza a sessão com o que o servidor diz de quem está logado (papel trocado pelo ADMIN, conta desativada). */
fun interface OwnProfileRefresher {
    suspend fun refresh()
}

/** O que a sincronização está fazendo, visto de fora (indicador da tela). */
@Singleton
class SyncStateHolder @Inject constructor() {
    private val _phase = MutableStateFlow(SyncPhase.IDLE)
    val phase: Flow<SyncPhase> = _phase.asStateFlow()

    internal fun set(phase: SyncPhase) {
        _phase.value = phase
    }
}

/** Como terminou uma rodada de sincronização. */
enum class SyncOutcome {
    /** Tudo enviado e baixado. */
    SUCCESS,

    /** Sem conexão ou erro do servidor: o WorkManager tenta de novo com espera crescente. */
    RETRY,

    /** A sessão não vale mais: é preciso entrar de novo. Os dados locais ficam intactos. */
    AUTH_EXPIRED,
}

/**
 * Sincronização completa (`specs/05-offline-sync.md`), tabela por tabela, na ordem dos pais para os filhos:
 *
 * 1. **Envia** os registros com `sincronizado = false`, em lotes. Só marca como enviado o que o servidor
 *    confirmou; se a conexão cair no meio, o resto continua pendente e o reenvio é inofensivo
 *    (o servidor deduplica por `uuid`, caso G1).
 * 2. **Baixa** as mudanças dos outros aparelhos e aplica por `uuid`; em conflito vence o maior
 *    `atualizadoEm` (caso G2).
 * 3. **Limpa** os registros excluídos que o servidor já confirmou (caso G3): até lá, ficam no banco.
 */
@Singleton
class SyncEngine @Inject constructor(
    private val tables: List<@JvmSuppressWildcards SyncTable>,
    private val remote: RemoteSyncApi,
    private val cursors: SyncCursorStore,
    private val state: SyncStateHolder,
    private val profile: OwnProfileRefresher,
) {
    private val running = Mutex()

    suspend fun run(): SyncOutcome = running.withLock {
        state.set(SyncPhase.RUNNING)
        val outcome = try {
            tables.forEach { push(it) }
            tables.forEach { pull(it) }
            profile.refresh()
            tables.asReversed().forEach { it.purgeConfirmedDeleted() }
            SyncOutcome.SUCCESS
        } catch (e: SyncException) {
            if (e.failure == SyncFailure.AUTH) SyncOutcome.AUTH_EXPIRED else SyncOutcome.RETRY
        } catch (e: CancellationException) {
            state.set(SyncPhase.IDLE)
            throw e
        } catch (e: Exception) {
            state.set(SyncPhase.FAILED)
            throw e
        }
        state.set(if (outcome == SyncOutcome.RETRY) SyncPhase.FAILED else SyncPhase.IDLE)
        outcome
    }

    private suspend fun push(table: SyncTable) {
        table.pendingRows().chunked(PUSH_BATCH_SIZE).forEach { batch ->
            remote.push(table.remoteName, batch.map { it.json })
            // Só depois da confirmação do servidor. Se a edição mudou o registro no meio do caminho,
            // o `markSynced` não o marca e ele vai de novo na próxima rodada.
            batch.forEach { table.markSynced(it) }
        }
    }

    private suspend fun pull(table: SyncTable) {
        var cursor = cursors.get(table.remoteName)
        // Na primeira página volta um pouco no tempo: um registro gravado no servidor "no mesmo instante"
        // pode ter sido confirmado depois da nossa última leitura. Reaplicar o que já temos é inofensivo.
        var after = cursor?.let(::rewind)
        while (true) {
            val page = remote.pull(table.remoteName, after, PULL_PAGE_SIZE)
            page.rows.forEach { row ->
                // Um registro que o banco local recusa (ex.: e-mail repetido) não pode travar todos os outros.
                try {
                    table.applyRemote(row)
                } catch (_: SQLiteException) {
                    // ignorado: o registro fica só no servidor
                }
            }
            val last = page.lastCursor
            if (last != null) {
                cursor = last
                cursors.set(table.remoteName, last)
            }
            if (page.rows.size < PULL_PAGE_SIZE || last == null) break
            after = last
        }
    }

    internal companion object {
        /** Registros por envio: pedidos pequenos caem menos quando a conexão é ruim. */
        const val PUSH_BATCH_SIZE = 100

        /** Registros por página baixada. */
        const val PULL_PAGE_SIZE = 500

        /** Folga ao retomar o download, em segundos (margem de engenharia, não é regra do domínio). */
        const val PULL_REWIND_SECONDS = 60L

        fun rewind(cursor: String): String = try {
            OffsetDateTime.parse(cursor).minusSeconds(PULL_REWIND_SECONDS).withOffsetSameInstant(ZoneOffset.UTC).toString()
        } catch (_: Exception) {
            cursor
        }
    }
}
