/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("LeakingThis")

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.encoding.updateModeDeprecated
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

    // must override public final val updateMode: UpdateMode defined in kotlinx.serialization.NamedValueDecoder
    // because it inherits many implementations of it
    @Suppress("DEPRECATION")
    @Deprecated(updateModeDeprecated, level = DeprecationLevel.HIDDEN)
    override val updateMode: UpdateMode
        get() = UpdateMode.OVERWRITE

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

    protected open fun getValue(tag: String): JsonPrimitive {
        val currentElement = currentElement(tag)
        return currentElement as? JsonPrimitive ?: throw JsonDecodingException(
            -1,
            "Expected JsonPrimitive at $tag, found $currentElement", currentObject().toString()
        )
    }

    protected abstract fun currentElement(tag: String): JsonElement

    override fun decodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor): Int =
        enumDescriptor.getElementIndexOrThrow(getValue(tag).content)

    override fun decodeTaggedNull(tag: String): Nothing? = null

    override fun decodeTaggedNotNullMark(tag: String): Boolean = currentElement(tag) !== JsonNull

    override fun decodeTaggedBoolean(tag: String): Boolean {
        val value = getValue(tag)
        if (!json.configuration.isLenient) {
            val literal = value as JsonLiteral
            if (literal.isString) throw JsonDecodingException(
                -1, "Boolean literal for key '$tag' should be unquoted.\n$lenientHint", currentObject().toString()
            )
        }
        return value.boolean
    }

    override fun decodeTaggedByte(tag: String) = getValue(tag).primitive("byte") { int.toByte() }
    override fun decodeTaggedShort(tag: String) = getValue(tag).primitive("short") { int.toShort() }
    override fun decodeTaggedInt(tag: String) = getValue(tag).primitive("int") { int }
    override fun decodeTaggedLong(tag: String) = getValue(tag).primitive("long") { long }
    override fun decodeTaggedFloat(tag: String) = getValue(tag).primitive("float") { float }
    override fun decodeTaggedDouble(tag: String) = getValue(tag).primitive("double") { double }
    override fun decodeTaggedChar(tag: String): Char = getValue(tag).primitive("char") { content.single() }

    private inline fun <T: Any> JsonPrimitive.primitive(primitive: String, block: JsonPrimitive.() -> T): T {
        try {
            return block()
        } catch (e: Throwable) {
            throw JsonDecodingException(-1, "Failed to parse '$primitive'", currentObject().toString())
        }
    }

    override fun decodeTaggedString(tag: String): String {
        val value = getValue(tag)
        if (!json.configuration.isLenient) {
            val literal = value as JsonLiteral
            if (!literal.isString) throw JsonDecodingException(
                -1, "String literal for key '$tag' should be quoted.\n$lenientHint", currentObject().toString()
            )
        }
        return value.content
    }
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
    private fun coerceInputValue(descriptor: SerialDescriptor, index: Int, tag: String): Boolean {
        val elementDescriptor = descriptor.getElementDescriptor(index)
        if (currentElement(tag) is JsonNull && !elementDescriptor.isNullable) return true // null for non-nullable
        if (elementDescriptor.kind == SerialKind.ENUM) {
            val enumValue = (currentElement(tag) as? JsonPrimitive)?.contentOrNull
                    ?: return false // if value is not a string, decodeEnum() will throw correct exception
            val enumIndex = elementDescriptor.getElementIndex(enumValue)
            if (enumIndex == CompositeDecoder.UNKNOWN_NAME) return true
        }
        return false
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (position < descriptor.elementsCount) {
            val name = descriptor.getTag(position++)
            if (name in value && (!configuration.coerceInputValues || !coerceInputValue(descriptor, position - 1, name))) {
                return position - 1
            }
        }
        return CompositeDecoder.DECODE_DONE
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
        val names = descriptor.cachedSerialNames()
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
