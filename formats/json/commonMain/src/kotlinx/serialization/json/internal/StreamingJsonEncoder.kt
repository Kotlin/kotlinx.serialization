/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.native.concurrent.*

@ExperimentalSerializationApi
@OptIn(ExperimentalUnsignedTypes::class)
@SharedImmutable
private val unsignedNumberDescriptors = setOf(
    UInt.serializer().descriptor,
    ULong.serializer().descriptor,
    UByte.serializer().descriptor,
    UShort.serializer().descriptor
)

@ExperimentalSerializationApi
internal val SerialDescriptor.isUnsignedNumber: Boolean
    get() = this.isInline && this in unsignedNumberDescriptors

@OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)
internal class StreamingJsonEncoder(
    private val composer: Composer,
    override val json: Json,
    private val mode: WriteMode,
    private val modeReuseCache: Array<JsonEncoder?>?
) : JsonEncoder, AbstractEncoder() {

    internal constructor(
        output: JsonStringBuilder, json: Json, mode: WriteMode,
        modeReuseCache: Array<JsonEncoder?>
    ) : this(Composer(output, json), json, mode, modeReuseCache)

    override val serializersModule: SerializersModule = json.serializersModule
    private val configuration = json.configuration

    // Forces serializer to wrap all values into quotes
    private var forceQuoting: Boolean = false
    private var writePolymorphic = false

    init {
        val i = mode.ordinal
        if (modeReuseCache != null) {
            if (modeReuseCache[i] !== null || modeReuseCache[i] !== this)
                modeReuseCache[i] = this
        }
    }

    override fun encodeJsonElement(element: JsonElement) {
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

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
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

        return modeReuseCache?.get(newMode.ordinal) ?: StreamingJsonEncoder(composer, json, newMode, modeReuseCache)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (mode.end != INVALID) {
            composer.unIndent()
            composer.nextItem()
            composer.print(mode.end)
        }
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
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
                encodeString(descriptor.getElementName(index))
                composer.print(COLON)
                composer.space()
            }
        }
        return true
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (value != null || configuration.explicitNulls) {
            super.encodeNullableSerializableElement(descriptor, index, serializer, value)
        }
    }

    override fun encodeInline(inlineDescriptor: SerialDescriptor): Encoder =
        if (inlineDescriptor.isUnsignedNumber) StreamingJsonEncoder(
            ComposerForUnsignedNumbers(
                composer.sb,
                json
            ), json, mode, null
        )
        else super.encodeInline(inlineDescriptor)

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
        if (!configuration.allowSpecialFloatingPointValues && !value.isFinite()) {
            throw InvalidFloatingPointEncoded(value, composer.sb.toString())
        }
    }

    override fun encodeDouble(value: Double) {
        // First encode value, then check, to have a prettier error message
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
        if (!configuration.allowSpecialFloatingPointValues && !value.isFinite()) {
            throw InvalidFloatingPointEncoded(value, composer.sb.toString())
        }
    }

    override fun encodeChar(value: Char) {
        encodeString(value.toString())
    }

    override fun encodeString(value: String) = composer.printQuoted(value)

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeString(enumDescriptor.getElementName(index))
    }
}
