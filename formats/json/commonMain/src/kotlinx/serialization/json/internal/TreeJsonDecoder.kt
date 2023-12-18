/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("LeakingThis")
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*

@JsonFriendModuleApi
public fun <T> readJson(json: Json, element: JsonElement, deserializer: DeserializationStrategy<T>): T {
    val input = when (element) {
        is JsonObject -> JsonTreeDecoder(json, element)
        is JsonArray -> JsonTreeListDecoder(json, element)
        is JsonLiteral, JsonNull -> JsonPrimitiveDecoder(json, element as JsonPrimitive)
    }
    return input.decodeSerializableValue(deserializer)
}

internal fun <T> Json.readPolymorphicJson(
    discriminator: String,
    element: JsonObject,
    deserializer: DeserializationStrategy<T>
): T {
    return JsonTreeDecoder(this, element, discriminator, deserializer.descriptor).decodeSerializableValue(deserializer)
}

private sealed class AbstractJsonTreeDecoder(
    override val json: Json,
    open val value: JsonElement
) : NamedValueDecoder(), JsonDecoder {

    override val serializersModule: SerializersModule
        get() = json.serializersModule

    @JvmField
    protected val configuration = json.configuration

    protected fun currentObject() = currentTagOrNull?.let { currentElement(it) } ?: value

    override fun decodeJsonElement(): JsonElement = currentObject()

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return decodeSerializableValuePolymorphic(deserializer)
    }

    override fun composeName(parentName: String, childName: String): String = childName

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val currentObject = currentObject()
        return when (descriptor.kind) {
            StructureKind.LIST, is PolymorphicKind -> JsonTreeListDecoder(json, cast(currentObject, descriptor))
            StructureKind.MAP -> json.selectMapMode(
                descriptor,
                { JsonTreeMapDecoder(json, cast(currentObject, descriptor)) },
                { JsonTreeListDecoder(json, cast(currentObject, descriptor)) }
            )
            else -> JsonTreeDecoder(json, cast(currentObject, descriptor))
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // Nothing
    }

    override fun decodeNotNullMark(): Boolean = currentObject() !is JsonNull

    protected fun getPrimitiveValue(tag: String): JsonPrimitive {
        val currentElement = currentElement(tag)
        return currentElement as? JsonPrimitive ?: throw JsonDecodingException(
            -1,
            "Expected JsonPrimitive at $tag, found $currentElement", currentObject().toString()
        )
    }

    protected abstract fun currentElement(tag: String): JsonElement

    override fun decodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor): Int =
        enumDescriptor.getJsonNameIndexOrThrow(json, getPrimitiveValue(tag).content)

    override fun decodeTaggedNull(tag: String): Nothing? = null

    override fun decodeTaggedNotNullMark(tag: String): Boolean = currentElement(tag) !== JsonNull

    override fun decodeTaggedBoolean(tag: String): Boolean {
        return getPrimitiveValue(tag).primitive("boolean", JsonPrimitive::booleanOrNull)
    }

    override fun decodeTaggedByte(tag: String) = getPrimitiveValue(tag).primitive("byte") {
        val result = int
        if (result in Byte.MIN_VALUE..Byte.MAX_VALUE) result.toByte()
        else null
    }

    override fun decodeTaggedShort(tag: String) = getPrimitiveValue(tag).primitive("short") {
        val result = int
        if (result in Short.MIN_VALUE..Short.MAX_VALUE) result.toShort()
        else null
    }

    override fun decodeTaggedInt(tag: String) = getPrimitiveValue(tag).primitive("int") { int }
    override fun decodeTaggedLong(tag: String) = getPrimitiveValue(tag).primitive("long") { long }

    override fun decodeTaggedFloat(tag: String): Float {
        val result = getPrimitiveValue(tag).primitive("float") { float }
        val specialFp = json.configuration.allowSpecialFloatingPointValues
        if (specialFp || result.isFinite()) return result
        throw InvalidFloatingPointDecoded(result, tag, currentObject().toString())
    }

    override fun decodeTaggedDouble(tag: String): Double {
        val result = getPrimitiveValue(tag).primitive("double") { double }
        val specialFp = json.configuration.allowSpecialFloatingPointValues
        if (specialFp || result.isFinite()) return result
        throw InvalidFloatingPointDecoded(result, tag, currentObject().toString())
    }

    override fun decodeTaggedChar(tag: String): Char = getPrimitiveValue(tag).primitive("char") { content.single() }

    private inline fun <T : Any> JsonPrimitive.primitive(primitive: String, block: JsonPrimitive.() -> T?): T {
        try {
            return block() ?: unparsedPrimitive(primitive)
        } catch (e: IllegalArgumentException) {
            unparsedPrimitive(primitive)
        }
    }

    private fun unparsedPrimitive(primitive: String): Nothing {
        throw JsonDecodingException(-1, "Failed to parse literal as '$primitive' value", currentObject().toString())
    }

    override fun decodeTaggedString(tag: String): String {
        val value = getPrimitiveValue(tag)
        if (!json.configuration.isLenient) {
            val literal = value.asLiteral("string")
            if (!literal.isString) throw JsonDecodingException(
                -1, "String literal for key '$tag' should be quoted.\n$lenientHint", currentObject().toString()
            )
        }
        if (value is JsonNull) throw JsonDecodingException(-1, "Unexpected 'null' value instead of string literal", currentObject().toString())
        return value.content
    }

    private fun JsonPrimitive.asLiteral(type: String): JsonLiteral {
        return this as? JsonLiteral ?: throw JsonDecodingException(-1, "Unexpected 'null' literal when non-nullable $type was expected")
    }

    override fun decodeTaggedInline(tag: String, inlineDescriptor: SerialDescriptor): Decoder =
        if (inlineDescriptor.isUnsignedNumber) JsonDecoderForUnsignedTypes(StringJsonLexer(getPrimitiveValue(tag).content), json)
        else super.decodeTaggedInline(tag, inlineDescriptor)

    override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        return if (currentTagOrNull != null) super.decodeInline(descriptor)
        else JsonPrimitiveDecoder(json, value).decodeInline(descriptor)
    }
}

private class JsonPrimitiveDecoder(json: Json, override val value: JsonElement) : AbstractJsonTreeDecoder(json, value) {

    init {
        pushTag(PRIMITIVE_TAG)
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0

    override fun currentElement(tag: String): JsonElement {
        require(tag === PRIMITIVE_TAG) { "This input can only handle primitives with '$PRIMITIVE_TAG' tag" }
        return value
    }
}

private open class JsonTreeDecoder(
    json: Json,
    override val value: JsonObject,
    private val polyDiscriminator: String? = null,
    private val polyDescriptor: SerialDescriptor? = null
) : AbstractJsonTreeDecoder(json, value) {
    private var position = 0
    private var forceNull: Boolean = false
    /*
     * Checks whether JSON has `null` value for non-null property or unknown enum value for enum property
     */
    private fun coerceInputValue(descriptor: SerialDescriptor, index: Int, tag: String): Boolean =
        json.tryCoerceValue(
            descriptor, index,
            { currentElement(tag) is JsonNull },
            { (currentElement(tag) as? JsonPrimitive)?.contentOrNull }
        )

    @Suppress("INVISIBLE_MEMBER")
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (position < descriptor.elementsCount) {
            val name = descriptor.getTag(position++)
            val index = position - 1
            forceNull = false
            if ((name in value || absenceIsNull(descriptor, index))
                && (!configuration.coerceInputValues || !coerceInputValue(descriptor, index, name))
            ) {
                return index
            }
        }
        return CompositeDecoder.DECODE_DONE
    }

    private fun absenceIsNull(descriptor: SerialDescriptor, index: Int): Boolean {
        forceNull = !json.configuration.explicitNulls
                && !descriptor.isElementOptional(index) && descriptor.getElementDescriptor(index).isNullable
        return forceNull
    }

    override fun decodeNotNullMark(): Boolean {
        return !forceNull && super.decodeNotNullMark()
    }

    override fun elementName(descriptor: SerialDescriptor, index: Int): String {
        val strategy = descriptor.namingStrategy(json)
        val baseName = descriptor.getElementName(index)
        if (strategy == null) {
            if (!configuration.useAlternativeNames) return baseName
            // Fast path, do not go through ConcurrentHashMap.get
            // Note, it blocks ability to detect collisions between the primary name and alternate,
            // but it eliminates a significant performance penalty (about -15% without this optimization)
            if (baseName in value.keys) return baseName
        }
        // Slow path
        val deserializationNamesMap = json.deserializationNamesMap(descriptor)
        value.keys.find { deserializationNamesMap[it] == index }?.let {
            return it
        }

        val fallbackName = strategy?.serialNameForJson(
            descriptor,
            index,
            baseName
        ) // Key not found exception should be thrown with transformed name, not original
        return fallbackName ?: baseName
    }

    override fun currentElement(tag: String): JsonElement = value.getValue(tag)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        // polyDiscriminator needs to be preserved so the check for unknown keys
        // in endStructure can filter polyDiscriminator out.
        if (descriptor === polyDescriptor) {
            return JsonTreeDecoder(
                json, cast(currentObject(), polyDescriptor), polyDiscriminator, polyDescriptor
            )
        }

        return super.beginStructure(descriptor)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (configuration.ignoreUnknownKeys || descriptor.kind is PolymorphicKind) return
        // Validate keys
        val strategy = descriptor.namingStrategy(json)

        @Suppress("DEPRECATION_ERROR")
        val names: Set<String> = when {
            strategy == null && !configuration.useAlternativeNames -> descriptor.jsonCachedSerialNames()
            strategy != null -> json.deserializationNamesMap(descriptor).keys
            else -> descriptor.jsonCachedSerialNames() + json.schemaCache[descriptor, JsonDeserializationNamesKey]?.keys.orEmpty()
        }

        for (key in value.keys) {
            if (key !in names && key != polyDiscriminator) {
                throw UnknownKeyException(key, value.toString())
            }
        }
    }
}

private class JsonTreeMapDecoder(json: Json, override val value: JsonObject) : JsonTreeDecoder(json, value) {
    private val keys = value.keys.toList()
    private val size: Int = keys.size * 2
    private var position = -1

    override fun elementName(descriptor: SerialDescriptor, index: Int): String {
        val i = index / 2
        return keys[i]
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (position < size - 1) {
            position++
            return position
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun currentElement(tag: String): JsonElement {
        return if (position % 2 == 0) JsonPrimitive(tag) else value.getValue(tag)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // do nothing, maps do not have strict keys, so strict mode check is omitted
    }
}

private class JsonTreeListDecoder(json: Json, override val value: JsonArray) : AbstractJsonTreeDecoder(json, value) {
    private val size = value.size
    private var currentIndex = -1

    override fun elementName(descriptor: SerialDescriptor, index: Int): String = index.toString()

    override fun currentElement(tag: String): JsonElement {
        return value[tag.toInt()]
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (currentIndex < size - 1) {
            currentIndex++
            return currentIndex
        }
        return CompositeDecoder.DECODE_DONE
    }
}
