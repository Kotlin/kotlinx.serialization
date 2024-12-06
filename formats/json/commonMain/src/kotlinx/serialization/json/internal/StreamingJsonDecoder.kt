/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*

/**
 * [JsonDecoder] which reads given JSON from [AbstractJsonLexer] field by field.
 */
@OptIn(ExperimentalSerializationApi::class)
internal open class StreamingJsonDecoder(
    final override val json: Json,
    private val mode: WriteMode,
    @JvmField internal val lexer: AbstractJsonLexer,
    descriptor: SerialDescriptor,
    discriminatorHolder: DiscriminatorHolder?
) : JsonDecoder, ChunkedDecoder, AbstractDecoder() {

    // A mutable reference to the discriminator that have to be skipped when in optimistic phase
    // of polymorphic serialization, see `decodeSerializableValue`
    internal class DiscriminatorHolder(@JvmField var discriminatorToSkip: String?)

    private fun DiscriminatorHolder?.trySkip(unknownKey: String): Boolean {
        if (this == null) return false
        if (discriminatorToSkip == unknownKey) {
            discriminatorToSkip = null
            return true
        }
        return false
    }


    override val serializersModule: SerializersModule = json.serializersModule
    private var currentIndex = -1
    private var discriminatorHolder: DiscriminatorHolder? = discriminatorHolder
    private val configuration = json.configuration

    private val elementMarker: JsonElementMarker? = if (configuration.explicitNulls) null else JsonElementMarker(descriptor)

    override fun decodeJsonElement(): JsonElement = JsonTreeReader(json.configuration, lexer).read()

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        try {
            /*
             * This is an optimized path over decodeSerializableValuePolymorphic(deserializer):
             * dSVP reads the very next JSON tree into a memory as JsonElement and then runs TreeJsonDecoder over it
             * in order to deal with an arbitrary order of keys, but with the price of additional memory pressure
             * and CPU consumption.
             * We would like to provide the best possible performance for data produced by kotlinx.serialization
             * itself, for that we do the following optimistic optimization:
             *
             * 0) Remember current position in the string
             * 1) Read the very next key of JSON structure
             * 2) If it matches*  the discriminator key, read the value, remember current position
             * 3) Return the value, recover an initial position
             * (*) -- if it doesn't match, fallback to dSVP method.
             */
            if (deserializer !is AbstractPolymorphicSerializer<*> || json.configuration.useArrayPolymorphism) {
                return deserializer.deserialize(this)
            }

            val discriminator = deserializer.descriptor.classDiscriminator(json)
            val type = lexer.peekLeadingMatchingValue(discriminator, configuration.isLenient)
                ?: // Fallback to slow path if we haven't found discriminator on first try
                return decodeSerializableValuePolymorphic<T>(deserializer as DeserializationStrategy<T>) { lexer.path.getPath() }

            @Suppress("UNCHECKED_CAST")
            val actualSerializer = try {
                    deserializer.findPolymorphicSerializer(this, type)
                } catch (it: SerializationException) { // Wrap SerializationException into JsonDecodingException to preserve position, path, and input.
                    // Split multiline message from private core function:
                    // core/commonMain/src/kotlinx/serialization/internal/AbstractPolymorphicSerializer.kt:102
                    val message = it.message!!.substringBefore('\n').removeSuffix(".")
                    val hint = it.message!!.substringAfter('\n', missingDelimiterValue = "")
                    lexer.fail(message, hint = hint)
                } as DeserializationStrategy<T>

            discriminatorHolder = DiscriminatorHolder(discriminator)
            return actualSerializer.deserialize(this)

        } catch (e: MissingFieldException) {
            // Add "at path" if and only if we've just caught an exception and it hasn't been augmented yet
            if (e.message!!.contains("at path")) throw e
            // NB: we could've use some additional flag marker or augment the stacktrace, but it seemed to be as too much of a burden
            throw MissingFieldException(e.missingFields, e.message + " at path: " + lexer.path.getPath(), e)
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
                descriptor,
                discriminatorHolder
            )
            else -> if (mode == newMode && json.configuration.explicitNulls) {
                this
            } else {
                StreamingJsonDecoder(json, newMode, lexer, descriptor, discriminatorHolder)
            }
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // If we're ignoring unknown keys, we have to skip all un-decoded elements,
        // e.g. for object serialization. It can be the case when the descriptor does
        // not have any elements and decodeElementIndex is not invoked at all
        if (descriptor.elementsCount == 0 && descriptor.ignoreUnknownKeys(json)) {
            skipLeftoverElements(descriptor)
        }
        if (lexer.tryConsumeComma() && !json.configuration.allowTrailingComma) lexer.invalidTrailingComma("")
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
        return !(elementMarker?.isUnmarkedNull ?: false) && !lexer.tryConsumeNull()
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
                if (currentIndex == -1) lexer.require(!hasComma) { "Unexpected leading comma" }
                else lexer.require(hasComma) { "Expected comma after the key-value pair" }
            }
            ++currentIndex
        } else {
            if (hasComma && !json.configuration.allowTrailingComma) lexer.invalidTrailingComma()
            CompositeDecoder.DECODE_DONE
        }
    }

    /*
     * Checks whether JSON has `null` value for non-null property or unknown enum value for enum property
     */
    private fun coerceInputValue(descriptor: SerialDescriptor, index: Int): Boolean = json.tryCoerceValue(
        descriptor, index,
        { lexer.tryConsumeNull(it) },
        { lexer.peekString(configuration.isLenient) },
        { lexer.consumeString() /* skip unknown enum string*/ }
    )

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
                hasComma = handleUnknown(descriptor, key)
            }
        }
        if (hasComma && !json.configuration.allowTrailingComma) lexer.invalidTrailingComma()

        return elementMarker?.nextUnmarkedIndex() ?: CompositeDecoder.DECODE_DONE
    }

    private fun handleUnknown(descriptor: SerialDescriptor, key: String): Boolean {
        if (descriptor.ignoreUnknownKeys(json) || discriminatorHolder.trySkip(key)) {
            lexer.skipElement(configuration.isLenient)
        } else {
            // Since path is updated on key decoding, it ends with the key that was successfully decoded last,
            // and we need to remove it to correctly point to the outer structure.
            lexer.path.popDescriptor()
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
            if (hasComma && !json.configuration.allowTrailingComma) lexer.invalidTrailingComma("array")
            CompositeDecoder.DECODE_DONE
        }
    }

    /*
     * The primitives are allowed to be quoted and unquoted
     * to simplify map key parsing and integrations with third-party API.
     */
    override fun decodeBoolean(): Boolean {
        return lexer.consumeBooleanLenient()
    }

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

    override fun decodeStringChunked(consumeChunk: (chunk: String) -> Unit) {
        lexer.consumeStringChunked(configuration.isLenient, consumeChunk)
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder =
        if (descriptor.isUnsignedNumber) JsonDecoderForUnsignedTypes(lexer, json)
        else super.decodeInline(descriptor)

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return enumDescriptor.getJsonNameIndexOrThrow(json, decodeString(), " at path " + lexer.path.getPath())
    }
}

@JsonFriendModuleApi // used in json-tests
public fun <T> decodeStringToJsonTree(
    json: Json,
    deserializer: DeserializationStrategy<T>,
    source: String
): JsonElement {
    val lexer = StringJsonLexer(json, source)
    val input = StreamingJsonDecoder(json, WriteMode.OBJ, lexer, deserializer.descriptor, null)
    val tree = input.decodeJsonElement()
    lexer.expectEof()
    return tree
}

@OptIn(ExperimentalSerializationApi::class)
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
