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
public fun <T> readJson(
    json: Json,
    element: JsonElement,
    deserializer: DeserializationStrategy<T>,
    previousDecoder: JsonDecoder? = null
): T {
    val discriminator = (previousDecoder as? PolymorphicJsonDecoder)?.discriminator
    val input = when (element) {
        is JsonObject -> JsonTreeDecoder(json, element, deserializer.descriptor, discriminator)
        is JsonArray -> JsonTreeListDecoder(json, element)
        is JsonLiteral, JsonNull -> JsonPrimitiveDecoder(json, element as JsonPrimitive)
    }
    return input.decodeSerializableValue(deserializer)
}

internal fun <T> Json.readPolymorphicJson(
    discriminator: String,
    element: JsonObject,
    // Note: this is an actual deserializer, not a polymorphic one
    deserializer: DeserializationStrategy<T>
): T {
    val descriptor = deserializer.descriptor
    return JsonTreeDecoder(
        this, element, descriptor, discriminator, descriptor
    ).decodeSerializableValue(deserializer)
}

private sealed class AbstractJsonTreeDecoder(
    override val json: Json,
    open val value: JsonElement,
    protected val polymorphicDiscriminator: String? = null
) : NamedValueDecoder(), PolymorphicJsonDecoder {

    override val serializersModule: SerializersModule
        get() = json.serializersModule

    override val discriminator: String?
        get() = polymorphicDiscriminator

    @JvmField
    protected val configuration = json.configuration

    protected fun currentObject() = currentTagOrNull?.let { currentElement(it) } ?: value

    fun renderTagStack(currentTag: String) = renderTagStack() + ".$currentTag"

    override fun decodeJsonElement(): JsonElement = currentObject()

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return decodeSerializableValuePolymorphic(deserializer, ::renderTagStack)
    }

    final override fun composeName(parentName: String, childName: String): String = childName

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val currentObject = currentObject()
        return when (descriptor.kind) {
            StructureKind.LIST, is PolymorphicKind -> JsonTreeListDecoder(json, cast(currentObject, descriptor))
            StructureKind.MAP -> json.selectMapMode(
                descriptor,
                { JsonTreeMapDecoder(json, cast(currentObject, descriptor), descriptor) },
                { JsonTreeListDecoder(json, cast(currentObject, descriptor)) }
            )
            else -> JsonTreeDecoder(json, cast(currentObject, descriptor), descriptor, polymorphicDiscriminator)
        }
    }

    inline fun <reified T : JsonElement> cast(value: JsonElement, descriptor: SerialDescriptor): T = cast(value, descriptor.serialName) { renderTagStack() }
    inline fun <reified T : JsonElement> cast(value: JsonElement, serialName: String, tag: String): T = cast(value, serialName) { renderTagStack(tag) }

    override fun endStructure(descriptor: SerialDescriptor) {
        // Nothing
    }

    override fun decodeNotNullMark(): Boolean = currentObject() !is JsonNull

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun getPrimitiveValue(tag: String, descriptor: SerialDescriptor): JsonPrimitive =
        cast(currentElement(tag), descriptor.serialName, tag)

    private inline fun <T : Any> getPrimitiveValue(tag: String, primitiveName: String, convert: JsonPrimitive.() -> T?): T {
        val literal = cast<JsonPrimitive>(currentElement(tag), primitiveName, tag)
        try {
            return literal.convert() ?: unparsedPrimitive(literal, primitiveName, tag)
        } catch (e: IllegalArgumentException) {
            // TODO: pass e as cause? (may conflict with #2590)
            unparsedPrimitive(literal, primitiveName, tag)
        }
    }

    private fun unparsedPrimitive(literal: JsonPrimitive, primitive: String, tag: String): Nothing {
        val type = if (primitive.startsWith("i")) "an $primitive" else "a $primitive"
        throw decodingExceptionOf("Failed to parse literal '$literal' as $type value", path = renderTagStack(tag)) {
            currentObject().toString()
        }
    }

    protected abstract fun currentElement(tag: String): JsonElement

    override fun decodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor): Int =
        enumDescriptor.getJsonNameIndexOrThrow(json, getPrimitiveValue(tag, enumDescriptor).content)

    override fun decodeTaggedNull(tag: String): Nothing? = null

    override fun decodeTaggedNotNullMark(tag: String): Boolean = currentElement(tag) !== JsonNull

    override fun decodeTaggedBoolean(tag: String): Boolean =
        getPrimitiveValue(tag, "boolean", JsonPrimitive::booleanOrNull)

    override fun decodeTaggedByte(tag: String) = getPrimitiveValue(tag, "byte") {
        val result = parseLongImpl()
        if (result in Byte.MIN_VALUE..Byte.MAX_VALUE) result.toByte()
        else null
    }

    override fun decodeTaggedShort(tag: String) = getPrimitiveValue(tag, "short") {
        val result = parseLongImpl()
        if (result in Short.MIN_VALUE..Short.MAX_VALUE) result.toShort()
        else null
    }

    override fun decodeTaggedInt(tag: String) = getPrimitiveValue(tag, "int") {
        val result = parseLongImpl()
        if (result in Int.MIN_VALUE..Int.MAX_VALUE) result.toInt()
        else null
    }

    override fun decodeTaggedLong(tag: String) = getPrimitiveValue(tag, "long") { parseLongImpl() }

    override fun decodeTaggedFloat(tag: String): Float {
        val result = getPrimitiveValue(tag, "float") { float }
        val specialFp = json.configuration.allowSpecialFloatingPointValues
        if (specialFp || result.isFinite()) return result
        throw InvalidFloatingPointDecoded(result, tag) { currentObject().toString() }
    }

    override fun decodeTaggedDouble(tag: String): Double {
        val result = getPrimitiveValue(tag, "double") { double }
        val specialFp = json.configuration.allowSpecialFloatingPointValues
        if (specialFp || result.isFinite()) return result
        throw InvalidFloatingPointDecoded(result, tag) { currentObject().toString() }
    }

    override fun decodeTaggedChar(tag: String): Char = getPrimitiveValue(tag, "char") { content.single() }

    override fun decodeTaggedString(tag: String): String {
        val value = cast<JsonPrimitive>(currentElement(tag), "string", tag)
        if (value !is JsonLiteral)
            throw decodingExceptionOf("Expected string value for a non-null key '$tag', got null literal instead", renderTagStack(tag), coerceInputValuesHint) {
                currentObject().toString()
            }
        if (!value.isString && !json.configuration.isLenient) {
            throw decodingExceptionOf("String literal for value of key '$tag' should be quoted", renderTagStack(tag), lenientHint) {
                currentObject().toString()
            }
        }
        return value.content
    }

    override fun decodeTaggedInline(tag: String, inlineDescriptor: SerialDescriptor): Decoder {
        return if (inlineDescriptor.isUnsignedNumber) {
            val lexer = StringJsonLexer(json, getPrimitiveValue(tag, inlineDescriptor).content)
            JsonDecoderForUnsignedTypes(lexer, json)
        } else super.decodeTaggedInline(tag, inlineDescriptor)
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        return if (currentTagOrNull != null) super.decodeInline(descriptor)
        else JsonPrimitiveDecoder(json, value, polymorphicDiscriminator).decodeInline(descriptor)
    }
}

private class JsonPrimitiveDecoder(
    json: Json,
    override val value: JsonElement,
    polymorphicDiscriminator: String? = null
) : AbstractJsonTreeDecoder(json, value, polymorphicDiscriminator) {

    init {
        pushTag(PRIMITIVE_TAG)
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0

    override fun currentElement(tag: String): JsonElement {
        require(tag == PRIMITIVE_TAG) { "This input can only handle primitives with '$PRIMITIVE_TAG' tag" }
        return value
    }
}

private open class JsonTreeDecoder(
    json: Json,
    override val value: JsonObject,
    descriptor: SerialDescriptor,
    polymorphicDiscriminator: String? = null,
    private val polyDescriptor: SerialDescriptor? = null
) : AbstractJsonTreeDecoder(json, value, polymorphicDiscriminator) {

    // Pointer to the current entry of JsonObject that is being decoded
    // NB: do not `override val value` in JsonTreeMapDecoder, otherwise this field won't be
    // initialized and will cause NPE.
    private val entries: Iterator<Map.Entry<String, JsonElement>> = value.entries.iterator()

    private val elementMarker: JsonElementMarker? = if (configuration.explicitNulls) null else JsonElementMarker(descriptor)

    // Cached results of entries.next() to be used in tagged protocol
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
                return index
            }
            if (key != polymorphicDiscriminator && !descriptor.ignoreUnknownKeys(json)) {
                throwUnknownKey(key)
            }
        }
        val markerIndex = elementMarker?.nextUnmarkedIndex() ?: CompositeDecoder.DECODE_DONE
        if (markerIndex != CompositeDecoder.DECODE_DONE) {
            currentName = descriptor.getElementName(markerIndex)
            currentValue = null
        }
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

    override fun elementName(descriptor: SerialDescriptor, index: Int): String =
        currentName ?: descriptor.getElementName(index)

    override fun currentElement(tag: String): JsonElement = currentValue ?: value.getValue(tag)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        // polyDiscriminator needs to be preserved so the discriminator key can be filtered out.
        if (descriptor === polyDescriptor) {
            return JsonTreeDecoder(
                json, cast(currentObject(), polyDescriptor), descriptor, polymorphicDiscriminator, polyDescriptor
            )
        }

        return super.beginStructure(descriptor)
    }

    private fun throwUnknownKey(key: String): Nothing {
        throw decodingExceptionOf(
            "Encountered an unknown key '$key'",
            renderTagStack(),
            ignoreUnknownKeysHint
        ) { value.toString() }
    }
}

private class JsonTreeMapDecoder(
    json: Json,
    value: JsonObject,
    descriptor: SerialDescriptor
) : JsonTreeDecoder(json, value, descriptor) {
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
