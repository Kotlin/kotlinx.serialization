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
    open val value: JsonElement,
    protected val polymorphicDiscriminator: String? = null
) : NamedValueDecoder(), JsonDecoder {

    override val serializersModule: SerializersModule
        get() = json.serializersModule

    @JvmField
    protected val configuration = json.configuration

    protected fun currentObject() = currentTagOrNull?.let { currentElement(it) } ?: value

    fun renderTagStack(currentTag: String) = renderTagStack() + ".$currentTag"

    override fun decodeJsonElement(): JsonElement = currentObject()

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return decodeSerializableValuePolymorphic(deserializer, ::renderTagStack)
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
            else -> JsonTreeDecoder(json, cast(currentObject, descriptor), polymorphicDiscriminator)
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
        throw JsonDecodingException(-1, "Failed to parse literal '$literal' as $type value at element: ${renderTagStack(tag)}", currentObject().toString())
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
        throw InvalidFloatingPointDecoded(result, tag, currentObject().toString())
    }

    override fun decodeTaggedDouble(tag: String): Double {
        val result = getPrimitiveValue(tag, "double") { double }
        val specialFp = json.configuration.allowSpecialFloatingPointValues
        if (specialFp || result.isFinite()) return result
        throw InvalidFloatingPointDecoded(result, tag, currentObject().toString())
    }

    override fun decodeTaggedChar(tag: String): Char = getPrimitiveValue(tag, "char") { content.single() }

    override fun decodeTaggedString(tag: String): String {
        val value = cast<JsonPrimitive>(currentElement(tag), "string", tag)
        if (value !is JsonLiteral)
            throw JsonDecodingException(-1, "Expected string value for a non-null key '$tag', got null literal instead at element: ${renderTagStack(tag)}", currentObject().toString())
        if (!value.isString && !json.configuration.isLenient) {
            throw JsonDecodingException(
                -1, "String literal for key '$tag' should be quoted at element: ${renderTagStack(tag)}.\n$lenientHint", currentObject().toString()
            )
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
        require(tag === PRIMITIVE_TAG) { "This input can only handle primitives with '$PRIMITIVE_TAG' tag" }
        return value
    }
}

private open class JsonTreeDecoder(
    json: Json,
    override val value: JsonObject,
    polymorphicDiscriminator: String? = null,
    private val polyDescriptor: SerialDescriptor? = null
) : AbstractJsonTreeDecoder(json, value, polymorphicDiscriminator) {
    private var position = 0
    private var forceNull: Boolean = false

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (position < descriptor.elementsCount) {
            val name = descriptor.getTag(position++)
            val index = position - 1
            forceNull = false

            if (name in value || setForceNull(descriptor, index)) {
                // if forceNull is true, then decodeNotNullMark returns false and `null` is automatically inserted
                // by Decoder.decodeIfNullable
                if (!configuration.coerceInputValues) return index

                if (json.tryCoerceValue(
                        descriptor, index,
                        { currentElementOrNull(name) is JsonNull },
                        { (currentElementOrNull(name) as? JsonPrimitive)?.contentOrNull },
                        { // an unknown enum value should be coerced to null via decodeNotNullMark if explicitNulls=false :
                            if (setForceNull(descriptor, index)) return index
                        }
                    )
                ) continue // do not read coerced value

                return index
            }
        }
        return CompositeDecoder.DECODE_DONE
    }

    private fun setForceNull(descriptor: SerialDescriptor, index: Int): Boolean {
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

    fun currentElementOrNull(tag: String): JsonElement? = value[tag]

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        // polyDiscriminator needs to be preserved so the check for unknown keys
        // in endStructure can filter polyDiscriminator out.
        if (descriptor === polyDescriptor) {
            return JsonTreeDecoder(
                json, cast(currentObject(), polyDescriptor), polymorphicDiscriminator, polyDescriptor
            )
        }

        return super.beginStructure(descriptor)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (descriptor.ignoreUnknownKeys(json) || descriptor.kind is PolymorphicKind) return
        // Validate keys
        val strategy = descriptor.namingStrategy(json)

        @Suppress("DEPRECATION_ERROR")
        val names: Set<String> = when {
            strategy == null && !configuration.useAlternativeNames -> descriptor.jsonCachedSerialNames()
            strategy != null -> json.deserializationNamesMap(descriptor).keys
            else -> descriptor.jsonCachedSerialNames() + json.schemaCache[descriptor, JsonDeserializationNamesKey]?.keys.orEmpty()
        }

        for (key in value.keys) {
            if (key !in names && key != polymorphicDiscriminator) {
                throw JsonDecodingException(
                    -1,
                    "Encountered an unknown key '$key' at element: ${renderTagStack()}\n" +
                        "$ignoreUnknownKeysHint\n" +
                        "JSON input: ${value.toString().minify()}"
                )
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
