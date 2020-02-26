/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerialModule
import kotlin.jvm.JvmField


internal class StreamingJsonOutput(private val composer: Composer, override val json: Json, private val mode: WriteMode,
                                   private val modeReuseCache: Array<JsonOutput?>) : JsonOutput, AbstractEncoder() {

    internal constructor(
        output: StringBuilder, json: Json, mode: WriteMode,
        modeReuseCache: Array<JsonOutput?>
    ) : this(Composer(output, json), json, mode, modeReuseCache)

    public override val context: SerialModule = json.context
    private val configuration = json.configuration

    // Forces serializer to wrap all values into quotes
    private var forceQuoting: Boolean = false
    private var writePolymorphic = false

    init {
        val i = mode.ordinal
        if (modeReuseCache[i] !== null || modeReuseCache[i] !== this)
            modeReuseCache[i] = this
    }

    override fun encodeJson(element: JsonElement) {
        encodeSerializableValue(JsonElementSerializer, element)
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean {
        return configuration.encodeDefaults
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        encodePolymorphically(serializer, value) {
            writePolymorphic = true
        }
    }

    private fun encodeTypeInfo(descriptor: SerialDescriptor) {
        composer.nextItem()
        encodeString(configuration.classDiscriminator)
        composer.print(COLON)
        composer.space()
        encodeString(descriptor.serialName)
    }

    override fun beginStructure(descriptor: SerialDescriptor, vararg typeSerializers: KSerializer<*>): CompositeEncoder {
        val newMode = json.switchMode(descriptor)
        if (newMode.begin != INVALID) { // entry
            composer.print(newMode.begin)
            composer.indent()
        }

        if (writePolymorphic) {
            writePolymorphic = false
            encodeTypeInfo(descriptor)
        }

        if (mode == newMode) {
            return this
        }

        return modeReuseCache[newMode.ordinal] ?: StreamingJsonOutput(composer, json, newMode, modeReuseCache)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (mode.end != INVALID) {
            composer.unIndent()
            composer.nextItem()
            composer.print(mode.end)
        }
    }

    override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
        when (mode) {
            WriteMode.LIST -> {
                if (!composer.writingFirst)
                    composer.print(COMMA)
                composer.nextItem()
            }
            WriteMode.MAP -> {
                if (!composer.writingFirst) {
                    forceQuoting = if (index % 2 == 0) {
                        composer.print(COMMA)
                        composer.nextItem() // indent should only be put after commas in map
                        true
                    } else {
                        composer.print(COLON)
                        composer.space()
                        false
                    }
                } else {
                    forceQuoting = true
                    composer.nextItem()
                }
            }
            WriteMode.POLY_OBJ -> {
                if (index == 0)
                    forceQuoting = true
                if (index == 1) {
                    composer.print(COMMA)
                    composer.space()
                    forceQuoting = false
                }
            }
            else -> {
                if (!composer.writingFirst)
                    composer.print(COMMA)
                composer.nextItem()
                encodeString(desc.getElementName(index))
                composer.print(COLON)
                composer.space()
            }
        }
        return true
    }

    override fun encodeNull() {
        composer.print(NULL)
    }

    override fun encodeBoolean(value: Boolean) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeByte(value: Byte) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeShort(value: Short) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeInt(value: Int) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeLong(value: Long) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeFloat(value: Float) {
        // First encode value, then check, to have a prettier error message
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
        if (!configuration.serializeSpecialFloatingPointValues && !value.isFinite()) {
            throw InvalidFloatingPoint(value, "float", composer.sb.toString())
        }
    }

    override fun encodeDouble(value: Double) {
        // First encode value, then check, to have a prettier error message
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
        if (!configuration.serializeSpecialFloatingPointValues && !value.isFinite()) {
            throw InvalidFloatingPoint(value, "double", composer.sb.toString())
        }
    }

    override fun encodeChar(value: Char) {
        encodeString(value.toString())
    }

    override fun encodeString(value: String) {
        if (configuration.unquotedPrint && !shouldBeQuoted(value)) {
            composer.print(value)
        } else {
            composer.printQuoted(value)
        }
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeString(enumDescriptor.getElementName(index))
    }

    internal class Composer(@JvmField internal val sb: StringBuilder, private val json: Json) {
        private var level = 0
        var writingFirst = true
            private set

        fun indent() {
            writingFirst = true; level++
        }

        fun unIndent() {
            level--
        }

        fun nextItem() {
            writingFirst = false
            if (json.configuration.prettyPrint) {
                print("\n")
                repeat(level) { print(json.configuration.indent) }
            }
        }

        fun space() {
            if (json.configuration.prettyPrint)
                print(' ')
        }

        fun print(v: Char) = sb.append(v)
        fun print(v: String) = sb.append(v)
        fun print(v: Float) = sb.append(v)
        fun print(v: Double) = sb.append(v)
        fun print(v: Byte) = sb.append(v)
        fun print(v: Short) = sb.append(v)
        fun print(v: Int) = sb.append(v)
        fun print(v: Long) = sb.append(v)
        fun print(v: Boolean) = sb.append(v)
        fun printQuoted(value: String): Unit = sb.printQuoted(value)
    }
}
