/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("LeakingThis")

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*

internal fun <T> Json.readJson(element: JsonElement, deserializer: DeserializationStrategy<T>): T {
    val input = when (element) {
        is JsonObject -> JsonTreeInput(this, element)
        is JsonArray -> JsonTreeListInput(this, element)
        is JsonLiteral, JsonNull -> JsonPrimitiveInput(this, element as JsonPrimitive)
    }
    return input.decode(deserializer)
}

private sealed class AbstractJsonTreeInput(
    override val json: Json,
    open val obj: JsonElement
) : NamedValueDecoder(), JsonInput {

    override val context: SerialModule
        get() = json.context

    @JvmField
    protected val configuration = json.configuration

    private fun currentObject() = currentTagOrNull?.let { currentElement(it) } ?: obj

    override fun decodeJson(): JsonElement = currentObject()

    @Suppress("DEPRECATION")
    override val updateMode: UpdateMode
        get() = configuration.updateMode

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return decodeSerializableValuePolymorphic(deserializer)
    }

    override fun composeName(parentName: String, childName: String): String = childName

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        val currentObject = currentObject()
        return when (desc.kind) {
            StructureKind.LIST, is PolymorphicKind -> JsonTreeListInput(json, cast(currentObject))
            StructureKind.MAP -> json.selectMapMode(
                desc,
                { JsonTreeMapInput(json, cast(currentObject)) },
                { JsonTreeListInput(json, cast(currentObject)) }
            )
            else -> JsonTreeInput(json, cast(currentObject))
        }
    }

    protected open fun getValue(tag: String): JsonPrimitive {
        val currentElement = currentElement(tag)
        return currentElement as? JsonPrimitive ?: throw JsonDecodingException(
            -1,
            "Expected JsonPrimitive at $tag, found $currentElement"
        )
    }

    protected abstract fun currentElement(tag: String): JsonElement

    override fun decodeTaggedChar(tag: String): Char {
        val o = getValue(tag)
        return if (o.content.length == 1) o.content[0] else throw SerializationException("$o can't be represented as Char")
    }

    override fun decodeTaggedEnum(tag: String, enumDescription: SerialDescriptor): Int =
        enumDescription.getElementIndexOrThrow(getValue(tag).content)

    override fun decodeTaggedNull(tag: String): Nothing? = null

    override fun decodeTaggedNotNullMark(tag: String): Boolean = currentElement(tag) !== JsonNull

    override fun decodeTaggedUnit(tag: String) {
        return
    }

    override fun decodeTaggedBoolean(tag: String): Boolean = getValue(tag).boolean
    override fun decodeTaggedByte(tag: String): Byte = getValue(tag).int.toByte()
    override fun decodeTaggedShort(tag: String) = getValue(tag).int.toShort()
    override fun decodeTaggedInt(tag: String) = getValue(tag).int
    override fun decodeTaggedLong(tag: String) = getValue(tag).long
    override fun decodeTaggedFloat(tag: String) = getValue(tag).float
    override fun decodeTaggedDouble(tag: String) = getValue(tag).double
    override fun decodeTaggedString(tag: String) = getValue(tag).content
}

private class JsonPrimitiveInput(json: Json, override val obj: JsonPrimitive) : AbstractJsonTreeInput(json, obj) {

    init {
        pushTag(PRIMITIVE_TAG)
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int = 0

    override fun currentElement(tag: String): JsonElement {
        require(tag === PRIMITIVE_TAG) { "This input can only handle primitives with '$PRIMITIVE_TAG' tag" }
        return obj
    }
}

private open class JsonTreeInput(json: Json, override val obj: JsonObject) : AbstractJsonTreeInput(json, obj) {
    private var position = 0

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        while (position < desc.elementsCount) {
            val name = desc.getTag(position++)
            if (name in obj) {
                return position - 1
            }
        }
        return CompositeDecoder.READ_DONE
    }

    override fun currentElement(tag: String): JsonElement = obj.getValue(tag)

    override fun endStructure(desc: SerialDescriptor) {
        if (!configuration.strictMode || desc is PolymorphicClassDescriptor) return

        // Validate keys
        val names = HashSet<String>(desc.elementsCount)
        for (i in 0 until desc.elementsCount) {
            names += desc.getElementName(i)
        }

        for (key in obj.keys) {
            if (key !in names) throw jsonUnknownKeyException(-1, key)
        }
    }
}

private class JsonTreeMapInput(json: Json, override val obj: JsonObject) : JsonTreeInput(json, obj) {
    private val keys = obj.keys.toList()
    private val size: Int = keys.size * 2
    private var position = -1

    override fun elementName(desc: SerialDescriptor, index: Int): String {
        val i = index / 2
        return keys[i]
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        while (position < size - 1) {
            position++
            return position
        }
        return CompositeDecoder.READ_DONE
    }

    override fun currentElement(tag: String): JsonElement {
        return if (position % 2 == 0) JsonLiteral(tag) else obj.getValue(tag)
    }

    override fun endStructure(desc: SerialDescriptor) {
        // do nothing, maps do not have strict keys, so strict mode check is omitted
    }
}

private class JsonTreeListInput(json: Json, override val obj: JsonArray) : AbstractJsonTreeInput(json, obj) {
    private val size = obj.content.size
    private var currentIndex = -1

    override fun elementName(desc: SerialDescriptor, index: Int): String = (index).toString()

    override fun currentElement(tag: String): JsonElement {
        return obj[tag.toInt()]
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        while (currentIndex < size - 1) {
            currentIndex++
            return currentIndex
        }
        return CompositeDecoder.READ_DONE
    }
}
