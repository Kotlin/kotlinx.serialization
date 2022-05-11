/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*

/**
 * [JsonDecoder] which reads given JSON from [AbstractJsonLexer] field by field.
 */
@OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)
internal open class StreamingJsonDecoder(
    final override val json: Json,
    private val mode: WriteMode,
    @JvmField internal val lexer: AbstractJsonLexer,
    descriptor: SerialDescriptor
) : JsonDecoder, AbstractDecoder() {

    override val serializersModule: SerializersModule = json.serializersModule
    private var currentIndex = -1
    private val configuration = json.configuration

    private val elementMarker: JsonElementMarker? = if (configuration.explicitNulls) null else JsonElementMarker(descriptor)

    override fun decodeJsonElement(): JsonElement = JsonTreeReader(json.configuration, lexer).read()

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        try {
            return decodeSerializableValuePolymorphic(deserializer)
        } catch (e: MissingFieldException) {
            throw MissingFieldException(e.message + " at path: " + lexer.path.getPath(), e)
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val newMode = json.switchMode(descriptor)
        lexer.path.pushDescriptor(descriptor)
        lexer.consumeNextToken(newMode.begin)
        checkLeadingComma()
        return when (newMode) {
            // In fact resets current index that these modes rely on
            WriteMode.LIST, WriteMode.MAP, WriteMode.POLY_OBJ -> StreamingJsonDecoder(
                json,
                newMode,
                lexer,
                descriptor
            )
            else -> if (mode == newMode && json.configuration.explicitNulls) {
                this
            } else {
                StreamingJsonDecoder(json, newMode, lexer, descriptor)
            }
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // If we're ignoring unknown keys, we have to skip all undecoded elements,
        // e.g. for object serialization. It can be the case when the descriptor does
        // not have any elements and decodeElementIndex is not invoked at all
        if (json.configuration.ignoreUnknownKeys && descriptor.elementsCount == 0) {
            skipLeftoverElements(descriptor)
        }
        // First consume the object so we know it's correct
        lexer.consumeNextToken(mode.end)
        // Then cleanup the path
        lexer.path.popDescriptor()
    }

    private fun skipLeftoverElements(descriptor: SerialDescriptor) {
        while (decodeElementIndex(descriptor) != DECODE_DONE) {
            // Skip elements
        }
    }

    override fun decodeNotNullMark(): Boolean {
        return !(elementMarker?.isUnmarkedNull ?: false) && lexer.tryConsumeNotNull()
    }

    override fun decodeNull(): Nothing? {
        // Do nothing, null was consumed by `decodeNotNullMark`
        return null
    }

    private fun checkLeadingComma() {
        if (lexer.peekNextToken() == TC_COMMA) {
            lexer.fail("Unexpected leading comma")
        }
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T {
        val isMapKey = mode == WriteMode.MAP && index and 1 == 0
        // Reset previous key
        if (isMapKey) {
            lexer.path.resetCurrentMapKey()
        }
        // Deserialize the key
        val value = super.decodeSerializableElement(descriptor, index, deserializer, previousValue)
        // Put the key to the path
        if (isMapKey) {
            lexer.path.updateCurrentMapKey(value)
        }
        return value
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val index = when (mode) {
            WriteMode.OBJ -> decodeObjectIndex(descriptor)
            WriteMode.MAP -> decodeMapIndex()
            else -> decodeListIndex() // Both for LIST and default polymorphic
        }
        // The element of the next index that will be decoded
        if (mode != WriteMode.MAP) {
            lexer.path.updateDescriptorIndex(index)
        }
        return index
    }

    private fun decodeMapIndex(): Int {
        var hasComma = false
        val decodingKey = currentIndex % 2 != 0
        if (decodingKey) {
            if (currentIndex != -1) {
                hasComma = lexer.tryConsumeComma()
            }
        } else {
            lexer.consumeNextToken(COLON)
        }

        return if (lexer.canConsumeValue()) {
            if (decodingKey) {
                if (currentIndex == -1) lexer.require(!hasComma) { "Unexpected trailing comma" }
                else lexer.require(hasComma) { "Expected comma after the key-value pair" }
            }
            ++currentIndex
        } else {
            if (hasComma) lexer.fail("Expected '}', but had ',' instead")
            CompositeDecoder.DECODE_DONE
        }
    }

    /*
     * Checks whether JSON has `null` value for non-null property or unknown enum value for enum property
     */
    private fun coerceInputValue(descriptor: SerialDescriptor, index: Int): Boolean = json.tryCoerceValue(
        descriptor.getElementDescriptor(index),
        { !lexer.tryConsumeNotNull() },
        { lexer.peekString(configuration.isLenient) },
        { lexer.consumeString() /* skip unknown enum string*/ }
    )

    @Suppress("INVISIBLE_MEMBER")
    private fun decodeObjectIndex(descriptor: SerialDescriptor): Int {
        // hasComma checks are required to properly react on trailing commas
        var hasComma = lexer.tryConsumeComma()
        while (lexer.canConsumeValue()) { // TODO: consider merging comma consumption and this check
            hasComma = false
            val key = decodeStringKey()
            lexer.consumeNextToken(COLON)
            val index = descriptor.getJsonNameIndex(json, key)
            val isUnknown = if (index != UNKNOWN_NAME) {
                if (configuration.coerceInputValues && coerceInputValue(descriptor, index)) {
                    hasComma = lexer.tryConsumeComma()
                    false // Known element, but coerced
                } else {
                    elementMarker?.mark(index)
                    return index // Known element without coercing, return it
                }
            } else {
                true // unknown element
            }

            if (isUnknown) { // slow-path for unknown keys handling
                hasComma = handleUnknown(key)
            }
        }
        if (hasComma) lexer.fail("Unexpected trailing comma")

        return elementMarker?.nextUnmarkedIndex() ?: CompositeDecoder.DECODE_DONE
    }

    private fun handleUnknown(key: String): Boolean {
        if (configuration.ignoreUnknownKeys) {
            lexer.skipElement(configuration.isLenient)
        } else {
            // Here we cannot properly update json path indicies
            // as we do not have a proper SerialDecriptor in our hands
            lexer.failOnUnknownKey(key)
        }
        return lexer.tryConsumeComma()
    }

    private fun decodeListIndex(): Int {
        // Prohibit leading comma
        val hasComma = lexer.tryConsumeComma()
        return if (lexer.canConsumeValue()) {
            if (currentIndex != -1 && !hasComma) lexer.fail("Expected end of the array or comma")
            ++currentIndex
        } else {
            if (hasComma) lexer.fail("Unexpected trailing comma")
            CompositeDecoder.DECODE_DONE
        }
    }


    override fun decodeBoolean(): Boolean {
        /*
         * We prohibit non true/false boolean literals at all as it is considered way too error-prone,
         * but allow quoted literal in relaxed mode for booleans.
         */
        return if (configuration.isLenient) {
            lexer.consumeBooleanLenient()
        } else {
            lexer.consumeBoolean()
        }
    }

    /*
     * The rest of the primitives are allowed to be quoted and unquoted
     * to simplify integrations with third-party API.
     */
    override fun decodeByte(): Byte {
        val value = lexer.consumeNumericLiteral()
        // Check for overflow
        if (value != value.toByte().toLong()) lexer.fail("Failed to parse byte for input '$value'")
        return value.toByte()
    }

    override fun decodeShort(): Short {
        val value = lexer.consumeNumericLiteral()
        // Check for overflow
        if (value != value.toShort().toLong()) lexer.fail("Failed to parse short for input '$value'")
        return value.toShort()
    }

    override fun decodeInt(): Int {
        val value = lexer.consumeNumericLiteral()
        // Check for overflow
        if (value != value.toInt().toLong()) lexer.fail("Failed to parse int for input '$value'")
        return value.toInt()
    }

    override fun decodeLong(): Long {
        return lexer.consumeNumericLiteral()
    }

    override fun decodeFloat(): Float {
        val result = lexer.parseString("float") { toFloat() }
        val specialFp = json.configuration.allowSpecialFloatingPointValues
        if (specialFp || result.isFinite()) return result
        lexer.throwInvalidFloatingPointDecoded(result)
    }

    override fun decodeDouble(): Double {
        val result = lexer.parseString("double") { toDouble() }
        val specialFp = json.configuration.allowSpecialFloatingPointValues
        if (specialFp || result.isFinite()) return result
        lexer.throwInvalidFloatingPointDecoded(result)
    }

    override fun decodeChar(): Char {
        val string = lexer.consumeStringLenient()
        if (string.length != 1) lexer.fail("Expected single char, but got '$string'")
        return string[0]
    }

    private fun decodeStringKey(): String {
        return if (configuration.isLenient) {
            lexer.consumeStringLenientNotNull()
        } else {
            lexer.consumeKeyString()
        }
    }

    override fun decodeString(): String {
        return if (configuration.isLenient) {
            lexer.consumeStringLenientNotNull()
        } else {
            lexer.consumeString()
        }
    }

    override fun decodeInline(inlineDescriptor: SerialDescriptor): Decoder =
        if (inlineDescriptor.isUnsignedNumber) JsonDecoderForUnsignedTypes(lexer, json)
        else super.decodeInline(inlineDescriptor)

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return enumDescriptor.getJsonNameIndexOrThrow(json, decodeString(), " at path " + lexer.path.getPath())
    }
}

@OptIn(ExperimentalSerializationApi::class)
@ExperimentalUnsignedTypes
internal class JsonDecoderForUnsignedTypes(
    private val lexer: AbstractJsonLexer,
    json: Json
) : AbstractDecoder() {
    override val serializersModule: SerializersModule = json.serializersModule
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = error("unsupported")

    override fun decodeInt(): Int = lexer.parseString("UInt") { toUInt().toInt() }
    override fun decodeLong(): Long = lexer.parseString("ULong") { toULong().toLong() }
    override fun decodeByte(): Byte = lexer.parseString("UByte") { toUByte().toByte() }
    override fun decodeShort(): Short = lexer.parseString("UShort") { toUShort().toShort() }
}

private inline fun <T> AbstractJsonLexer.parseString(expectedType: String, block: String.() -> T): T {
    val input = consumeStringLenient()
    try {
        return input.block()
    } catch (e: IllegalArgumentException) {
        fail("Failed to parse type '$expectedType' for input '$input'")
    }
}
