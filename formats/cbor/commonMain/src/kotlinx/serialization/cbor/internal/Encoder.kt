/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.cbor.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import kotlin.experimental.*

// Writes class as map [fieldName, fieldValue]
// Split implementation to optimize base case
@CborFriendModuleApi
public sealed class CborWriter(
    override val cbor: Cbor,
    protected val output: Output,
) : AbstractEncoder(), CborEncoder {
    protected var isClass: Boolean = false

    protected var encodeByteArrayAsByteString: Boolean = false

    protected abstract fun getDestination(): Output

    override val serializersModule: SerializersModule
        get() = cbor.serializersModule


    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {

        if ((encodeByteArrayAsByteString || cbor.configuration.alwaysUseByteString)
            && serializer.descriptor == ByteArraySerializer().descriptor
        ) {
            getDestination().encodeByteString(value as ByteArray)
        } else {
            encodeByteArrayAsByteString = encodeByteArrayAsByteString || serializer.descriptor.isInlineByteString()
            super<AbstractEncoder>.encodeSerializableValue(serializer, value)
        }
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        cbor.configuration.encodeDefaults

    protected abstract fun incrementChildren()

    override fun encodeString(value: String) {
        getDestination().encodeString(value)
    }


    override fun encodeFloat(value: Float) {
        getDestination().encodeFloat(value)
    }


    override fun encodeDouble(value: Double) {
        getDestination().encodeDouble(value)
    }


    override fun encodeChar(value: Char) {
        getDestination().encodeNumber(value.code.toLong())
    }


    override fun encodeByte(value: Byte) {
        getDestination().encodeNumber(value.toLong())
    }


    override fun encodeShort(value: Short) {
        getDestination().encodeNumber(value.toLong())
    }

    override fun encodeInt(value: Int) {
        getDestination().encodeNumber(value.toLong())
    }


    override fun encodeLong(value: Long) {
        getDestination().encodeNumber(value)
    }


    override fun encodeBoolean(value: Boolean) {
        getDestination().encodeBoolean(value)
    }


    override fun encodeNull() {
        if (isClass) getDestination().encodeEmptyMap()
        else getDestination().encodeNull()
    }

    @OptIn(ExperimentalSerializationApi::class) // KT-46731
    override fun encodeEnum(
        enumDescriptor: SerialDescriptor,
        index: Int
    ) {
        getDestination().encodeString(enumDescriptor.getElementName(index))
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        val destination = getDestination()
        isClass = descriptor.getElementDescriptor(index).kind == StructureKind.CLASS
        encodeByteArrayAsByteString = descriptor.isByteString(index)

        val name = descriptor.getElementName(index)


        if (!descriptor.hasArrayTag()) {
            if (cbor.configuration.encodeKeyTags) descriptor.getKeyTags(index)?.forEach { destination.encodeTag(it) }

            if ((descriptor.kind !is StructureKind.LIST) && (descriptor.kind !is StructureKind.MAP) && (descriptor.kind !is PolymorphicKind)) {
                //indices are put into the name field. we don't want to write those, as it would result in double writes
                val cborLabel = descriptor.getCborLabel(index)
                if (cbor.configuration.preferCborLabelsOverNames && cborLabel != null) {
                    destination.encodeNumber(cborLabel)
                } else {
                    destination.encodeString(name)
                }
            }
        }

        if (cbor.configuration.encodeValueTags) {
            descriptor.getValueTags(index)?.forEach { destination.encodeTag(it) }
        }
        incrementChildren() // needed for definite len encoding, NOOP for indefinite length encoding
        return true
    }
}


// optimized indefinite length encoder
@CborFriendModuleApi
public class IndefiniteLengthCborWriter(cbor: Cbor, output: Output) : CborWriter(
    cbor, output
) {

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (cbor.configuration.encodeObjectTags) descriptor.getObjectTags()?.forEach {
            output.encodeTag(it)
        }
        if (descriptor.hasArrayTag()) {
            output.startArray()
        } else {
            when (descriptor.kind) {
                StructureKind.LIST, is PolymorphicKind -> output.startArray()
                is StructureKind.MAP -> output.startMap()
                else -> output.startMap()
            }
        }
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        output.end()
    }

    override fun getDestination(): Output = output


    override fun incrementChildren() {/*NOOP*/
    }

}

//optimized definite length encoder
@CborFriendModuleApi
public class DefiniteLengthCborWriter(cbor: Cbor, output: Output) : CborWriter(cbor, output) {

    private class Data(val bytes: ByteArrayOutput, var elementCount: Int)

    private val structureStack = mutableListOf<Data>()
    override fun getDestination(): Output =
        structureStack.lastOrNull()?.bytes ?: output


    override fun incrementChildren() {
        structureStack.last().elementCount++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val current = Data(ByteArrayOutput(), 0)
        val _ = structureStack.add(current)
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        val completedCurrent = structureStack.removeLast()

        val accumulator = getDestination()

        val numChildren = completedCurrent.elementCount

        if (cbor.configuration.encodeObjectTags) descriptor.getObjectTags()?.forEach {
            accumulator.encodeTag(it)
        }

        if (descriptor.hasArrayTag()) {
            accumulator.startArray(numChildren.toULong())
        } else {
            when (descriptor.kind) {
                StructureKind.LIST, is PolymorphicKind -> accumulator.startArray(numChildren.toULong())
                is StructureKind.MAP -> accumulator.startMap((numChildren / 2).toULong())
                else -> accumulator.startMap((numChildren).toULong())
            }
        }
        completedCurrent.bytes.copyInto(accumulator)
    }
}


private fun Output.startArray() = write(BEGIN_ARRAY.toByte())

private fun Output.startArray(size: ULong) {
    composePositiveInline(size, HEADER_ARRAY)
}

private fun Output.startMap() = write(BEGIN_MAP.toByte())

private fun Output.startMap(size: ULong) {
    composePositiveInline(size, HEADER_MAP)
}

private fun Output.encodeTag(tag: ULong) {
    composePositiveInline(tag, HEADER_TAG)
}

internal fun Output.end() = write(BREAK.toByte())

internal fun Output.encodeNull() = write(NULL.toByte())

internal fun Output.encodeEmptyMap() = write(EMPTY_MAP.toByte())

internal fun Output.writeByte(byteValue: Int) = write(byteValue.toByte())

internal fun Output.encodeBoolean(value: Boolean) = write(if (value) TRUE.toByte() else FALSE.toByte())

internal fun Output.encodeNumber(value: Long) = write(composeNumber(value))

internal fun Output.encodeByteString(data: ByteArray) {
    this.encodeByteArray(data, HEADER_BYTE_STRING)
}

internal fun Output.encodeString(value: String) {
    this.encodeByteArray(value.encodeToByteArray(), HEADER_STRING)
}

internal fun Output.encodeByteArray(data: ByteArray, type: Int) {
    composePositiveInline(data.size.toULong(), type)
    write(data)
}

internal fun Output.encodeFloat(value: Float) {
    write(NEXT_FLOAT.toByte())
    val bits = value.toRawBits()
    for (i in 0..3) {
        write(((bits shr (24 - 8 * i)) and 0xFF).toByte())
    }
}

internal fun Output.encodeDouble(value: Double) {
    write(NEXT_DOUBLE.toByte())
    val bits = value.toRawBits()
    for (i in 0..7) {
        write(((bits shr (56 - 8 * i)) and 0xFF).toByte())
    }
}

//don't know why, but if the negative branch is also optimized and everything operates directly on the Output it gets slower
private fun composeNumber(value: Long): ByteArray =
    if (value >= 0) composePositive(value.toULong()) else composeNegative(value)

private fun Output.composePositiveInline(value: ULong, mod: Int) = when (value) {
    in 0u..23u -> writeByte(value.toInt() or mod)
    in 24u..UByte.MAX_VALUE.toUInt() -> {
        writeByte(24 or mod)
        writeByte(value.toInt())
    }

    in (UByte.MAX_VALUE.toUInt() + 1u)..UShort.MAX_VALUE.toUInt() -> encodeToInline(value, 2, 25 or mod)
    in (UShort.MAX_VALUE.toUInt() + 1u)..UInt.MAX_VALUE -> encodeToInline(value, 4, 26 or mod)
    else -> encodeToInline(value, 8, 27 or mod)
}


private fun composePositive(value: ULong): ByteArray = when (value) {
    in 0u..23u -> byteArrayOf(value.toByte())
    in 24u..UByte.MAX_VALUE.toUInt() -> byteArrayOf(24, value.toByte())
    in (UByte.MAX_VALUE.toUInt() + 1u)..UShort.MAX_VALUE.toUInt() -> encodeToByteArray(value, 2, 25)
    in (UShort.MAX_VALUE.toUInt() + 1u)..UInt.MAX_VALUE -> encodeToByteArray(value, 4, 26)
    else -> encodeToByteArray(value, 8, 27)
}


private fun Output.encodeToInline(value: ULong, bytes: Int, tag: Int) {
    val limit = bytes * 8 - 8
    writeByte(tag)
    for (i in 0 until bytes) {
        writeByte(((value shr (limit - 8 * i)) and 0xFFu).toInt())
    }
}

private fun encodeToByteArray(value: ULong, bytes: Int, tag: Byte): ByteArray {
    val result = ByteArray(bytes + 1)
    val limit = bytes * 8 - 8
    result[0] = tag
    for (i in 0 until bytes) {
        result[i + 1] = ((value shr (limit - 8 * i)) and 0xFFu).toByte()
    }
    return result
}

private fun composeNegative(value: Long): ByteArray {
    val aVal = if (value == Long.MIN_VALUE) Long.MAX_VALUE else -1 - value
    val data = composePositive(aVal.toULong())
    data[0] = data[0] or HEADER_NEGATIVE
    return data
}
