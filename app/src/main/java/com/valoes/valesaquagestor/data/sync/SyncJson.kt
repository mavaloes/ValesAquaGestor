package com.valoes.valesaquagestor.data.sync

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/*
 * Leitura e escrita das linhas do servidor. Campo ausente e `null` valem o mesmo: "sem valor".
 */

private fun JsonObject.primitive(key: String): JsonPrimitive? = (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }

internal fun JsonObject.string(key: String): String? = primitive(key)?.contentOrNull

internal fun JsonObject.long(key: String): Long? = primitive(key)?.longOrNull

internal fun JsonObject.int(key: String): Int? = primitive(key)?.intOrNull

internal fun JsonObject.double(key: String): Double? = primitive(key)?.doubleOrNull

internal fun JsonObject.boolean(key: String): Boolean? = primitive(key)?.booleanOrNull

/** Monta uma linha: valores `null` viram `null` do JSON (o servidor limpa a coluna). */
internal fun row(vararg fields: Pair<String, Any?>): JsonObject = JsonObject(
    fields.associate { (key, value) ->
        key to when (value) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Enum<*> -> JsonPrimitive(value.name)
            else -> error("Tipo não suportado em $key: ${value::class}")
        }
    },
)
