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
internal open class StreamingJsonDecoder(
    final override val json: Json,
    private val mode: WriteMode,
    @JvmField internal val reader: JsonReader
) : JsonDecoder, AbstractDecoder() {

    override val serializersModule: SerializersModule = json.serializersModule
    private var currentIndex = -1
    private val configuration = json.configuration

    override fun decodeJsonElement(): JsonElement = JsonParser(json.configuration, reader).read()

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return decodeSerializableValuePolymorphic(deserializer)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val newMode = json.switchMode(descriptor)
        reader.consumeNextToken(newMode.begin)
        checkLeadingComma()
        return when (newMode) {
            // In fact resets current index that these modes rely on
            WriteMode.LIST, WriteMode.MAP, WriteMode.POLY_OBJ -> StreamingJsonDecoder(
                json,
                newMode,
                reader
            )
            else -> if (mode == newMode) {
                this
            } else {
                StreamingJsonDecoder(json, newMode, reader)
            }
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        reader.consumeNextToken(mode.end)
    }

    override fun decodeNotNullMark(): Boolean {
        return reader.tryConsumeNotNull()
    }

    override fun decodeNull(): Nothing? {
        // Do nothing, null was consumed by `decodeNotNullMark`
        return null
    }

    private fun checkLeadingComma() {
        if (reader.peekNextToken() == TC_COMMA) {
            reader.fail("Unexpected leading comma")
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return when (mode) {
            WriteMode.OBJ -> decodeObjectIndex(descriptor)
            WriteMode.MAP -> decodeMapIndex()
            else -> decodeListIndex() // Both for LIST and default polymorphic
        }
    }

    private fun decodeMapIndex(): Int {
        var hasComma = false
        val decodingKey = currentIndex % 2 != 0
        if (decodingKey) {
            if (currentIndex != -1) {
                hasComma = reader.tryConsumeComma()
            }
        } else {
            reader.consumeNextToken(COLON)
        }

        return if (reader.canConsumeValue()) {
            if (decodingKey) {
                if (currentIndex == -1) reader.require(!hasComma) { "Unexpected trailing comma" }
                else reader.require(hasComma) { "Expected comma after the key-value pair" }
            }
            ++currentIndex
        } else {
            if (hasComma) reader.fail("Expected '}', but had ',' instead")
            CompositeDecoder.DECODE_DONE
        }
    }

    /*
     * Checks whether JSON has `null` value for non-null property or unknown enum value for enum property
     */
    private fun coerceInputValue(descriptor: SerialDescriptor, index: Int): Boolean {
        val elementDescriptor = descriptor.getElementDescriptor(index)
        if (!elementDescriptor.isNullable && !reader.tryConsumeNotNull()) return true
        if (elementDescriptor.kind == SerialKind.ENUM) {
            val enumValue = reader.peekString(configuration.isLenient)
                ?: return false // if value is not a string, decodeEnum() will throw correct exception
            val enumIndex = elementDescriptor.getElementIndex(enumValue)
            if (enumIndex == UNKNOWN_NAME) {
                // Encountered unknown enum value, have to skip it
                reader.consumeString()
                return true
            }
        }
        return false
    }

    private fun decodeObjectIndex(descriptor: SerialDescriptor): Int {
        // hasComma checks are required to properly react on trailing commas
        var hasComma = reader.tryConsumeComma()
        while (reader.canConsumeValue()) { // TODO: consider merging comma consumption and this check
            hasComma = false
            val key = decodeStringKey()
            reader.consumeNextToken(COLON)
            val index = descriptor.getElementIndex(key)
            val isUnknown = if (index != UNKNOWN_NAME) {
                if (configuration.coerceInputValues && coerceInputValue(descriptor, index)) {
                    hasComma = reader.tryConsumeComma()
                    false // Known element, but coerced
                } else {
                    return index // Known element without coercing, return it
                }
            } else {
                true // unknown element
            }

            if (isUnknown) { // slow-path for unknown keys handling
                hasComma = handleUnknown(key)
            }
        }
        if (hasComma) reader.fail("Unexpected trailing comma")
        return CompositeDecoder.DECODE_DONE
    }

    private fun handleUnknown(key: String): Boolean {
        if (configuration.ignoreUnknownKeys) {
            reader.skipElement()
        } else {
            reader.failOnUnknownKey(key)
        }
        return reader.tryConsumeComma()
    }

    private fun decodeListIndex(): Int {
        // Prohibit leading comma
        val hasComma = reader.tryConsumeComma()
        return if (reader.canConsumeValue()) {
            if (currentIndex != -1 && !hasComma) reader.fail("Expected end of the array or comma")
            ++currentIndex
        } else {
            if (hasComma) reader.fail("Unexpected trailing comma")
            CompositeDecoder.DECODE_DONE
        }
    }


    override fun decodeBoolean(): Boolean {
        /*
         * We prohibit non true/false boolean literals at all as it is considered way too error-prone,
         * but allow quoted literal in relaxed mode for booleans.
         */
        return if (configuration.isLenient) {
            reader.consumeBooleanLenient()
        } else {
            reader.consumeBoolean()
        }
    }

    /*
     * The rest of the primitives are allowed to be quoted and unquoted
     * to simplify integrations with third-party API.
     */
    override fun decodeByte(): Byte {
        val value = reader.consumeNumericLiteral()
        // Check for overflow
        if (value != value.toByte().toLong()) reader.fail("Failed to parse byte for input '$value'")
        return value.toByte()
    }

    override fun decodeShort(): Short {
        val value = reader.consumeNumericLiteral()
        // Check for overflow
        if (value != value.toShort().toLong()) reader.fail("Failed to parse byte for input '$value'")
        return value.toShort()
    }

    override fun decodeInt(): Int {
        val value = reader.consumeNumericLiteral()
        // Check for overflow
        if (value != value.toInt().toLong()) reader.fail("Failed to parse byte for input '$value'")
        return value.toInt()
    }

    override fun decodeLong(): Long {
        return reader.consumeNumericLiteral()
    }

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

    override fun decodeChar(): Char {
        val string = reader.consumeStringLenient()
        if (string.length != 1) reader.fail("Expected single char, but got '$string'")
        return string[0]
    }

    private fun decodeStringKey(): String {
        return if (configuration.isLenient) {
            reader.consumeStringLenient()
        } else {
            reader.consumeKeyString()
        }
    }

    override fun decodeString(): String {
        return if (configuration.isLenient) {
            reader.consumeStringLenient()
        } else {
            reader.consumeString()
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
    val input = consumeStringLenient()
    try {
        return input.block()
    } catch (e: IllegalArgumentException) {
        fail("Failed to parse type '$expectedType' for input '$input'")
    }
}
