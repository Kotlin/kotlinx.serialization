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

internal val JsonDeserializationNamesKey = DescriptorSchemaCache.Key<Map<String, Int>>()

internal val JsonSerializationNamesKey = DescriptorSchemaCache.Key<Array<String>>()

private fun SerialDescriptor.buildDeserializationNamesMap(json: Json): Map<String, Int> {
    fun MutableMap<String, Int>.putOrThrow(name: String, index: Int) {
        val entity = if (kind == SerialKind.ENUM) "enum value" else "property"
        if (name in this) {
            throw JsonException(
                "The suggested name '$name' for $entity ${getElementName(index)} is already one of the names for $entity " +
                        "${getElementName(getValue(name))} in ${this@buildDeserializationNamesMap}"
            )
        }
        this[name] = index
    }

    val builder: MutableMap<String, Int> =
        mutableMapOf() // can be not concurrent because it is only read after creation and safely published to concurrent map
    val useLowercaseEnums = json.decodeCaseInsensitive(this)
    val strategyForClasses = namingStrategy(json)
    for (i in 0 until elementsCount) {
        getElementAnnotations(i).filterIsInstance<JsonNames>().singleOrNull()?.names?.forEach { name ->
            builder.putOrThrow(if (useLowercaseEnums) name.lowercase() else name, i)
        }
        val nameToPut = when {
            // the branches do not intersect because useLowercase = true for enums only, and strategy != null for classes only.
            useLowercaseEnums -> getElementName(i).lowercase()
            strategyForClasses != null -> strategyForClasses.serialNameForJson(this, i, getElementName(i))
            else -> null
        }
        nameToPut?.let { builder.putOrThrow(it, i) }
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

private fun SerialDescriptor.getJsonNameIndexSlowPath(json: Json, name: String): Int =
    json.deserializationNamesMap(this)[name] ?: CompositeDecoder.UNKNOWN_NAME

private fun Json.decodeCaseInsensitive(descriptor: SerialDescriptor) =
    configuration.decodeEnumsCaseInsensitive && descriptor.kind == SerialKind.ENUM

/**
 * Serves same purpose as [SerialDescriptor.getElementIndex] but respects [JsonNames] annotation
 * and [JsonConfiguration] settings.
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getJsonNameIndex(json: Json, name: String): Int {
    if (json.decodeCaseInsensitive(this)) {
        return getJsonNameIndexSlowPath(json, name.lowercase())
    }

    val strategy = namingStrategy(json)
    if (strategy != null) return getJsonNameIndexSlowPath(json, name)
    val index = getElementIndex(name)
    // Fast path, do not go through ConcurrentHashMap.get
    // Note, it blocks ability to detect collisions between the primary name and alternate,
    // but it eliminates a significant performance penalty (about -15% without this optimization)
    if (index != CompositeDecoder.UNKNOWN_NAME) return index
    if (!json.configuration.useAlternativeNames) return index
    // Slow path
    return getJsonNameIndexSlowPath(json, name)
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

/**
 * Tries to coerce value according to the rules of [JsonConfiguration.coerceInputValues] and [JsonConfiguration.explicitNulls] flags:
 *
 * - If a property is optional (has default), has a non-nullable type, but input was `null` literal, property is coerced. (1)
 * - If a property is enum, but input contained string which is not a valid enum constant (3) or a `null` literal (2):
 *   - Property is coerced in case it is optional AND non-nullable (5), or nullable AND `explicitNulls` is on (4).
 *
 * @param descriptor Descriptor of class that owns the property
 * @param index The index of the element (property).
 * @param peekNull A function to peek if the next JSON token is `null`. In case `consume` is true, should consume `null` from the input.
 * @param peekString A function to peek the next JSON token as a string.
 * @param onEnumCoercing A callback function to be executed when coercing an enum. Use it to discard incorrect enum constant from the input.
 * @return `true` if value was coerced, `false` otherwise.
 */
@OptIn(ExperimentalSerializationApi::class)
internal inline fun Json.tryCoerceValue(
    descriptor: SerialDescriptor,
    index: Int,
    peekNull: (consume: Boolean) -> Boolean,
    peekString: () -> String?,
    onEnumCoercing: () -> Unit = {}
): Boolean {
    val isOptional = descriptor.isElementOptional(index)
    val elementDescriptor = descriptor.getElementDescriptor(index)
    if (isOptional && !elementDescriptor.isNullable && peekNull(true)) return true // (1)
    if (elementDescriptor.kind == SerialKind.ENUM) {
        if (elementDescriptor.isNullable && peekNull(false)) { // (2)
            return false
        }

        val enumValue = peekString()
            ?: return false // if value is not a string, decodeEnum() will throw correct exception
        val enumIndex = elementDescriptor.getJsonNameIndex(this, enumValue) // (3)
        val coerceToNull = !configuration.explicitNulls && elementDescriptor.isNullable // (4)
        if (enumIndex == CompositeDecoder.UNKNOWN_NAME && (isOptional || coerceToNull)) { // (3, 4, 5)
            onEnumCoercing()
            return true
        }
    }
    return false
}

internal fun SerialDescriptor.ignoreUnknownKeys(json: Json): Boolean =
    json.configuration.ignoreUnknownKeys || annotations.any { it is JsonIgnoreUnknownKeys }
