package com.cikup.amazgone.catalog.data.local

import androidx.room.TypeConverter
import com.cikup.amazgone.core.network.AppJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

class CatalogConverters {
    private val serializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromList(value: List<String>): String = AppJson.encodeToString(serializer, value)

    @TypeConverter
    fun toList(value: String): List<String> =
        runCatching { AppJson.decodeFromString(serializer, value) }.getOrDefault(emptyList())
}
