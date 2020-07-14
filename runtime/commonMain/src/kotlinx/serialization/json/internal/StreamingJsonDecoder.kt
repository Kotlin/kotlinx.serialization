/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlinx.serialization.internal.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*

/**
 * [JsonDecoder] which reads given JSON from [JsonReader] field by field.
 */
internal class StreamingJsonDecoder internal constructor(
    public override val json: Json,
    private val mode: WriteMode,
    @JvmField internal val reader: JsonReader
) : JsonDecoder, AbstractDecoder() {

    public override val serializersModule: SerializersModule = json.serializersModule
    private var currentIndex = -1
    private val configuration = json.configuration

    // must override public open val updateMode: UpdateMode defined in kotlinx.serialization.json.JsonDecoder
    // because it inherits many implementations of it
    @Suppress("DEPRECATION")
    @Deprecated(updateModeDeprecated, level = DeprecationLevel.HIDDEN)
    override val updateMode: UpdateMode
        get() = UpdateMode.OVERWRITE

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
        return if (configuration.isLenient) {
            reader.takeString().toBooleanStrict()
        } else {
            reader.takeBooleanStringUnquoted().toBooleanStrict()
        }
    }

    /*
     * The rest of the primitives are allowed to be quoted and unqouted
     * to simplify integrations with third-party API.
     */
    override fun decodeByte(): Byte = reader.takeString().parse("byte") { toByte() }
    override fun decodeShort(): Short = reader.takeString().parse("short") { toShort() }
    override fun decodeInt(): Int = reader.takeString().parse("int") { toInt() }
    override fun decodeLong(): Long = reader.takeString().parse("long") { toLong() }
    override fun decodeFloat(): Float = reader.takeString().parse("float") { toFloat() }
    override fun decodeDouble(): Double = reader.takeString().parse("double") { toDouble() }
    override fun decodeChar(): Char = reader.takeString().parse("char") { single() }

    override fun decodeString(): String {
        return if (configuration.isLenient) {
            reader.takeString()
        } else {
            reader.takeStringQuoted()
        }
    }

    private inline fun <T> String.parse(type: String, block: String.() -> T): T {
        try {
            return block()
        } catch (e: Throwable) {
            reader.fail("Failed to parse '$type'")
        }
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return enumDescriptor.getElementIndexOrThrow(decodeString())
    }
}
