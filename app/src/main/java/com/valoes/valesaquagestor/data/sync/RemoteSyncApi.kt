package com.valoes.valesaquagestor.data.sync

import kotlinx.serialization.json.JsonObject

/** Uma página de registros baixados do servidor, da mais antiga à mais nova mudança. */
class RemotePage(
    val rows: List<JsonObject>,
    /** Marca da última mudança da página (`servidor_em`); `null` se a página veio vazia. */
    val lastCursor: String?,
)

/** Por que uma chamada ao servidor falhou. */
enum class SyncFailure {
    /** Sem internet ou conexão interrompida: tentar de novo depois. */
    OFFLINE,

    /** O servidor recusou a sessão e não foi possível renová-la: pedir novo login. */
    AUTH,

    /** Erro do servidor ou resposta inesperada: tentar de novo depois. */
    SERVER,
}

class SyncException(val failure: SyncFailure, message: String? = null, cause: Throwable? = null) :
    Exception(message ?: failure.name, cause)

/**
 * Conversa com o servidor, uma tabela por vez. As linhas são JSON já no formato do servidor
 * (colunas em snake_case, referências por `uuid`). O envio é um *upsert* por `uuid`: reenviar o mesmo
 * registro nunca cria uma cópia (caso G1 de `09-casos-de-teste.md`).
 */
interface RemoteSyncApi {

    /** Envia [rows] para [table]. Só retorna depois que o servidor confirmou; senão lança [SyncException]. */
    suspend fun push(table: String, rows: List<JsonObject>)

    /** Baixa até [limit] registros de [table] mudados depois de [after] (ou todos, se `null`). */
    suspend fun pull(table: String, after: String?, limit: Int): RemotePage
}
