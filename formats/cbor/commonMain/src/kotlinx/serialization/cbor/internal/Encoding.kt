/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.cbor.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.cbor.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import kotlin.experimental.*

private const val FALSE = 0xf4
private const val TRUE = 0xf5
private const val NULL = 0xf6

private const val NEXT_FLOAT = 0xfa
private const val NEXT_DOUBLE = 0xfb

private const val BEGIN_ARRAY = 0x9f
private const val BEGIN_MAP = 0xbf
private const val BREAK = 0xff

private const val ADDITIONAL_INFORMATION_INDEFINITE_LENGTH = 0x1f

private const val HEADER_BYTE_STRING: Byte = 0b010_00000
private const val HEADER_STRING: Byte = 0b011_00000
private const val HEADER_NEGATIVE: Byte = 0b001_00000
private const val HEADER_ARRAY: Int = 0b100_00000
private const val HEADER_MAP: Int = 0b101_00000

// Differs from List only in start byte
private class CborMapWriter(cbor: Cbor, encoder: CborEncoder) : CborListWriter(cbor, encoder) {
    override fun writeBeginToken() = encoder.startMap()
}

// Writes all elements consequently, except size - CBOR supports maps and arrays of indefinite length
private open class CborListWriter(cbor: Cbor, encoder: CborEncoder) : CborWriter(cbor, encoder) {
    override fun writeBeginToken() = encoder.startArray()

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean = true
}

// Writes class as map [fieldName, fieldValue]
internal open class CborWriter(private val cbor: Cbor, protected val encoder: CborEncoder) : AbstractEncoder() {
    override val serializersModule: SerializersModule
        get() = cbor.serializersModule

    private var encodeByteArrayAsByteString = false

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if (encodeByteArrayAsByteString && serializer.descriptor == ByteArraySerializer().descriptor) {
            encoder.encodeByteString(value as ByteArray)
        } else {
            super.encodeSerializableValue(serializer, value)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = cbor.encodeDefaults

    protected open fun writeBeginToken() = encoder.startMap()

    //todo: Write size of map or array if known
    @OptIn(ExperimentalSerializationApi::class)
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val writer = when (descriptor.kind) {
            StructureKind.LIST, is PolymorphicKind -> CborListWriter(cbor, encoder)
            StructureKind.MAP -> CborMapWriter(cbor, encoder)
            else -> CborWriter(cbor, encoder)
        }
        writer.writeBeginToken()
        return writer
    }

    override fun endStructure(descriptor: SerialDescriptor) = encoder.end()

    @OptIn(ExperimentalSerializationApi::class)
    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        encodeByteArrayAsByteString = descriptor.isByteString(index)
        val name = descriptor.getElementName(index)
        encoder.encodeString(name)
        return true
    }

    override fun encodeString(value: String) = encoder.encodeString(value)

    override fun encodeFloat(value: Float) = encoder.encodeFloat(value)
    override fun encodeDouble(value: Double) = encoder.encodeDouble(value)

    override fun encodeChar(value: Char) = encoder.encodeNumber(value.toLong())
    override fun encodeByte(value: Byte) = encoder.encodeNumber(value.toLong())
    override fun encodeShort(value: Short) = encoder.encodeNumber(value.toLong())
    override fun encodeInt(value: Int) = encoder.encodeNumber(value.toLong())
    override fun encodeLong(value: Long) = encoder.encodeNumber(value)

    override fun encodeBoolean(value: Boolean) = encoder.encodeBoolean(value)

    override fun encodeNull() = encoder.encodeNull()

    override fun encodeEnum(
        enumDescriptor: SerialDescriptor,
        index: Int
    ) =
        encoder.encodeString(enumDescriptor.getElementName(index))
}

// For details of representation, see https://tools.ietf.org/html/rfc7049#section-2.1
internal class CborEncoder(private val output: ByteArrayOutput) {

    fun startArray() = output.write(BEGIN_ARRAY)
    fun startMap() = output.write(BEGIN_MAP)
    fun end() = output.write(BREAK)

    fun encodeNull() = output.write(NULL)

    fun encodeBoolean(value: Boolean) = output.write(if (value) TRUE else FALSE)

    fun encodeNumber(value: Long) = output.write(composeNumber(value))

    fun encodeByteString(data: ByteArray) {
        encodeByteArray(data, HEADER_BYTE_STRING)
    }

    fun encodeString(value: String) {
        encodeByteArray(value.encodeToByteArray(), HEADER_STRING)
    }

    private fun encodeByteArray(data: ByteArray, type: Byte) {
        val header = composeNumber(data.size.toLong())
        header[0] = header[0] or type
        output.write(header)
        output.write(data)
    }

    fun encodeFloat(value: Float) {
        output.write(NEXT_FLOAT)
        val bits = value.toRawBits()
        for (i in 0..3) {
            output.write((bits shr (24 - 8 * i)) and 0xFF)
        }
    }

    fun encodeDouble(value: Double) {
        output.write(NEXT_DOUBLE)
        val bits = value.toRawBits()
        for (i in 0..7) {
            output.write(((bits shr (56 - 8 * i)) and 0xFF).toInt())
        }
    }

    private fun composeNumber(value: Long): ByteArray =
        if (value >= 0) composePositive(value) else composeNegative(value)

    private fun composePositive(value: Long): ByteArray = when (value) {
        in 0..23 -> byteArrayOf(value.toByte())
        in 24..Byte.MAX_VALUE -> byteArrayOf(24, value.toByte())
        in Byte.MAX_VALUE + 1..Short.MAX_VALUE -> encodeToByteArray(value, 2, 25)
        in Short.MAX_VALUE + 1..Int.MAX_VALUE -> encodeToByteArray(value, 4, 26)
        in (Int.MAX_VALUE.toLong() + 1..Long.MAX_VALUE) -> encodeToByteArray(value, 8, 27)
        else -> throw AssertionError("$value should be positive")
    }

    private fun encodeToByteArray(value: Long, bytes: Int, tag: Byte): ByteArray {
        val result = ByteArray(bytes + 1)
        val limit = bytes * 8 - 8
        result[0] = tag
        for (i in 0 until bytes) {
            result[i + 1] = ((value shr (limit - 8 * i)) and 0xFF).toByte()
        }
        return result
    }

    private fun composeNegative(value: Long): ByteArray {
        val aVal = if (value == Long.MIN_VALUE) Long.MAX_VALUE else -1 - value
        val data = composePositive(aVal)
        data[0] = data[0] or HEADER_NEGATIVE
        return data
    }
}

private class CborMapReader(cbor: Cbor, decoder: CborDecoder) : CborListReader(cbor, decoder) {
    override fun skipBeginToken() = setSize(decoder.startMap() * 2)
}

private open class CborListReader(cbor: Cbor, decoder: CborDecoder) : CborReader(cbor, decoder) {
    private var ind = 0

    override fun skipBeginToken() = setSize(decoder.startArray())

    override fun decodeElementIndex(descriptor: SerialDescriptor) = if (!finiteMode && decoder.isEnd() || (finiteMode && ind >= size)) CompositeDecoder.DECODE_DONE else ind++
}

internal open class CborReader(private val cbor: Cbor, protected val decoder: CborDecoder) : AbstractDecoder() {

    protected var size = -1
        private set
    protected var finiteMode = false
        private set
    private var readProperties: Int = 0

    private var decodeByteArrayAsByteString = false

    protected fun setSize(size: Int) {
        if (size >= 0) {
            finiteMode = true
            this.size = size
        }
    }

    override val serializersModule: SerializersModule
        get() = cbor.serializersModule

    protected open fun skipBeginToken() = setSize(decoder.startMap())

    @OptIn(ExperimentalSerializationApi::class)
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val re = when (descriptor.kind) {
            StructureKind.LIST, is PolymorphicKind -> CborListReader(cbor, decoder)
            StructureKind.MAP -> CborMapReader(cbor, decoder)
            else -> CborReader(cbor, decoder)
        }
        re.skipBeginToken()
        return re
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (!finiteMode) decoder.end()
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (!finiteMode && decoder.isEnd() || (finiteMode && readProperties >= size)) return CompositeDecoder.DECODE_DONE
        val elemName = decoder.nextString()
        readProperties++
        val index = descriptor.getElementIndexOrThrow(elemName)
        decodeByteArrayAsByteString = descriptor.isByteString(index)
        return index
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return if (decodeByteArrayAsByteString && deserializer.descriptor == ByteArraySerializer().descriptor) {
            @Suppress("UNCHECKED_CAST")
            decoder.nextByteString() as T
        } else {
            super.decodeSerializableValue(deserializer)
        }
    }

    override fun decodeString() = decoder.nextString()

    override fun decodeNotNullMark(): Boolean = !decoder.isNull()

    override fun decodeDouble() = decoder.nextDouble()
    override fun decodeFloat() = decoder.nextFloat()

    override fun decodeBoolean() = decoder.nextBoolean()

    override fun decodeByte() = decoder.nextNumber().toByte()
    override fun decodeShort() = decoder.nextNumber().toShort()
    override fun decodeChar() = decoder.nextNumber().toChar()
    override fun decodeInt() = decoder.nextNumber().toInt()
    override fun decodeLong() = decoder.nextNumber()

    override fun decodeNull() = decoder.nextNull()

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int =
        enumDescriptor.getElementIndexOrThrow(decoder.nextString())

}

internal class CborDecoder(private val input: ByteArrayInput) {
    private var curByte: Int = -1

    init {
        readByte()
    }

    private fun readByte(): Int {
        curByte = input.read()
        return curByte
    }

    private fun skipByte(expected: Int) {
        if (curByte != expected) throw CborDecodingException("byte ${printByte(expected)}", curByte)
        readByte()
    }

    fun isNull() = curByte == NULL

    fun nextNull(): Nothing? {
        skipByte(NULL)
        return null
    }

    fun nextBoolean(): Boolean {
        val ans = when (curByte) {
            TRUE -> true
            FALSE -> false
            else -> throw CborDecodingException("boolean value", curByte)
        }
        readByte()
        return ans
    }

    fun startArray() = startSized(BEGIN_ARRAY, HEADER_ARRAY, "array")

    fun startMap() = startSized(BEGIN_MAP, HEADER_MAP, "map")

    private fun startSized(unboundedHeader: Int, boundedHeaderMask: Int, collectionType: String): Int {
        if (curByte == unboundedHeader) {
            skipByte(unboundedHeader)
            return -1
        }
        if ((curByte and 0b111_00000) != boundedHeaderMask)
            throw CborDecodingException("start of $collectionType", curByte)
        val size = readNumber().toInt()
        readByte()
        return size
    }

    fun isEnd() = curByte == BREAK

    fun end() = skipByte(BREAK)

    fun nextByteString(): ByteArray {
        if ((curByte and 0b111_00000) != HEADER_BYTE_STRING.toInt())
            throw CborDecodingException("start of byte string", curByte)
        val arr = readBytes()
        readByte()
        return arr
    }

    fun nextString(): String {
        if ((curByte and 0b111_00000) != HEADER_STRING.toInt())
            throw CborDecodingException("start of string", curByte)
        val arr = readBytes()
        val ans = arr.decodeToString()
        readByte()
        return ans
    }

    private fun readBytes(): ByteArray =
        if (curByte and 0b000_11111 == ADDITIONAL_INFORMATION_INDEFINITE_LENGTH) {
            readByte()
            readIndefiniteLengthBytes()
        } else {
            val strLen = readNumber().toInt()
            input.readExactNBytes(strLen)
        }

    fun nextNumber(): Long {
        val res = readNumber()
        readByte()
        return res
    }

    private fun readNumber(): Long {
        val value = curByte and 0b000_11111
        val negative = (curByte and 0b111_00000) == HEADER_NEGATIVE.toInt()
        val bytesToRead = when (value) {
            24 -> 1
            25 -> 2
            26 -> 4
            27 -> 8
            else -> 0
        }
        if (bytesToRead == 0) {
            return if (negative) -(value + 1).toLong()
            else value.toLong()
        }
        val res = input.readExact(bytesToRead)
        return if (negative) -(res + 1)
        else res
    }

    private fun ByteArrayInput.readExact(bytes: Int): Long {
        val arr = readExactNBytes(bytes)
        var result = 0L
        for (i in 0 until bytes) {
            result = (result shl 8) or (arr[i].toInt() and 0xFF).toLong()
        }
        return result
    }

    private fun ByteArrayInput.readExactNBytes(bytesCount: Int): ByteArray {
        if (bytesCount > availableBytes) {
            error("Unexpected EOF, available $availableBytes bytes, requested: $bytesCount")
        }
        val array = ByteArray(bytesCount)
        read(array, 0, bytesCount)
        return array
    }

    fun nextFloat(): Float {
        if (curByte != NEXT_FLOAT) throw CborDecodingException("float header", curByte)
        val res = Float.fromBits(readInt())
        readByte()
        return res
    }

    fun nextDouble(): Double {
        if (curByte != NEXT_DOUBLE) throw CborDecodingException("double header", curByte)
        val res = Double.fromBits(readLong())
        readByte()
        return res
    }

    private fun readLong(): Long {
        var result = 0L
        for (i in 0..7) {
            val byte = input.read()
            result = (result shl 8) or byte.toLong()
        }
        return result
    }

    private fun readInt(): Int {
        var result = 0
        for (i in 0..3) {
            val byte = input.read()
            result = (result shl 8) or byte
        }
        return result
    }

    /**
     * Indefinite-length byte sequences contain an unknown number of fixed-length byte sequences (chunks).
     *
     * @return [ByteArray] containing all of the concatenated bytes found in the buffer.
     */
    private fun readIndefiniteLengthBytes(): ByteArray {
        val byteStrings = mutableListOf<ByteArray>()
        do {
            byteStrings.add(readBytes())
            readByte()
        } while (!isEnd())
        return byteStrings.flatten()
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getElementIndexOrThrow(name: String): Int {
    val index = getElementIndex(name)
    if (index == CompositeDecoder.UNKNOWN_NAME)
        throw SerializationException("$serialName does not contain element with name '$name'")
    return index
}

private fun Iterable<ByteArray>.flatten(): ByteArray {
    val output = ByteArray(sumBy { it.size })

    var position = 0
    for (chunk in this) {
        chunk.copyInto(output, position)
        position += chunk.size
    }

    return output
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.isByteString(index: Int): Boolean =
    getElementDescriptor(index) == ByteArraySerializer().descriptor &&
        getElementAnnotations(index).find { it is ByteString } != null
