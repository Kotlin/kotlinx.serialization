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

internal fun <T> Json.readJson(element: JsonElement, deserializer: DeserializationStrategy<T>): T {
    val input = when (element) {
        is JsonObject -> JsonTreeDecoder(this, element)
        is JsonArray -> JsonTreeListDecoder(this, element)
        is JsonLiteral, JsonNull -> JsonPrimitiveDecoder(this, element as JsonPrimitive)
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

    private fun currentObject() = currentTagOrNull?.let { currentElement(it) } ?: value

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
        val value = getPrimitiveValue(tag)
        if (!json.configuration.isLenient) {
            val literal = value.asLiteral("boolean")
            if (literal.isString) throw JsonDecodingException(
                -1, "Boolean literal for key '$tag' should be unquoted.\n$lenientHint", currentObject().toString()
            )
        }
        return value.primitive("boolean") {
            booleanOrNull ?: throw IllegalArgumentException() /* Will be handled by 'primitive' */
        }
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

    private inline fun <T: Any> JsonPrimitive.primitive(primitive: String, block: JsonPrimitive.() -> T?): T {
        try {
            return block() ?: unparsedPrimitive(primitive)
        } catch (e: IllegalArgumentException) {
            unparsedPrimitive(primitive)
        }
    }

    private fun unparsedPrimitive(primitive: String): Nothing {
        throw JsonDecodingException(-1, "Failed to parse '$primitive'", currentObject().toString())
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
        return this as? JsonLiteral ?: throw JsonDecodingException(-1, "Unexpected 'null' when $type was expected")
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun decodeTaggedInline(tag: String, inlineDescriptor: SerialDescriptor): Decoder =
        if (inlineDescriptor.isUnsignedNumber) JsonDecoderForUnsignedTypes(StringJsonLexer(getPrimitiveValue(tag).content), json)
        else super.decodeTaggedInline(tag, inlineDescriptor)
}

private class JsonPrimitiveDecoder(json: Json, override val value: JsonPrimitive) : AbstractJsonTreeDecoder(json, value) {

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

    /*
     * Checks whether JSON has `null` value for non-null property or unknown enum value for enum property
     */
    private fun coerceInputValue(descriptor: SerialDescriptor, index: Int, tag: String): Boolean =
        json.tryCoerceValue(
            descriptor.getElementDescriptor(index),
            { currentElement(tag) is JsonNull },
            { (currentElement(tag) as? JsonPrimitive)?.contentOrNull }
        )

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (position < descriptor.elementsCount) {
            val name = descriptor.getTag(position++)
            if (name in value && (!configuration.coerceInputValues || !coerceInputValue(descriptor, position - 1, name))) {
                return position - 1
            }
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun elementName(desc: SerialDescriptor, index: Int): String {
        val mainName = desc.getElementName(index)
        if (!configuration.useAlternativeNames) return mainName
        // Fast path, do not go through ConcurrentHashMap.get
        // Note, it blocks ability to detect collisions between the primary name and alternate,
        // but it eliminates a significant performance penalty (about -15% without this optimization)
        if (mainName in value.keys) return mainName
        // Slow path
        val alternativeNamesMap =
            json.schemaCache.getOrPut(desc, JsonAlternativeNamesKey, desc::buildAlternativeNamesMap)
        val nameInObject = value.keys.find { alternativeNamesMap[it] == index }
        return nameInObject ?: mainName
    }

    override fun currentElement(tag: String): JsonElement = value.getValue(tag)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        /*
         * For polymorphic serialization we'd like to avoid excessive decoder creating in
         * beginStructure to properly preserve 'polyDiscriminator' field and filter it out.
         */
        if (descriptor === polyDescriptor) return this
        return super.beginStructure(descriptor)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (configuration.ignoreUnknownKeys || descriptor.kind is PolymorphicKind) return
        // Validate keys
        @Suppress("DEPRECATION_ERROR")
        val names: Set<String> =
            if (!configuration.useAlternativeNames)
                descriptor.jsonCachedSerialNames()
            else
                descriptor.jsonCachedSerialNames() + json.schemaCache[descriptor, JsonAlternativeNamesKey]?.keys.orEmpty()

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

    override fun elementName(desc: SerialDescriptor, index: Int): String {
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

    override fun elementName(desc: SerialDescriptor, index: Int): String = (index).toString()

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
