package com.valoes.valesaquagestor.data.repository

import com.valoes.valesaquagestor.data.local.entity.SyncEntity

/** O que aconteceu ao aplicar um registro vindo do servidor. */
enum class MergeResult {
    /** Não existia neste aparelho: foi inserido. */
    INSERTED,

    /** Existia e o recebido era mais novo: foi atualizado. */
    UPDATED,

    /** Existia com `atualizadoEm` igual ou maior: o recebido foi descartado. */
    IGNORED,
}

/**
 * Regra de sincronização (`specs/05-offline-sync.md`): deduplica por `uuid` e, em conflito, vence o
 * maior `atualizadoEm`. Em empate o registro local é mantido, então enviar o mesmo registro duas
 * vezes não cria uma cópia (caso G1) e nada é regravado à toa.
 *
 * É usada pelos repositórios a partir da Fase 09 (`SyncWorker`); aqui fica só a regra, testada.
 *
 * @param find busca o registro local pelo `uuid` (incluindo excluídos).
 * @param insert grava o recebido como registro novo (o chamador zera o `id`).
 * @param update grava o recebido sobre o registro local de id [SyncEntity.id] informado.
 */
internal suspend fun <T : SyncEntity> mergeByUuid(
    incoming: T,
    find: suspend (uuid: String) -> T?,
    insert: suspend (T) -> Unit,
    update: suspend (incoming: T, localId: Long) -> Unit,
): MergeResult {
    val local = find(incoming.uuid)
    return when {
        local == null -> {
            insert(incoming)
            MergeResult.INSERTED
        }

        incoming.atualizadoEm > local.atualizadoEm -> {
            update(incoming, local.id)
            MergeResult.UPDATED
        }

        else -> MergeResult.IGNORED
    }
}
