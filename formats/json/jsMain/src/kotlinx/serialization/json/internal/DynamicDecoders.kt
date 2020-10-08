/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.math.*

/**
 * [https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER]
 */
internal const val MAX_SAFE_INTEGER: Double = 9007199254740991.toDouble() // 2^53 - 1

@JsName("decodeDynamic")
internal fun <T> Json.decodeDynamic(deserializer: DeserializationStrategy<T>, dynamic: dynamic): T {
    val input = when (jsTypeOf(dynamic)) {
        "boolean", "number", "string" -> PrimitiveDynamicInput(dynamic, this)
        else -> {
            if (js("Array.isArray(dynamic)")) {
                DynamicListInput(dynamic, this)
            } else {
                DynamicInput(dynamic, this)
            }
        }
    }
    return input.decodeSerializableValue(deserializer)
}

@OptIn(ExperimentalSerializationApi::class)
private open class DynamicInput(
    protected val value: dynamic,
    override val json: Json
) : NamedValueDecoder(), JsonDecoder {

    override val serializersModule: SerializersModule
        get() = json.serializersModule

    private var currentPosition = 0

    override fun decodeJsonElement(): JsonElement {
        val tag = currentTagOrNull
        if (tag != null) { // reading a nested value, not the current one
            return json.decodeFromDynamic(JsonElement.serializer(), value[tag])
        }

        val keys: dynamic = js("Object").keys(value)
        val size: Int = keys.length as Int
        return buildJsonObject {
            for (i in 0 until size) {
                val key = keys[i]
                val value = json.decodeDynamic(JsonElement.serializer(), value[key])
                put(key.toString(), value)
            }
        }
    }

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return decodeSerializableValuePolymorphic(deserializer)
    }

    override fun composeName(parentName: String, childName: String): String = childName

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (currentPosition < descriptor.elementsCount) {
            val name = descriptor.getTag(currentPosition++)
            if (value[name] !== undefined) return currentPosition - 1
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun decodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor): Int =
        enumDescriptor.getElementIndexOrThrow(getByTag(tag) as String)

    protected open fun getByTag(tag: String): dynamic = value[tag]

    override fun decodeTaggedChar(tag: String): Char {
        return when (val value = getByTag(tag)) {
            is String -> if (value.length == 1) value[0] else throw SerializationException("$value can't be represented as Char")
            is Number -> value.toChar()
            else -> throw SerializationException("$value can't be represented as Char")
        }
    }

    override fun decodeTaggedLong(tag: String): Long {
        val value = getByTag(tag)
        val number = value as? Double ?: throw SerializationException("$value is not a Number")
        return toJavascriptLong(number)
    }

    protected fun toJavascriptLong(number: Double): Long {
        val canBeConverted = number.isFinite() && floor(number) == number
        if (!canBeConverted)
            throw SerializationException("$number can't be represented as Long because it is not finite or has non-zero fractional part")
        val inBound = abs(number) <= MAX_SAFE_INTEGER
        if (!inBound)
            throw SerializationException("$number can't be deserialized to Long due to a potential precision loss")
        return number.toLong()
    }

    override fun decodeTaggedValue(tag: String): Any {
        val o = getByTag(tag) ?: throwMissingTag(tag)
        return o as Any
    }

    override fun decodeTaggedNotNullMark(tag: String): Boolean {
        val o = getByTag(tag)
        if (o === undefined) throwMissingTag(tag)
        @Suppress("SENSELESS_COMPARISON") // null !== undefined !
        return o != null
    }

    private fun throwMissingTag(tag: String) {
        throw SerializationException("Value for field $tag is missing")
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val currentValue = currentTagOrNull?.let { value[it] } ?: value
        val kind = when (descriptor.kind) {
            is PolymorphicKind -> {
                if (json.configuration.useArrayPolymorphism) StructureKind.LIST
                else StructureKind.MAP
            }
            else -> descriptor.kind
        }
        return when (kind) {
            StructureKind.LIST -> DynamicListInput(currentValue, json)
            StructureKind.MAP -> DynamicMapInput(currentValue, json)
            else -> DynamicInput(currentValue, json)
        }
    }
}

private class DynamicMapInput(
    value: dynamic,
    json: Json,
) : DynamicInput(value, json) {
    private val keys: dynamic = js("Object").keys(value)
    private val size: Int = (keys.length as Int) * 2
    private var currentPosition = -1
    private val isKey: Boolean get() = currentPosition % 2 == 0

    override fun elementName(desc: SerialDescriptor, index: Int): String {
        val i = index / 2
        return keys[i] as String
    }

    /*
     * Decode tagger primitives rationale:
     * In JS, key type of js("{1:2}") is String for any primitive.
     * In order to properly deserialize them, we should additionally check
     * for String type, to properly handle it
     */
    private fun throwIllegalKeyType(tag: String, value: Any, type: String): Nothing {
        throw SerializationException("Property $tag is not valid type $type: $value")
    }

    override fun decodeTaggedByte(tag: String): Byte =
        decodeMapKey(tag, "byte", { super.decodeTaggedByte(tag) }, { toByteOrNull() })

    override fun decodeTaggedShort(tag: String): Short =
        decodeMapKey(tag, "short", { super.decodeTaggedShort(tag) }, { toShortOrNull() })

    override fun decodeTaggedInt(tag: String): Int =
        decodeMapKey(tag, "int", { super.decodeTaggedInt(tag) }, { toIntOrNull() })

    override fun decodeTaggedLong(tag: String): Long = decodeMapKey(tag, "long", { super.decodeTaggedLong(tag) }) {
        toJavascriptLong(toDoubleOrNull() ?: throwIllegalKeyType(tag, this, "long"))
    }

    override fun decodeTaggedFloat(tag: String): Float =
        decodeMapKey(tag, "float", { super.decodeTaggedFloat(tag) }, { toFloatOrNull() })

    override fun decodeTaggedDouble(tag: String): Double =
        decodeMapKey(tag, "double", { super.decodeTaggedDouble(tag) }, { toDoubleOrNull() })

    private inline fun <reified T> decodeMapKey(
        tag: String,
        type: String,
        decode: (tag: String) -> T,
        cast: String.() -> T?
    ): T {
        if (isKey) {
            val value = decodeTaggedValue(tag)
            if (value !is String) return decode(tag)
            return value.toString().cast() ?: throwIllegalKeyType(tag, value, type)
        }
        return decode(tag)
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (currentPosition < size - 1) {
            val i = currentPosition++ / 2
            val name = keys[i] as String
            if (this.value[name] !== undefined) return currentPosition
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun getByTag(tag: String): dynamic {
        return if (currentPosition % 2 == 0) tag else value[tag]
    }
}

private class DynamicListInput(
    value: dynamic,
    json: Json,
) : DynamicInput(value, json) {
    private val size = value.length as Int
    private var currentPosition = -1

    override fun elementName(desc: SerialDescriptor, index: Int): String = (index).toString()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (currentPosition < size - 1) {
            val o = value[++currentPosition]
            if (o !== undefined) return currentPosition
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun decodeJsonElement(): JsonElement {
        val tag = currentTagOrNull
        if (tag != null) { // reading a nested value, not the current one
            return json.decodeFromDynamic(JsonElement.serializer(), value[tag])
        }
        return buildJsonArray {
            for (i in 0 until size) {
                add(json.decodeFromDynamic(JsonElement.serializer(), value[i]))
            }
        }
    }
}

private class PrimitiveDynamicInput(
    value: dynamic,
    json: Json,
) : DynamicInput(value, json) {
    init {
        pushTag("primitive")
    }

    override fun getByTag(tag: String): dynamic = value

    override fun decodeJsonElement(): JsonElement {
        val str = value.toString()
        return when (jsTypeOf(value)) {
            "boolean" -> JsonPrimitive(str.toBoolean())
            "number" -> {
                val l = str.toLongOrNull()
                if (l != null) return JsonPrimitive(l)
                val d = str.toDoubleOrNull()
                if (d != null) return JsonPrimitive(d)
                return JsonPrimitive(str)
            }
            else -> JsonPrimitive(str)
        }
    }
}
