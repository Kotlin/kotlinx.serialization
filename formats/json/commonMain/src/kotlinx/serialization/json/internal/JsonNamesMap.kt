/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlin.native.concurrent.*

@SharedImmutable
internal val JsonAlternativeNamesKey = DescriptorSchemaCache.Key<Map<String, Int>>()

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.buildAlternativeNamesMap(): Map<String, Int> {
    fun MutableMap<String, Int>.putOrThrow(name: String, index: Int) {
        if (name in this) {
            throw JsonException(
                "The suggested name '$name' for property ${getElementName(index)} is already one of the names for property " +
                        "${getElementName(getValue(name))} in ${this@buildAlternativeNamesMap}"
            )
        }
        this[name] = index
    }

    var builder: MutableMap<String, Int>? = null
    for (i in 0 until elementsCount) {
        getElementAnnotations(i).filterIsInstance<JsonNames>().singleOrNull()?.names?.forEach { name ->
            if (builder == null) builder = createMapForCache(elementsCount)
            builder!!.putOrThrow(name, i)
        }
    }
    return builder ?: emptyMap()
}

/**
 * Serves same purpose as [SerialDescriptor.getElementIndex] but respects
 * [JsonNames] annotation and [JsonConfiguration.useAlternativeNames] state.
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getJsonNameIndex(json: Json, name: String): Int {
    val index = getElementIndex(name)
    // Fast path, do not go through ConcurrentHashMap.get
    // Note, it blocks ability to detect collisions between the primary name and alternate,
    // but it eliminates a significant performance penalty (about -15% without this optimization)
    if (index != CompositeDecoder.UNKNOWN_NAME) return index
    if (!json.configuration.useAlternativeNames) return index
    // Slow path
    val alternativeNamesMap =
        json.schemaCache.getOrPut(this, JsonAlternativeNamesKey, this::buildAlternativeNamesMap)
    return alternativeNamesMap[name] ?: CompositeDecoder.UNKNOWN_NAME
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
    peekString: () -> String?,
    onEnumCoercing: () -> Unit = {}
): Boolean {
    if (!elementDescriptor.isNullable && peekNull()) return true
    if (elementDescriptor.kind == SerialKind.ENUM) {
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
