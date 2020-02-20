/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*

/**
 * [JsonInput] which reads given JSON from [JsonReader] field by field.
 */
internal class StreamingJsonInput internal constructor(
    public override val json: Json,
    private val mode: WriteMode,
    @JvmField internal val reader: JsonReader
) : JsonInput, ElementValueDecoder() {

    public override val context: SerialModule = json.context
    private var currentIndex = -1
    private val configuration = json.configuration
    
    public override fun decodeJson(): JsonElement = JsonParser(json.configuration, reader).read()

    @Suppress("DEPRECATION")
    override val updateMode: UpdateMode
        get() = configuration.updateMode

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return decodeSerializableValuePolymorphic(deserializer)
    }

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        val newMode = json.switchMode(desc)
        if (newMode.begin != INVALID) {
            reader.requireTokenClass(newMode.beginTc) { "Expected '${newMode.begin}, kind: ${desc.kind}'" }
            reader.nextToken()
        }
        return when (newMode) {
            WriteMode.LIST, WriteMode.MAP, WriteMode.POLY_OBJ -> StreamingJsonInput(
                json,
                newMode,
                reader
            ) // need fresh cur index
            else -> if (mode == newMode) this else
                StreamingJsonInput(json, newMode, reader) // todo: reuse instance per mode
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
                        CompositeDecoder.READ_DONE
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
            CompositeDecoder.READ_DONE
        } else {
            ++currentIndex
        }
    }

    private fun decodeObjectIndex(tokenClass: Byte, desc: SerialDescriptor): Int {
        if (tokenClass == TC_COMMA && !reader.canBeginValue) {
            reader.fail("Unexpected trailing comma")
        }

        while (reader.canBeginValue) {
            ++currentIndex
            val key = decodeString()
            reader.requireTokenClass(TC_COLON) { "Expected ':'" }
            reader.nextToken()
            val index = desc.getElementIndex(key)
            if (index != CompositeDecoder.UNKNOWN_NAME) {
                return index
            }

            if (!configuration.ignoreUnknownKeys) {
                reader.fail("Encountered an unknown key $key. You can enable 'ignoreUnknownKeys' property" +
                        " to silently ignore such keys")
            }
            else reader.skipElement()
            if (reader.tokenClass == TC_COMMA) {
                reader.nextToken()
                reader.require(reader.canBeginValue, reader.currentPosition) { "Unexpected trailing comma" }
            }
        }
        return CompositeDecoder.READ_DONE
    }

    private fun decodeListIndex(tokenClass: Byte): Int {
        // Prohibit leading comma
        if (tokenClass != TC_COMMA && currentIndex != -1) {
            reader.requireTokenClass(TC_END_LIST) { "Expected end of the array or comma" }
        }
        return if (!reader.canBeginValue) {
            reader.require(tokenClass != TC_COMMA) { "Unexpected trailing comma" }
            CompositeDecoder.READ_DONE
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
            val string = reader.takeBooleanStringUnquoted()
            string.toBooleanStrict()
        }
    }

    /*
     * The rest of the primitives are allowed to be quoted and unqouted
     * to simplify integrations with third-party API.
     */
    override fun decodeByte(): Byte = reader.takeString().toByte()
    override fun decodeShort(): Short = reader.takeString().toShort()
    override fun decodeInt(): Int = reader.takeString().toInt()
    override fun decodeLong(): Long = reader.takeString().toLong()
    override fun decodeFloat(): Float = reader.takeString().toFloat()
    override fun decodeDouble(): Double = reader.takeString().toDouble()
    override fun decodeChar(): Char = reader.takeString().single()
    override fun decodeString(): String {
        return if (configuration.isLenient) {
            reader.takeString()
        } else {
            reader.takeStringQuoted()
        }
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return enumDescriptor.getElementIndexOrThrow(decodeString())
    }
}
