/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlin.native.concurrent.*

@SharedImmutable
internal val JsonDeserializationNamesKey = DescriptorSchemaCache.Key<Map<String, Int>>()

@SharedImmutable
internal val JsonSerializationNamesKey = DescriptorSchemaCache.Key<Array<String>>()

private fun SerialDescriptor.buildDeserializationNamesMap(json: Json): Map<String, Int> {
    fun MutableMap<String, Int>.putOrThrow(name: String, index: Int) {
        if (name in this) {
            throw JsonException(
                "The suggested name '$name' for property ${getElementName(index)} is already one of the names for property " +
                        "${getElementName(getValue(name))} in ${this@buildDeserializationNamesMap}"
            )
        }
        this[name] = index
    }

    val builder: MutableMap<String, Int> =
        mutableMapOf() // can be not concurrent because it is only read after creation and safely published to concurrent map
    val strategy = namingStrategy(json)
    for (i in 0 until elementsCount) {
        getElementAnnotations(i).filterIsInstance<JsonNames>().singleOrNull()?.names?.forEach { name ->
            builder.putOrThrow(name, i)
        }
        strategy?.let { builder.putOrThrow(it.serialNameForJson(this, i, getElementName(i)), i) }
    }
    return builder.ifEmpty { emptyMap() }
}

/**
 * Contains strategy-mapped names and @JsonNames,
 * so original names are not stored when strategy is `null`.
 */
internal fun Json.deserializationNamesMap(descriptor: SerialDescriptor): Map<String, Int> =
    schemaCache.getOrPut(descriptor, JsonDeserializationNamesKey) { descriptor.buildDeserializationNamesMap(this) }

internal fun SerialDescriptor.serializationNamesIndices(json: Json, strategy: JsonNamingStrategy): Array<String> =
    json.schemaCache.getOrPut(this, JsonSerializationNamesKey) {
        Array(elementsCount) { i ->
            val baseName = getElementName(i)
            strategy.serialNameForJson(this, i, baseName)
        }
    }

internal fun SerialDescriptor.getJsonElementName(json: Json, index: Int): String {
    val strategy = namingStrategy(json)
    return if (strategy == null) getElementName(index) else serializationNamesIndices(json, strategy)[index]
}

internal fun SerialDescriptor.namingStrategy(json: Json) =
    if (kind == StructureKind.CLASS) json.configuration.namingStrategy else null

/**
 * Serves same purpose as [SerialDescriptor.getElementIndex] but respects
 * [JsonNames] annotation and [JsonConfiguration.useAlternativeNames] state.
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getJsonNameIndex(json: Json, name: String): Int {
    fun getJsonNameIndexSlowPath(): Int =
        json.deserializationNamesMap(this)[name] ?: CompositeDecoder.UNKNOWN_NAME

    val strategy = namingStrategy(json)
    if (strategy != null) return getJsonNameIndexSlowPath()
    val index = getElementIndex(name)
    // Fast path, do not go through ConcurrentHashMap.get
    // Note, it blocks ability to detect collisions between the primary name and alternate,
    // but it eliminates a significant performance penalty (about -15% without this optimization)
    if (index != CompositeDecoder.UNKNOWN_NAME) return index
    if (!json.configuration.useAlternativeNames) return index
    // Slow path
    return getJsonNameIndexSlowPath()
}

/**
 * Throws on [CompositeDecoder.UNKNOWN_NAME]
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getJsonNameIndexOrThrow(json: Json, name: String, suffix: String = ""): Int {
    val index = getJsonNameIndex(json, name)
    if (index == CompositeDecoder.UNKNOWN_NAME)
        throw SerializationException("$serialName does not contain element with name '$name'$suffix")
    return index
}

@OptIn(ExperimentalSerializationApi::class)
internal inline fun Json.tryCoerceValue(
    elementDescriptor: SerialDescriptor,
    peekNull: () -> Boolean,
    tryPeekNull: () -> Boolean,
    peekString: () -> String?,
    onEnumCoercing: () -> Unit = {}
): Boolean {
    if (!elementDescriptor.isNullable && peekNull()) return true
    if (elementDescriptor.kind == SerialKind.ENUM) {
        if (elementDescriptor.isNullable && tryPeekNull()) {
            return false
        }

        val enumValue = peekString()
            ?: return false // if value is not a string, decodeEnum() will throw correct exception
        val enumIndex = elementDescriptor.getJsonNameIndex(this, enumValue)
        if (enumIndex == CompositeDecoder.UNKNOWN_NAME) {
            onEnumCoercing()
            return true
        }
    }
    return false
}
