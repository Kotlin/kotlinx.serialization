/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("LeakingThis")
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*

@JsonFriendModuleApi
public fun <T> readJson(
    json: Json,
    element: JsonElement,
    deserializer: DeserializationStrategy<T>,
    previousDecoder: JsonDecoder? = null
): T {
    val parent = previousDecoder as? PolymorphicJsonDecoder
    val discriminator = parent?.discriminator
    val path = parent?.path ?: JsonPath(json.configuration)
    val input = when (element) {
        is JsonObject -> JsonTreeDecoder(json, element, deserializer.descriptor, path, discriminator)
        is JsonArray -> JsonTreeListDecoder(json, element, path)
        is JsonLiteral, JsonNull -> JsonPrimitiveDecoder(json, element as JsonPrimitive, path)
    }
    return input.decodeSerializableValue(deserializer)
}

internal fun <T> Json.readPolymorphicJson(
    discriminator: String,
    element: JsonObject,
    // Note: this is an actual deserializer, not a polymorphic one
    deserializer: DeserializationStrategy<T>,
    path: JsonPath
): T {
    val descriptor = deserializer.descriptor
    return JsonTreeDecoder(
        this, element, descriptor, path, discriminator, descriptor
    ).decodeSerializableValue(deserializer)
}

private sealed class AbstractJsonTreeDecoder(
    override val json: Json,
    open val value: JsonElement,
    final override val path: JsonPath,
    protected val polymorphicDiscriminator: String? = null
) : AbstractDecoder(), PolymorphicJsonDecoder {

    override val serializersModule: SerializersModule
        get() = json.serializersModule

    override val discriminator: String?
        get() = polymorphicDiscriminator

    @JvmField
    protected val configuration = json.configuration

    override fun decodeJsonElement(): JsonElement = currentElement()

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return withExceptionHandling(path = path, input = currentElement()::toString) {
            decodeSerializableValuePolymorphic(deserializer)
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val element = currentElement()
        path.pushDescriptor(descriptor)
        return when (descriptor.kind) {
            StructureKind.LIST, is PolymorphicKind -> JsonTreeListDecoder(json, cast(element, descriptor), path)
            StructureKind.MAP -> json.selectMapMode(
                descriptor,
                { JsonTreeMapDecoder(json, cast(element, descriptor), descriptor, path) },
                { JsonTreeListDecoder(json, cast(element, descriptor), path) }
            )
            else -> JsonTreeDecoder(json, cast(element, descriptor), descriptor, path, polymorphicDiscriminator)
        }
    }

    inline fun <reified T : JsonElement> cast(value: JsonElement, descriptor: SerialDescriptor): T =
        cast(value, descriptor.serialName, path::getPath)

    override fun endStructure(descriptor: SerialDescriptor) {
        path.popDescriptor()
    }

    override fun decodeNotNullMark(): Boolean = currentElement() !is JsonNull

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun getPrimitiveValue(descriptor: SerialDescriptor): JsonPrimitive =
        cast(currentElement(), descriptor.serialName, path::getPath)

    private inline fun <T : Any> getPrimitiveValue(primitiveName: String, convert: JsonPrimitive.() -> T?): T {
        val literal = cast<JsonPrimitive>(currentElement(), primitiveName, path::getPath)
        try {
            return literal.convert() ?: unparsedPrimitive(literal, primitiveName)
        } catch (e: IllegalArgumentException) {
            // TODO: pass e as cause? (may conflict with #2590)
            unparsedPrimitive(literal, primitiveName)
        }
    }

    private fun unparsedPrimitive(literal: JsonPrimitive, primitive: String): Nothing {
        val type = if (primitive.startsWith("i")) "an $primitive" else "a $primitive"
        throw decodingExceptionOf("Failed to parse literal '$literal' as $type value", path = path.getPath()) {
            currentElement().toString()
        }
    }

    // The element being decoded
    protected open fun currentElement(): JsonElement = value

    // The name of the element being decoded, used in error messages only
    protected open fun currentTag(): String = PRIMITIVE_TAG

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int =
        enumDescriptor.getJsonNameIndexOrThrow(json, getPrimitiveValue(enumDescriptor).content, path)

    override fun decodeBoolean(): Boolean =
        getPrimitiveValue("boolean", JsonPrimitive::booleanOrNull)

    override fun decodeByte() = getPrimitiveValue("byte") {
        val result = parseLongImpl()
        if (result in Byte.MIN_VALUE..Byte.MAX_VALUE) result.toByte()
        else null
    }

    override fun decodeShort() = getPrimitiveValue("short") {
        val result = parseLongImpl()
        if (result in Short.MIN_VALUE..Short.MAX_VALUE) result.toShort()
        else null
    }

    override fun decodeInt() = getPrimitiveValue("int") {
        val result = parseLongImpl()
        if (result in Int.MIN_VALUE..Int.MAX_VALUE) result.toInt()
        else null
    }

    override fun decodeLong() = getPrimitiveValue("long") { parseLongImpl() }

    override fun decodeFloat(): Float {
        val result = getPrimitiveValue("float") { float }
        val specialFp = json.configuration.allowSpecialFloatingPointValues
        if (specialFp || result.isFinite()) return result
        throw decodingExceptionOf(nonFiniteFpMessage(result, null), path.getPath(), specialFlowingValuesHint) { currentElement().toString() }
    }

    override fun decodeDouble(): Double {
        val result = getPrimitiveValue("double") { double }
        val specialFp = json.configuration.allowSpecialFloatingPointValues
        if (specialFp || result.isFinite()) return result
        throw decodingExceptionOf(nonFiniteFpMessage(result, null), path.getPath(), specialFlowingValuesHint) { currentElement().toString() }
    }

    override fun decodeChar(): Char = getPrimitiveValue("char") { content.single() }

    override fun decodeString(): String {
        val tag = currentTag()
        val value = cast<JsonPrimitive>(currentElement(), "string", path::getPath)
        if (value !is JsonLiteral)
            throw decodingExceptionOf("Expected string value for a non-null key '$tag', got null literal instead", path.getPath(), coerceInputValuesHint) {
                currentElement().toString()
            }
        if (!value.isString && !json.configuration.isLenient) {
            throw decodingExceptionOf("String literal for value of key '$tag' should be quoted", path.getPath(), lenientHint) {
                currentElement().toString()
            }
        }
        return value.content
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        return if (descriptor.isUnsignedNumber) {
            val lexer = StringJsonLexer(json, getPrimitiveValue(descriptor).content)
            JsonDecoderForUnsignedTypes(lexer, json)
        } else this
    }
}

private class JsonPrimitiveDecoder(
    json: Json,
    override val value: JsonElement,
    path: JsonPath,
) : AbstractJsonTreeDecoder(json, value, path) {

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0
}

private open class JsonTreeDecoder(
    json: Json,
    override val value: JsonObject,
    descriptor: SerialDescriptor,
    path: JsonPath,
    polymorphicDiscriminator: String? = null,
    private val polyDescriptor: SerialDescriptor? = null
) : AbstractJsonTreeDecoder(json, value, path, polymorphicDiscriminator) {

    // Pointer to the current entry of JsonObject that is being decoded
    // NB: do not `override val value` in JsonTreeMapDecoder, otherwise this field won't be
    // initialized and will cause NPE.
    private val entries: Iterator<Map.Entry<String, JsonElement>> = value.entries.iterator()

    private val elementMarker: JsonElementMarker? = if (configuration.explicitNulls) null else JsonElementMarker(descriptor)

    // Cached results of entries.next() used directly by primitive and nested decoders
    private var currentName: String? = null
    private var currentValue: JsonElement? = null

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val entries = entries
        while (entries.hasNext()) {
            val entry = entries.next()
            val key = entry.key
            val index = descriptor.getJsonNameIndex(json, key)
            if (index != CompositeDecoder.UNKNOWN_NAME) {
                if (configuration.coerceInputValues && coerceInputValue(descriptor, index, entry.value)) {
                    continue
                }
                elementMarker?.mark(index)
                currentName = key
                currentValue = entry.value
                path.updateDescriptorIndex(index)
                return index
            }
            if (key != polymorphicDiscriminator && !descriptor.ignoreUnknownKeys(json)) {
                path.updateDescriptorIndex(CompositeDecoder.UNKNOWN_NAME)
                throwUnknownKey(key)
            }
        }
        val markerIndex = elementMarker?.nextUnmarkedIndex() ?: CompositeDecoder.DECODE_DONE
        if (markerIndex != CompositeDecoder.DECODE_DONE) {
            currentName = descriptor.getElementName(markerIndex)
            currentValue = null
        }
        path.updateDescriptorIndex(markerIndex)
        return markerIndex
    }

    private fun coerceInputValue(descriptor: SerialDescriptor, index: Int, element: JsonElement): Boolean =
        json.tryCoerceValue(
            descriptor, index,
            { element is JsonNull },
            { (element as? JsonPrimitive)?.contentOrNull }
        )

    override fun decodeNotNullMark(): Boolean {
        return !(elementMarker?.isUnmarkedNull ?: false) && super.decodeNotNullMark()
    }

    override fun currentElement(): JsonElement =
        currentValue ?: currentName?.let(value::getValue) ?: value

    override fun currentTag(): String = currentName ?: PRIMITIVE_TAG

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        // polyDiscriminator needs to be preserved so the discriminator key can be filtered out.
        if (descriptor === polyDescriptor) {
            path.pushDescriptor(descriptor)
            return JsonTreeDecoder(
                json, cast(currentElement(), polyDescriptor), descriptor, path, polymorphicDiscriminator, polyDescriptor
            )
        }

        return super.beginStructure(descriptor)
    }

    private fun throwUnknownKey(key: String): Nothing {
        throw decodingExceptionOf(
            "Encountered an unknown key '$key'",
            path.getPath(),
            ignoreUnknownKeysHint
        ) { value.toString() }
    }
}

private class JsonTreeMapDecoder(
    json: Json,
    value: JsonObject,
    descriptor: SerialDescriptor,
    path: JsonPath,
) : JsonTreeDecoder(json, value, descriptor, path) {
    private val keys = value.keys.toList()
    private val size: Int = keys.size * 2
    private var position = -1

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (position < size - 1) {
            position++
            return position
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T {
        val isMapKey = index and 1 == 0
        return withMapKeyTracking(path, isMapKey) {
            super.decodeSerializableElement(descriptor, index, deserializer, previousValue)
        }
    }

    override fun currentElement(): JsonElement {
        if (position < 0) return value
        val key = keys[position / 2]
        return if (position % 2 == 0) JsonPrimitive(key) else value.getValue(key)
    }

    override fun currentTag(): String = if (position < 0) PRIMITIVE_TAG else keys[position / 2]
}

private class JsonTreeListDecoder(
    json: Json,
    override val value: JsonArray,
    path: JsonPath,
) : AbstractJsonTreeDecoder(json, value, path) {
    private val size = value.size
    private var currentIndex = -1

    override fun currentElement(): JsonElement = if (currentIndex < 0) value else value[currentIndex]

    override fun currentTag(): String = if (currentIndex < 0) PRIMITIVE_TAG else currentIndex.toString()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (currentIndex < size - 1) {
            currentIndex++
            path.updateDescriptorIndex(currentIndex)
            return currentIndex
        }
        path.updateDescriptorIndex(CompositeDecoder.DECODE_DONE)
        return CompositeDecoder.DECODE_DONE
    }
}
