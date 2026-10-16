package com.valoes.valesaquagestor.data.local.entity

/**
 * Campos de sincronização que toda entity tem (`specs/02-dominio.md`). Permite aplicar a regra de
 * sincronização (deduplicar por [uuid], vence o maior [atualizadoEm]) em código único.
 */
interface SyncEntity {
    /** Chave local, gerada pelo banco. */
    val id: Long

    /** Identidade do registro na sincronização; único no banco. */
    val uuid: String

    /** Última alteração, em epoch millis. */
    val atualizadoEm: Long

    /** Exclusão lógica. */
    val excluido: Boolean

    /** `false` enquanto houver alteração ainda não enviada ao servidor. */
    val sincronizado: Boolean
}
