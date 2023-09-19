/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.collections.set
import kotlin.jvm.*

@JsonFriendModuleApi
public fun <T> writeJson(json: Json, value: T, serializer: SerializationStrategy<T>): JsonElement {
    lateinit var result: JsonElement
    val encoder = JsonTreeEncoder(json) { result = it }
    encoder.encodeSerializableValue(serializer, value)
    return result
}

@ExperimentalSerializationApi
private sealed class AbstractJsonTreeEncoder(
    final override val json: Json,
    protected val nodeConsumer: (JsonElement) -> Unit
) : NamedValueEncoder(), JsonEncoder {

    final override val serializersModule: SerializersModule
        get() = json.serializersModule

    @JvmField
    protected val configuration = json.configuration

    private var polymorphicDiscriminator: String? = null

    override fun elementName(descriptor: SerialDescriptor, index: Int): String =
        descriptor.getJsonElementName(json, index)

    override fun encodeJsonElement(element: JsonElement) {
        encodeSerializableValue(JsonElementSerializer, element)
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        configuration.encodeDefaults

    override fun composeName(parentName: String, childName: String): String = childName
    abstract fun putElement(key: String, element: JsonElement)
    abstract fun getCurrent(): JsonElement

    // has no tag when encoding a nullable element at root level
    override fun encodeNotNullMark() {}

    // has no tag when encoding a nullable element at root level
    override fun encodeNull() {
        val tag = currentTagOrNull ?: return nodeConsumer(JsonNull)
        encodeTaggedNull(tag)
    }

    override fun encodeTaggedNull(tag: String) = putElement(tag, JsonNull)

    override fun encodeTaggedInt(tag: String, value: Int) = putElement(tag, JsonPrimitive(value))
    override fun encodeTaggedByte(tag: String, value: Byte) = putElement(tag, JsonPrimitive(value))
    override fun encodeTaggedShort(tag: String, value: Short) = putElement(tag, JsonPrimitive(value))
    override fun encodeTaggedLong(tag: String, value: Long) = putElement(tag, JsonPrimitive(value))

    override fun encodeTaggedFloat(tag: String, value: Float) {
        // First encode value, then check, to have a prettier error message
        putElement(tag, JsonPrimitive(value))
        if (!configuration.allowSpecialFloatingPointValues && !value.isFinite()) {
            throw InvalidFloatingPointEncoded(value, tag, getCurrent().toString())
        }
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        // Writing non-structured data (i.e. primitives) on top-level (e.g. without any tag) requires special output
        if (currentTagOrNull != null || !serializer.descriptor.carrierDescriptor(serializersModule).requiresTopLevelTag) {
            encodePolymorphically(serializer, value) { polymorphicDiscriminator = it }
        } else JsonPrimitiveEncoder(json, nodeConsumer).apply {
            encodeSerializableValue(serializer, value)
        }
    }

    override fun encodeTaggedDouble(tag: String, value: Double) {
        // First encode value, then check, to have a prettier error message
        putElement(tag, JsonPrimitive(value))
        if (!configuration.allowSpecialFloatingPointValues && !value.isFinite()) {
            throw InvalidFloatingPointEncoded(value, tag, getCurrent().toString())
        }
    }

    override fun encodeTaggedBoolean(tag: String, value: Boolean) = putElement(tag, JsonPrimitive(value))
    override fun encodeTaggedChar(tag: String, value: Char) = putElement(tag, JsonPrimitive(value.toString()))
    override fun encodeTaggedString(tag: String, value: String) = putElement(tag, JsonPrimitive(value))
    override fun encodeTaggedEnum(
        tag: String,
        enumDescriptor: SerialDescriptor,
        ordinal: Int
    ) = putElement(tag, JsonPrimitive(enumDescriptor.getElementName(ordinal)))

    override fun encodeTaggedValue(tag: String, value: Any) {
        putElement(tag, JsonPrimitive(value.toString()))
    }

    override fun encodeTaggedInline(tag: String, inlineDescriptor: SerialDescriptor): Encoder =
        when {
            inlineDescriptor.isUnsignedNumber -> inlineUnsignedNumberEncoder(tag)
            inlineDescriptor.isUnquotedLiteral -> inlineUnquotedLiteralEncoder(tag, inlineDescriptor)
            else -> super.encodeTaggedInline(tag, inlineDescriptor)
        }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder {
        return if (currentTagOrNull != null) super.encodeInline(descriptor)
        else JsonPrimitiveEncoder(json, nodeConsumer).encodeInline(descriptor)
    }

    @SuppressAnimalSniffer // Long(Integer).toUnsignedString(long)
    private fun inlineUnsignedNumberEncoder(tag: String) = object : AbstractEncoder() {
            override val serializersModule: SerializersModule = json.serializersModule

            fun putUnquotedString(s: String) = putElement(tag, JsonLiteral(s, isString = false))
            override fun encodeInt(value: Int) = putUnquotedString(value.toUInt().toString())
            override fun encodeLong(value: Long) = putUnquotedString(value.toULong().toString())
            override fun encodeByte(value: Byte) = putUnquotedString(value.toUByte().toString())
            override fun encodeShort(value: Short) = putUnquotedString(value.toUShort().toString())
        }

    private fun inlineUnquotedLiteralEncoder(tag: String, inlineDescriptor: SerialDescriptor) = object : AbstractEncoder() {
        override val serializersModule: SerializersModule get() = json.serializersModule

        override fun encodeString(value: String)  = putElement(tag, JsonLiteral(value, isString = false, coerceToInlineType = inlineDescriptor))
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val consumer =
            if (currentTagOrNull == null) nodeConsumer
            else { node -> putElement(currentTag, node) }

        val encoder = when (descriptor.kind) {
            StructureKind.LIST, is PolymorphicKind -> JsonTreeListEncoder(json, consumer)
            StructureKind.MAP -> json.selectMapMode(
                descriptor,
                { JsonTreeMapEncoder(json, consumer) },
                { JsonTreeListEncoder(json, consumer) }
            )
            else -> JsonTreeEncoder(json, consumer)
        }

        if (polymorphicDiscriminator != null) {
            encoder.putElement(polymorphicDiscriminator!!, JsonPrimitive(descriptor.serialName))
            polymorphicDiscriminator = null
        }

        return encoder
    }

    override fun endEncode(descriptor: SerialDescriptor) {
        nodeConsumer(getCurrent())
    }
}

private val SerialDescriptor.requiresTopLevelTag: Boolean
    get() = kind is PrimitiveKind || kind === SerialKind.ENUM

internal const val PRIMITIVE_TAG = "primitive" // also used in JsonPrimitiveInput

private class JsonPrimitiveEncoder(
    json: Json,
    nodeConsumer: (JsonElement) -> Unit
) : AbstractJsonTreeEncoder(json, nodeConsumer) {
    private var content: JsonElement? = null

    init {
        pushTag(PRIMITIVE_TAG)
    }

    override fun putElement(key: String, element: JsonElement) {
        require(key === PRIMITIVE_TAG) { "This output can only consume primitives with '$PRIMITIVE_TAG' tag" }
        require(content == null) { "Primitive element was already recorded. Does call to .encodeXxx happen more than once?" }
        content = element
        nodeConsumer(element)
    }

    override fun getCurrent(): JsonElement =
        requireNotNull(content) { "Primitive element has not been recorded. Is call to .encodeXxx is missing in serializer?" }
}

private open class JsonTreeEncoder(
    json: Json, nodeConsumer: (JsonElement) -> Unit
) : AbstractJsonTreeEncoder(json, nodeConsumer) {

    protected val content: MutableMap<String, JsonElement> = linkedMapOf()

    override fun putElement(key: String, element: JsonElement) {
        content[key] = element
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (value != null || configuration.explicitNulls) {
            super.encodeNullableSerializableElement(descriptor, index, serializer, value)
        }
    }

    override fun getCurrent(): JsonElement = JsonObject(content)
}

private class JsonTreeMapEncoder(json: Json, nodeConsumer: (JsonElement) -> Unit) : JsonTreeEncoder(json, nodeConsumer) {
    private lateinit var tag: String
    private var isKey = true

    override fun putElement(key: String, element: JsonElement) {
        if (isKey) { // writing key
            tag = when (element) {
                is JsonPrimitive -> element.content
                is JsonObject -> throw InvalidKeyKindException(JsonObjectSerializer.descriptor)
                is JsonArray -> throw InvalidKeyKindException(JsonArraySerializer.descriptor)
            }
            isKey = false
        } else {
            content[tag] = element
            isKey = true
        }
    }

    override fun getCurrent(): JsonElement {
        return JsonObject(content)
    }
}

private class JsonTreeListEncoder(json: Json, nodeConsumer: (JsonElement) -> Unit) :
    AbstractJsonTreeEncoder(json, nodeConsumer) {
    private val array: ArrayList<JsonElement> = arrayListOf()
    override fun elementName(descriptor: SerialDescriptor, index: Int): String = index.toString()

    override fun putElement(key: String, element: JsonElement) {
        val idx = key.toInt()
        array.add(idx, element)
    }

    override fun getCurrent(): JsonElement = JsonArray(array)
}

@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified T : JsonElement> cast(value: JsonElement, descriptor: SerialDescriptor): T {
    if (value !is T) {
        throw JsonDecodingException(
            -1,
            "Expected ${T::class} as the serialized body of ${descriptor.serialName}, but had ${value::class}"
        )
    }
    return value
}
