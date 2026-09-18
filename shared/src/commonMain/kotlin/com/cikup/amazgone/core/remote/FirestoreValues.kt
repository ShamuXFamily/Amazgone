package com.cikup.amazgone.core.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Instant

/**
 * Encoding between plain Kotlin values and Firestore REST "Value" JSON
 * (https://firebase.google.com/docs/firestore/reference/rest/v1/Value).
 */
object FirestoreValues {

    fun encode(value: Any?): JsonObject = when (value) {
        null -> buildJsonObject { put("nullValue", JsonNull) }
        is String -> buildJsonObject { put("stringValue", value) }
        is Boolean -> buildJsonObject { put("booleanValue", value) }
        is Int -> buildJsonObject { put("integerValue", value.toString()) }
        is Long -> buildJsonObject { put("integerValue", value.toString()) }
        is Double -> buildJsonObject { put("doubleValue", value) }
        is Instant -> buildJsonObject { put("timestampValue", value.toString()) }
        is List<*> -> buildJsonObject {
            put("arrayValue", buildJsonObject { put("values", JsonArray(value.map { encode(it) })) })
        }
        is Map<*, *> -> buildJsonObject { put("mapValue", buildJsonObject { put("fields", encodeFields(value)) }) }
        else -> error("Unsupported Firestore value type: ${value::class.simpleName}")
    }

    fun encodeFields(map: Map<*, *>): JsonObject =
        JsonObject(map.entries.associate { (k, v) -> k.toString() to encode(v) })

    /** Decodes a document's "fields" object into plain Kotlin values (timestamps → epoch millis). */
    fun decodeFields(fields: JsonObject?): Map<String, Any?> =
        fields?.mapValues { (_, v) -> decode(v) }.orEmpty()

    fun decode(element: JsonElement): Any? {
        val obj = element.jsonObject
        val (type, raw) = obj.entries.firstOrNull() ?: return null
        return when (type) {
            "nullValue" -> null
            "stringValue", "referenceValue" -> raw.jsonPrimitive.contentOrNull
            "booleanValue" -> raw.jsonPrimitive.booleanOrNull
            "integerValue" -> raw.jsonPrimitive.contentOrNull?.toLongOrNull()
            "doubleValue" -> raw.jsonPrimitive.doubleOrNull
            "timestampValue" -> raw.jsonPrimitive.contentOrNull?.let { Instant.parse(it).toEpochMilliseconds() }
            "arrayValue" -> raw.jsonObject["values"]?.jsonArray?.map { decode(it) }.orEmpty()
            "mapValue" -> decodeFields(raw.jsonObject["fields"]?.jsonObject)
            else -> (raw as? JsonPrimitive)?.contentOrNull
        }
    }
}

/** Typed accessors over decoded document fields. */
fun Map<String, Any?>.long(key: String): Long = (this[key] as? Number)?.toLong() ?: 0L
fun Map<String, Any?>.longOrNull(key: String): Long? = (this[key] as? Number)?.toLong()
fun Map<String, Any?>.string(key: String): String? = this[key] as? String
fun Map<String, Any?>.bool(key: String): Boolean = this[key] as? Boolean ?: false
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.list(key: String): List<Any?> = this[key] as? List<Any?> ?: emptyList()
