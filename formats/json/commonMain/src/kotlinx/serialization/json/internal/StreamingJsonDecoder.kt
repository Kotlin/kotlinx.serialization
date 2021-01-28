/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*

/**
 * [JsonDecoder] which reads given JSON from [JsonReader] field by field.
 */
@OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)
internal open class StreamingJsonDecoder internal constructor(
    public override val json: Json,
    private val mode: WriteMode,
    @JvmField internal val reader: JsonReader
) : JsonDecoder, AbstractDecoder() {

    public override val serializersModule: SerializersModule = json.serializersModule
    private var currentIndex = -1
    private val configuration = json.configuration

    public override fun decodeJsonElement(): JsonElement = JsonParser(json.configuration, reader).read()

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return decodeSerializableValuePolymorphic(deserializer)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val newMode = json.switchMode(descriptor)
        if (newMode.begin != INVALID) {
            reader.requireTokenClass(newMode.beginTc) { "Expected '${newMode.begin}, kind: ${descriptor.kind}'" }
            reader.nextToken()
        }
        return when (newMode) {
            WriteMode.LIST, WriteMode.MAP, WriteMode.POLY_OBJ -> StreamingJsonDecoder(
                json,
                newMode,
                reader
            ) // need fresh cur index
            else -> if (mode == newMode) this else
                StreamingJsonDecoder(json, newMode, reader) // todo: reuse instance per mode
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (mode.end != INVALID) {
            reader.requireTokenClass(mode.endTc) { "Expected '${mode.end}'" }
            reader.nextToken()
        }
    }

    override fun decodeNotNullMark(): Boolean {
        return reader.tokenClass != TC_NULL
    }

    override fun decodeNull(): Nothing? {
        reader.requireTokenClass(TC_NULL) { "Expected 'null' literal" }
        reader.nextToken()
        return null
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val tokenClass = reader.tokenClass
        if (tokenClass == TC_COMMA) {
            reader.require(currentIndex != -1, reader.currentPosition) { "Unexpected leading comma" }
            reader.nextToken()
        }
        return when (mode) {
            WriteMode.LIST -> decodeListIndex(tokenClass)
            WriteMode.MAP -> decodeMapIndex(tokenClass)
            WriteMode.POLY_OBJ -> {
                when (++currentIndex) {
                    0 -> 0
                    1 -> 1
                    else -> {
                        CompositeDecoder.DECODE_DONE
                    }
                }
            }
            else -> decodeObjectIndex(tokenClass, descriptor)
        }
    }

    private fun decodeMapIndex(tokenClass: Byte): Int {
        if (tokenClass != TC_COMMA && currentIndex % 2 == 1) {
            reader.requireTokenClass(TC_END_OBJ) { "Expected end of the object or comma" }
        }
        if (currentIndex % 2 == 0) {
            reader.requireTokenClass(TC_COLON) { "Expected ':' after the key" }
            reader.nextToken()
        }
        return if (!reader.canBeginValue) {
            reader.require(tokenClass != TC_COMMA) { "Unexpected trailing comma" }
            CompositeDecoder.DECODE_DONE
        } else {
            ++currentIndex
        }
    }

    /*
     * Checks whether JSON has `null` value for non-null property or unknown enum value for enum property
     */
    private fun coerceInputValue(descriptor: SerialDescriptor, index: Int): Boolean {
        val elementDescriptor = descriptor.getElementDescriptor(index)
        if (reader.tokenClass == TC_NULL && !elementDescriptor.isNullable) return true // null for non-nullable
        if (elementDescriptor.kind == SerialKind.ENUM) {
            val enumValue = reader.peekString(configuration.isLenient)
                    ?: return false // if value is not a string, decodeEnum() will throw correct exception
            val enumIndex = elementDescriptor.getElementIndex(enumValue)
            if (enumIndex == UNKNOWN_NAME) return true
        }
        return false
    }

    private fun decodeObjectIndex(tokenClass: Byte, descriptor: SerialDescriptor): Int {
        if (tokenClass == TC_COMMA && !reader.canBeginValue) {
            reader.fail("Unexpected trailing comma")
        }

        while (reader.canBeginValue) {
            ++currentIndex
            val key = decodeString()
            reader.requireTokenClass(TC_COLON) { "Expected ':'" }
            reader.nextToken()
            val index = descriptor.getElementIndex(key)
            val isUnknown = if (index != UNKNOWN_NAME) {
                if (configuration.coerceInputValues && coerceInputValue(descriptor, index)) {
                    false // skip known element
                } else {
                    return index // read known element
                }
            } else {
                true // unknown element
            }

            if (isUnknown && !configuration.ignoreUnknownKeys) {
                reader.fail("Encountered an unknown key '$key'.\n$ignoreUnknownKeysHint")
            } else {
                reader.skipElement()
            }

            if (reader.tokenClass == TC_COMMA) {
                reader.nextToken()
                reader.require(reader.canBeginValue, reader.currentPosition) { "Unexpected trailing comma" }
            }
        }
        return CompositeDecoder.DECODE_DONE
    }

    private fun decodeListIndex(tokenClass: Byte): Int {
        // Prohibit leading comma
        if (tokenClass != TC_COMMA && currentIndex != -1) {
            reader.requireTokenClass(TC_END_LIST) { "Expected end of the array or comma" }
        }
        return if (!reader.canBeginValue) {
            reader.require(tokenClass != TC_COMMA) { "Unexpected trailing comma" }
            CompositeDecoder.DECODE_DONE
        } else {
            ++currentIndex
        }
    }

    override fun decodeBoolean(): Boolean {
        /*
         * We prohibit non true/false boolean literals at all as it is considered way too error-prone,
         * but allow quoted literal in relaxed mode for booleans.
         */
        val string = if (configuration.isLenient) {
            reader.takeString()
        } else {
            reader.takeBooleanStringUnquoted()
        }
        string.toBooleanStrictOrNull()?.let { return it }
        reader.fail("Failed to parse type 'boolean' for input '$string'")
    }

    /*
     * The rest of the primitives are allowed to be quoted and unqouted
     * to simplify integrations with third-party API.
     */
    override fun decodeByte(): Byte = reader.parseString("byte") { toByte() }
    override fun decodeShort(): Short = reader.parseString("short") { toShort() }
    override fun decodeInt(): Int = reader.parseString("int") { toInt() }
    override fun decodeLong(): Long = reader.parseString("long") { toLong() }

    override fun decodeFloat(): Float {
        val result = reader.parseString("float") { toFloat() }
        val specialFp = json.configuration.allowSpecialFloatingPointValues
        if (specialFp || result.isFinite()) return result
        reader.throwInvalidFloatingPointDecoded(result)
    }

    override fun decodeDouble(): Double {
        val result = reader.parseString("double") { toDouble() }
        val specialFp = json.configuration.allowSpecialFloatingPointValues
        if (specialFp || result.isFinite()) return result
        reader.throwInvalidFloatingPointDecoded(result)
    }

    override fun decodeChar(): Char = reader.parseString("char") { single() }

    override fun decodeString(): String {
        return if (configuration.isLenient) {
            reader.takeString()
        } else {
            reader.takeStringQuoted()
        }
    }

    override fun decodeInline(inlineDescriptor: SerialDescriptor): Decoder {
        return if (inlineDescriptor.isUnsignedNumber) JsonDecoderForUnsignedTypes(reader, json) else this
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return enumDescriptor.getElementIndexOrThrow(decodeString())
    }
}

@OptIn(ExperimentalSerializationApi::class)
@ExperimentalUnsignedTypes
internal class JsonDecoderForUnsignedTypes(
    private val reader: JsonReader,
    json: Json
) : AbstractDecoder() {
    override val serializersModule: SerializersModule = json.serializersModule
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = error("unsupported")

    override fun decodeInt(): Int = reader.parseString("UInt") { toUInt().toInt() }
    override fun decodeLong(): Long = reader.parseString("ULong") { toULong().toLong() }
    override fun decodeByte(): Byte = reader.parseString("UByte") { toUByte().toByte() }
    override fun decodeShort(): Short = reader.parseString("UShort") { toUShort().toShort() }
}

private inline fun <T> JsonReader.parseString(expectedType: String, block: String.() -> T): T {
    val input = takeString()
    try {
        return input.block()
    } catch (e: IllegalArgumentException) {
        fail("Failed to parse type '$expectedType' for input '$input'")
    }
}
