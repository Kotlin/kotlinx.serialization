/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.internal.EnumDescriptor
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerialModule

/**
 * [JsonInput] which reads given JSON from [JsonReader] field by field.
 */
internal class StreamingJsonInput internal constructor(
    public override val json: Json,
    private val mode: WriteMode,
    private val reader: JsonReader
) : JsonInput, ElementValueDecoder() {

    public override val context: SerialModule = json.context
    private var currentIndex = -1
    private var entryIndex = 0
    private val configuration = json.configuration
    
    public override fun decodeJson(): JsonElement = JsonParser(reader).read()

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

    override fun endStructure(desc: SerialDescriptor) {
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

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        val tokenClass = reader.tokenClass
        if (tokenClass == TC_COMMA) reader.nextToken()
        when (mode) {
            WriteMode.LIST -> {
                // Prohibit leading comma
                if (tokenClass == TC_COMMA) {
                    reader.require(currentIndex != -1, reader.currentPosition) { "Unexpected leading comma" }
                } else if (currentIndex != -1) {
                    // Prohibit leading comma
                    reader.requireTokenClass(TC_END_LIST) { "Expected end of the array or comma" }
                }

                return if (!reader.canBeginValue) {
                    reader.require(tokenClass != TC_COMMA) { "Unexpected trailing comma" }
                    CompositeDecoder.READ_DONE
                } else {
                    ++currentIndex
                }
            }
            WriteMode.MAP -> {
                if (currentIndex % 2 == 0 && reader.tokenClass == TC_COLON) reader.nextToken()
                return if (!reader.canBeginValue) CompositeDecoder.READ_DONE else ++currentIndex
            }
            WriteMode.POLY_OBJ -> {
                return when (entryIndex++) {
                    0 -> 0
                    1 -> 1
                    else -> {
                        entryIndex = 0
                        CompositeDecoder.READ_DONE
                    }
                }
            }
            else -> {
                while (reader.canBeginValue) {
                    val key = reader.takeString()
                    reader.requireTokenClass(TC_COLON) { "Expected ':'" }
                    reader.nextToken()
                    val index = desc.getElementIndex(key)
                    if (index != CompositeDecoder.UNKNOWN_NAME) {
                        return index
                    }
                    if (configuration.strictMode) throw jsonUnknownKeyException(reader.currentPosition, key)
                    else reader.skipElement()

                    if (reader.tokenClass == TC_COMMA) reader.nextToken()
                }
                return CompositeDecoder.READ_DONE
            }
        }
    }

    override fun decodeBoolean(): Boolean = reader.takeString().run { if (configuration.strictMode) toBooleanStrict() else toBoolean() }
    override fun decodeByte(): Byte = reader.takeString().toByte()
    override fun decodeShort(): Short = reader.takeString().toShort()
    override fun decodeInt(): Int = reader.takeString().toInt()
    override fun decodeLong(): Long = reader.takeString().toLong()
    override fun decodeFloat(): Float = reader.takeString().toFloat()
    override fun decodeDouble(): Double = reader.takeString().toDouble()
    override fun decodeChar(): Char = reader.takeString().single()
    override fun decodeString(): String = reader.takeString()
    override fun decodeEnum(enumDescription: EnumDescriptor): Int = enumDescription.getElementIndexOrThrow(reader.takeString())
}
