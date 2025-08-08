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


//value classes are only inlined on the JVM, so we use a typealias and extensions instead
private typealias Stack = MutableList<CborWriter.Data>

private fun Stack(initial: CborWriter.Data): Stack = mutableListOf(initial)
private fun Stack.push(value: CborWriter.Data) = add(value)
private fun Stack.pop() = removeLast()
private fun Stack.peek() = last()

// Writes class as map [fieldName, fieldValue]
// Split implementation to optimize base case
internal sealed class CborWriter(
    override val cbor: Cbor,
) : AbstractEncoder(), CborEncoder {

    internal open fun encodeByteString(byteArray: ByteArray) {
        getDestination().encodeByteString(byteArray)
    }

    protected var isClass = false

    protected var encodeByteArrayAsByteString = false

    class Data(val bytes: ByteArrayOutput, var elementCount: Int)

    protected abstract fun getDestination(): ByteArrayOutput

    override val serializersModule: SerializersModule
        get() = cbor.serializersModule


    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {

        if ((encodeByteArrayAsByteString || cbor.configuration.alwaysUseByteString)
            && serializer.descriptor == ByteArraySerializer().descriptor
        ) {
            encodeByteString(value as ByteArray)
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

    internal abstract fun encodeTags(tags: ULongArray)
}


// optimized indefinite length encoder
internal class IndefiniteLengthCborWriter(cbor: Cbor, private val output: ByteArrayOutput) : CborWriter(
    cbor
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

    override fun getDestination(): ByteArrayOutput = output


    override fun incrementChildren() {/*NOOP*/
    }

    override fun encodeTags(tags: ULongArray) = tags.forEach { getDestination().encodeTag(it) }

}

// optimized indefinite length encoder
internal class StructuredCborWriter(cbor: Cbor) : CborWriter(cbor) {

    /**
     * Tags and values are "written", i.e. recorded/prepared for encoding separately. Hence, we need a helper that allows
     * for setting tags and values independently, and then merging them into the final [CborElement] at the end.
     */
    internal sealed class CborContainer(tags: ULongArray, elements: MutableList<CborElement>) {
        protected val elements = elements

        var tags = tags
            internal set


        open fun add(element: CborElement) = elements.add(element)
        class Map(tags: ULongArray, elements: MutableList<CborElement> = mutableListOf()) :
            CborContainer(tags, elements)

        class List(tags: ULongArray, elements: MutableList<CborElement> = mutableListOf()) :
            CborContainer(tags, elements)

        class Primitive(tags: ULongArray) : CborContainer(tags, elements = mutableListOf()) {
            override fun add(element: CborElement): Boolean {
                require(elements.isEmpty()) { "Implementation error. Please report a bug." }
                return elements.add(element)
            }
        }

        fun finalize() = when (this) {
            is List -> CborList(content = elements, tags = tags)
            is Map -> CborMap(
                content = if (elements.isNotEmpty()) IntRange(0, elements.size / 2 - 1).associate {
                    elements[it * 2] to elements[it * 2 + 1]
                } else mapOf(),
                tags = tags
            )

            is Primitive -> elements.first().also { it.tags += tags }

        }
    }

    private operator fun CborContainer?.plusAssign(element: CborElement) {
        this!!.add(element)
    }


    private val stack = ArrayDeque<CborContainer>()
    private var currentElement: CborContainer? = null

    // value tags are collects inside beginStructure, so we need to cache them here and write them in beginStructure or encodeXXX
    // and then null them out, so there are no leftovers
    private var nextValueTags: ULongArray = ulongArrayOf()
        get() {
            val ret = field
            field = ulongArrayOf()
            return ret
        }

    fun finalize() = currentElement!!.finalize()

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val tags = nextValueTags +
            if (cbor.configuration.encodeObjectTags) descriptor.getObjectTags() ?: ulongArrayOf()
            else ulongArrayOf()
        val element = if (descriptor.hasArrayTag()) {
            CborContainer.List(tags)
        } else {
            when (descriptor.kind) {
                StructureKind.LIST, is PolymorphicKind -> CborContainer.List(tags)
                is StructureKind.MAP -> CborContainer.Map(tags)
                else -> CborContainer.Map(tags)
            }
        }
        currentElement?.let { stack.add(it) }
        currentElement = element
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        val finalized = currentElement!!.finalize()
        if (stack.isNotEmpty()) {
            currentElement = stack.removeLast()
            currentElement += finalized
        }
    }

    override fun getDestination() = throw IllegalStateException("There is no byteArrayInput")

    override fun incrementChildren() {
        /*NOOP*/
    }


    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        // this mirrors the special encoding of nullable classes that are null into am empty map.
        // THIS IS NOT CBOR-COMPLiANT
        // but keeps backwards compatibility with the way kotlinx.serialization CBOR format has always worked.
        isClass = descriptor.getElementDescriptor(index).kind == StructureKind.CLASS

        encodeByteArrayAsByteString = descriptor.isByteString(index)
        //TODO check if cborelement and be done
        val name = descriptor.getElementName(index)
        if (!descriptor.hasArrayTag()) {
            val keyTags = if (cbor.configuration.encodeKeyTags) descriptor.getKeyTags(index) else null

            if ((descriptor.kind !is StructureKind.LIST) && (descriptor.kind !is StructureKind.MAP) && (descriptor.kind !is PolymorphicKind)) {
                //indices are put into the name field. we don't want to write those, as it would result in double writes
                val cborLabel = descriptor.getCborLabel(index)
                if (cbor.configuration.preferCborLabelsOverNames && cborLabel != null) {
                    currentElement += CborInt(value = cborLabel, tags = keyTags ?: ulongArrayOf())
                } else {
                    currentElement += CborString(name, tags = keyTags ?: ulongArrayOf())
                }
            }
        }

        if (cbor.configuration.encodeValueTags) {
            descriptor.getValueTags(index).let { valueTags ->
                //collect them for late encoding in beginStructure or encodeXXX
                nextValueTags = valueTags ?: ulongArrayOf()
            }
        }
        return true
    }


    override fun encodeTags(tags: ULongArray) {
        nextValueTags += tags
    }

    override fun encodeBoolean(value: Boolean) {
        currentElement += CborBoolean(value, tags = nextValueTags)
    }

    override fun encodeByte(value: Byte) {
        currentElement += CborInt(value.toLong(), tags = nextValueTags)
    }

    override fun encodeChar(value: Char) {
        currentElement += CborInt(value.code.toLong(), tags = nextValueTags)
    }

    override fun encodeDouble(value: Double) {
        currentElement += CborFloat(value, tags = nextValueTags)
    }

    override fun encodeFloat(value: Float) {
        currentElement += CborFloat(value.toDouble(), tags = nextValueTags)
    }

    override fun encodeInt(value: Int) {
        currentElement += CborInt(value.toLong(), tags = nextValueTags)
    }

    override fun encodeLong(value: Long) {
        currentElement += CborInt(value, tags = nextValueTags)
    }

    override fun encodeShort(value: Short) {
        currentElement += CborInt(value.toLong(), tags = nextValueTags)
    }

    override fun encodeString(value: String) {
        currentElement += CborString(value, tags = nextValueTags)
    }

    override fun encodeByteString(byteArray: ByteArray) {
        currentElement += CborByteString(byteArray, tags = nextValueTags)
    }

    override fun encodeNull() {
        /*NOT CBOR-COMPLIANT, KxS-proprietary behaviour*/
        currentElement += if (isClass) CborMap(
            mapOf(),
            tags = nextValueTags
        )
        else CborNull(tags = nextValueTags)
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        currentElement += CborString(enumDescriptor.getElementName(index), tags = nextValueTags)
    }

}

//optimized definite length encoder
internal class DefiniteLengthCborWriter(cbor: Cbor, output: ByteArrayOutput) : CborWriter(cbor) {

    private val structureStack = Stack(Data(output, -1))
    override fun getDestination(): ByteArrayOutput =
        structureStack.peek().bytes


    override fun incrementChildren() {
        structureStack.peek().elementCount++
    }

    override fun encodeTags(tags: ULongArray) = tags.forEach { getDestination().encodeTag(it) }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val current = Data(ByteArrayOutput(), 0)
        val _ = structureStack.push(current)
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        val completedCurrent = structureStack.pop()

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
        accumulator.copyFrom(completedCurrent.bytes)
    }
}


private fun ByteArrayOutput.startArray() = write(BEGIN_ARRAY)

private fun ByteArrayOutput.startArray(size: ULong) {
    composePositiveInline(size, HEADER_ARRAY)
}

private fun ByteArrayOutput.startMap() = write(BEGIN_MAP)

private fun ByteArrayOutput.startMap(size: ULong) {
    composePositiveInline(size, HEADER_MAP)
}

internal fun ByteArrayOutput.encodeTag(tag: ULong) {
    composePositiveInline(tag, HEADER_TAG)
}

internal fun ByteArrayOutput.end() = write(BREAK)

internal fun ByteArrayOutput.encodeNull() = write(NULL)

internal fun ByteArrayOutput.encodeEmptyMap() = write(EMPTY_MAP)

internal fun ByteArrayOutput.writeByte(byteValue: Int) = write(byteValue)

internal fun ByteArrayOutput.encodeBoolean(value: Boolean) = write(if (value) TRUE else FALSE)

internal fun ByteArrayOutput.encodeNumber(value: Long) = write(composeNumber(value))

internal fun ByteArrayOutput.encodeByteString(data: ByteArray) {
    this.encodeByteArray(data, HEADER_BYTE_STRING)
}

internal fun ByteArrayOutput.encodeString(value: String) {
    this.encodeByteArray(value.encodeToByteArray(), HEADER_STRING)
}

internal fun ByteArrayOutput.encodeByteArray(data: ByteArray, type: Int) {
    composePositiveInline(data.size.toULong(), type)
    write(data)
}

internal fun ByteArrayOutput.encodeFloat(value: Float) {
    write(NEXT_FLOAT)
    val bits = value.toRawBits()
    for (i in 0..3) {
        write((bits shr (24 - 8 * i)) and 0xFF)
    }
}

internal fun ByteArrayOutput.encodeDouble(value: Double) {
    write(NEXT_DOUBLE)
    val bits = value.toRawBits()
    for (i in 0..7) {
        write(((bits shr (56 - 8 * i)) and 0xFF).toInt())
    }
}

//don't know why, but if the negative branch is also optimized and everything operates directly on the ByteArrayOutput it gets slower
private fun composeNumber(value: Long): ByteArray =
    if (value >= 0) composePositive(value.toULong()) else composeNegative(value)

private fun ByteArrayOutput.composePositiveInline(value: ULong, mod: Int) = when (value) {
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


private fun ByteArrayOutput.encodeToInline(value: ULong, bytes: Int, tag: Int) {
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
