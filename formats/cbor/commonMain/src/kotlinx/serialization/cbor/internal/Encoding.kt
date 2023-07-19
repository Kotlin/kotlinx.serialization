/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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

private const val FALSE = 0xf4
private const val TRUE = 0xf5
private const val NULL = 0xf6

private const val NEXT_HALF = 0xf9
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
private const val HEADER_TAG: Int = 0b110_00000

/** Value to represent an indefinite length CBOR item within a "length stack". */
private const val LENGTH_STACK_INDEFINITE = -1

private const val HALF_PRECISION_EXPONENT_BIAS = 15
private const val HALF_PRECISION_MAX_EXPONENT = 0x1f
private const val HALF_PRECISION_MAX_MANTISSA = 0x3ff

private const val SINGLE_PRECISION_EXPONENT_BIAS = 127
private const val SINGLE_PRECISION_MAX_EXPONENT = 0xFF

private const val SINGLE_PRECISION_NORMALIZE_BASE = 0.5f

// Writes class as map [fieldName, fieldValue]
internal open class CborWriter(private val cbor: Cbor, protected val encoder: CborEncoder) : AbstractEncoder() {


    var encodeByteArrayAsByteString = false

    inner class Node(
        val descriptor: SerialDescriptor?,
        var data: Any?,
        var parent: Node?,
        val index: Int,
        val name: String?,
        val label: Long? = null,
    ) {
        val children = mutableListOf<Node>()

        override fun toString(): String {
            return "(${descriptor?.serialName}:${descriptor?.kind}, $data, ${children.joinToString { it.toString() }})"
        }

        fun encode() {
            val childNodes =
                if (!cbor.explicitNulls && (descriptor?.kind is StructureKind.CLASS || descriptor?.kind is StructureKind.OBJECT)) {
                    children.filterNot { (it.data as ByteArray?)?.contentEquals(byteArrayOf(NULL.toByte())) == true }
                } else children

            encodeElementPreamble()

            if (parent?.descriptor?.isByteString(index) != true) {
                if (children.isNotEmpty()) { //TODO: base this on structurekind not on emptyiness
                    if (descriptor != null)
                        if (descriptor.hasArrayTag()) {
                            descriptor.getArrayTags()?.forEach { encoder.encodeTag(it) }
                            if (cbor.writeDefiniteLengths) encoder.startArray(childNodes.size.toULong()) else encoder.startArray()
                        } else {
                            when (descriptor.kind) {
                                StructureKind.LIST, is PolymorphicKind -> {
                                    if (cbor.writeDefiniteLengths) encoder.startArray(childNodes.size.toULong())
                                    else encoder.startArray()
                                }

                                is StructureKind.MAP -> {
                                    if (cbor.writeDefiniteLengths) encoder.startMap((childNodes.size / 2).toULong()) else encoder.startMap()
                                }

                                else -> {
                                    if (cbor.writeDefiniteLengths) encoder.startMap(childNodes.size.toULong()) else encoder.startMap()
                                }
                            }
                        }

                }

                childNodes.forEach { it.encode() }
                if (children.isNotEmpty() && descriptor != null && !cbor.writeDefiniteLengths) encoder.end()

            }
            //byteStrings are encoded into the data already, as are primitives
            data?.let {
                it as ByteArray; it.forEach { encoder.writeByte(it.toInt()) }
            }

        }

        private fun encodeElementPreamble() {

            if (parent?.descriptor?.hasArrayTag() != true) {
                if (cbor.writeKeyTags) {
                    parent?.descriptor?.getKeyTags(index)?.forEach { encoder.encodeTag(it) }
                }
                if ((parent?.descriptor?.kind !is StructureKind.LIST) && (parent?.descriptor?.kind !is StructureKind.MAP)) { //TODO polymorphicKind?
                    //indieces are put into the name field. we don't want to write those, as it would result in double writes
                    if (cbor.preferSerialLabelsOverNames && label != null) {
                        encoder.encodeNumber(label)
                    } else if (name != null) {
                        encoder.encodeString(name)
                    }
                }
            }

            if (cbor.writeValueTags) {
                parent?.descriptor?.getValueTags(index)?.forEach { encoder.encodeTag(it) }
            }

        }
    }


    private var currentNode = Node(null, null, null, -1, null)
    fun encode() {
        currentNode.encode()
    }


    override val serializersModule: SerializersModule
        get() = cbor.serializersModule

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if ((encodeByteArrayAsByteString || cbor.alwaysUseByteString)
            && serializer.descriptor == ByteArraySerializer().descriptor) {
            currentNode.children.last().data =
                ByteArrayOutput().also { CborEncoder(it).encodeByteString(value as ByteArray) }.toByteArray()
        } else {
            super.encodeSerializableValue(serializer, value)
        }
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = cbor.encodeDefaults


    //todo: Write size of map or array if known
    @OptIn(ExperimentalSerializationApi::class)
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (currentNode.parent == null)
            currentNode = Node(
                descriptor,
                null,
                currentNode,
                -1,
                null
            ).apply { currentNode.children += this }
        else {
            currentNode = currentNode.children.last()
        }
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        currentNode = currentNode.parent ?: throw SerializationException("Root node reached!")

    }


    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        encodeByteArrayAsByteString = descriptor.isByteString(index)
        currentNode.children += Node(
            descriptor.getElementDescriptor(index),
            null,
            currentNode,
            index,
            descriptor.getElementName(index),
            descriptor.getSerialLabel(index),
        )
        return true
    }

    //If any of the following functions are called for serializing raw primitives (i.e. something other than a class,
    // list, map or array, no children exist and the root node needs the data
    override fun encodeString(value: String) {
        (currentNode.children.lastOrNull() ?: currentNode).apply {
            data =
                ByteArrayOutput().also { CborEncoder(it).encodeString(value) }.toByteArray()

        }
    }

    override fun encodeFloat(value: Float) {
        (currentNode.children.lastOrNull() ?: currentNode).apply {
            data =
                ByteArrayOutput().also { CborEncoder(it).encodeFloat(value) }.toByteArray()
        }
    }

    override fun encodeDouble(value: Double) {
        (currentNode.children.lastOrNull() ?: currentNode).apply {
            data =
                ByteArrayOutput().also { CborEncoder(it).encodeDouble(value) }.toByteArray()
        }
    }

    override fun encodeChar(value: Char) {
        (currentNode.children.lastOrNull() ?: currentNode).apply {
            data =
                ByteArrayOutput().also { CborEncoder(it).encodeNumber(value.code.toLong()) }.toByteArray()
        }
    }

    override fun encodeByte(value: Byte) {
        (currentNode.children.lastOrNull() ?: currentNode).apply {
            data =
                ByteArrayOutput().also { CborEncoder(it).encodeNumber(value.toLong()) }.toByteArray()
        }
    }

    override fun encodeShort(value: Short) {
        (currentNode.children.lastOrNull() ?: currentNode).apply {
            data =
                ByteArrayOutput().also { CborEncoder(it).encodeNumber(value.toLong()) }.toByteArray()
        }
    }

    override fun encodeInt(value: Int) {
        (currentNode.children.lastOrNull() ?: currentNode).apply {
            data =
                ByteArrayOutput().also { CborEncoder(it).encodeNumber(value.toLong()) }.toByteArray()
        }
    }

    override fun encodeLong(value: Long) {
        (currentNode.children.lastOrNull() ?: currentNode).apply {
            data =
                ByteArrayOutput().also { CborEncoder(it).encodeNumber(value) }.toByteArray()
        }
    }

    override fun encodeBoolean(value: Boolean) {
        (currentNode.children.lastOrNull() ?: currentNode).apply {
            data =
                ByteArrayOutput().also { CborEncoder(it).encodeBoolean(value) }.toByteArray()
        }
    }

    override fun encodeNull() {
        (currentNode.children.lastOrNull() ?: currentNode).apply {
            data = ByteArrayOutput().also { CborEncoder(it).encodeNull() }.toByteArray()
        }
    }

    @OptIn(ExperimentalSerializationApi::class) // KT-46731
    override fun encodeEnum(
        enumDescriptor: SerialDescriptor,
        index: Int
    ) {
        (currentNode.children.lastOrNull() ?: currentNode).apply {
            data =
                ByteArrayOutput().also { CborEncoder(it).encodeString(enumDescriptor.getElementName(index)) }
                    .toByteArray()
        }
    }

}

// For details of representation, see https://tools.ietf.org/html/rfc7049#section-2.1
internal class CborEncoder(private val output: ByteArrayOutput) {

    fun startArray() = output.write(BEGIN_ARRAY)

    fun startArray(size: ULong) {
        val encodedNumber = composePositive(size)
        encodedNumber[0] = encodedNumber[0] or HEADER_ARRAY.toUByte().toByte()
        encodedNumber.forEach { writeByte(it.toUByte().toInt()) }
    }

    fun startMap() = output.write(BEGIN_MAP)

    fun startMap(size: ULong) {
        val encodedNumber = composePositive(size)
        encodedNumber[0] = encodedNumber[0] or HEADER_MAP.toUByte().toByte()
        encodedNumber.forEach { writeByte(it.toUByte().toInt()) }
    }

    fun encodeTag(tag: ULong) {
        val encodedTag = composePositive(tag)
        encodedTag[0] = encodedTag[0] or HEADER_TAG.toUByte().toByte()
        encodedTag.forEach { writeByte(it.toUByte().toInt()) }
    }

    fun end() = output.write(BREAK)

    fun encodeNull() = output.write(NULL)

    internal fun writeByte(byteValue: Int) = output.write(byteValue)

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
        if (value >= 0) composePositive(value.toULong()) else composeNegative(value)

    internal fun composePositive(value: ULong): ByteArray = when (value) {
        in 0u..23u -> byteArrayOf(value.toByte())
        in 24u..UByte.MAX_VALUE.toUInt() -> byteArrayOf(24, value.toByte())
        in (UByte.MAX_VALUE.toUInt() + 1u)..UShort.MAX_VALUE.toUInt() -> encodeToByteArray(value, 2, 25)
        in (UShort.MAX_VALUE.toUInt() + 1u)..UInt.MAX_VALUE -> encodeToByteArray(value, 4, 26)
        else -> encodeToByteArray(value, 8, 27)
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
}

private class CborMapReader(cbor: Cbor, decoder: CborDecoder) : CborListReader(cbor, decoder) {
    override fun skipBeginToken() = setSize(decoder.startMap(tags) * 2)
}

private open class CborListReader(cbor: Cbor, decoder: CborDecoder) : CborReader(cbor, decoder) {
    private var ind = 0

    override fun skipBeginToken() = setSize(decoder.startArray(tags))

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (!finiteMode && decoder.isEnd() || (finiteMode && ind >= size)) CompositeDecoder.DECODE_DONE else
            ind++.also {
                decodeByteArrayAsByteString = descriptor.isByteString(it)
            }
    }
}

private class CborDefiniteListReader(
    cbor: Cbor,
    decoder: CborDecoder,
    private val expectedSize: ULong,
    private val tag: ULongArray?
) : CborListReader(cbor, decoder) {

}

internal open class CborReader(private val cbor: Cbor, protected val decoder: CborDecoder) : AbstractDecoder() {

    protected var size = -1
        private set
    protected var finiteMode = false
        private set
    private var readProperties: Int = 0

    protected var decodeByteArrayAsByteString = false
    protected var tags: ULongArray? = null

    protected fun setSize(size: Int) {
        if (size >= 0) {
            finiteMode = true
            this.size = size
        }
    }

    override val serializersModule: SerializersModule
        get() = cbor.serializersModule

    protected open fun skipBeginToken() = setSize(decoder.startMap(tags))

    @OptIn(ExperimentalSerializationApi::class)
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val re = if (descriptor.hasArrayTag()) {
            CborDefiniteListReader(cbor, decoder, descriptor.elementNames.count().toULong(), descriptor.getArrayTags())
        } else when (descriptor.kind) {
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
        val index = if (cbor.ignoreUnknownKeys) {
            val knownIndex: Int
            while (true) {
                if (isDone()) return CompositeDecoder.DECODE_DONE
                val (elemName, tags) = decodeElementNameWithTagsLenient(descriptor)
                readProperties++

                val index = elemName?.let { descriptor.getElementIndex(it) } ?: CompositeDecoder.UNKNOWN_NAME
                if (index == CompositeDecoder.UNKNOWN_NAME) {
                    decoder.skipElement(tags)
                } else {
                    if (cbor.verifyKeyTags) {
                        descriptor.getKeyTags(index)?.let { keyTags ->
                            if (!(keyTags contentEquals tags)) throw CborDecodingException("CBOR tags $tags do not match declared tags $keyTags")
                        }
                    }
                    knownIndex = index
                    break
                }
            }
            knownIndex
        } else {
            if (isDone()) return CompositeDecoder.DECODE_DONE
            val (elemName, tags) = decodeElementNameWithTags(descriptor)
            readProperties++
            descriptor.getElementIndexOrThrow(elemName).also { index ->
                if (cbor.verifyKeyTags) {
                    descriptor.getKeyTags(index)?.let { keyTags ->
                        if (!(keyTags contentEquals tags)) throw CborDecodingException("CBOR tags $tags do not match declared tags $keyTags")
                    }
                }
            }
        }

        decodeByteArrayAsByteString = descriptor.isByteString(index)
        tags = if (cbor.verifyValueTags) descriptor.getValueTags(index) else null
        return index
    }

    private fun decodeElementNameWithTags(descriptor: SerialDescriptor): Pair<String, ULongArray?> {
        var (elemName, serialLabel, tags) = decoder.nextTaggedStringOrNumber()
        if (elemName == null && serialLabel != null) {
            elemName = descriptor.getElementNameForSerialLabel(serialLabel)
                ?: throw CborDecodingException("SerialLabel unknown: $serialLabel")
        }
        if (elemName == null) {
            throw CborDecodingException("Expected (tagged) string or number, got nothing")
        }
        return elemName to tags
    }

    private fun decodeElementNameWithTagsLenient(descriptor: SerialDescriptor): Pair<String?, ULongArray?> {
        var (elemName, serialLabel, tags) = decoder.nextTaggedStringOrNumber()
        if (elemName == null && serialLabel != null) {
            elemName = descriptor.getElementNameForSerialLabel(serialLabel)
        }
        return elemName to tags
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return if ((decodeByteArrayAsByteString || cbor.alwaysUseByteString)
            && deserializer.descriptor == ByteArraySerializer().descriptor) {
            @Suppress("UNCHECKED_CAST")
            decoder.nextByteString(tags) as T
        } else {
            super.decodeSerializableValue(deserializer)
        }
    }

    override fun decodeString() = decoder.nextString(tags)

    override fun decodeNotNullMark(): Boolean = !decoder.isNull()

    override fun decodeDouble() = decoder.nextDouble(tags)
    override fun decodeFloat() = decoder.nextFloat(tags)

    override fun decodeBoolean() = decoder.nextBoolean(tags)

    override fun decodeByte() = decoder.nextNumber(tags).toByte()
    override fun decodeShort() = decoder.nextNumber(tags).toShort()
    override fun decodeChar() = decoder.nextNumber(tags).toInt().toChar()
    override fun decodeInt() = decoder.nextNumber(tags).toInt()
    override fun decodeLong() = decoder.nextNumber(tags)

    override fun decodeNull() = decoder.nextNull(tags)

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int =
        enumDescriptor.getElementIndexOrThrow(decoder.nextString(tags))

    private fun isDone(): Boolean = !finiteMode && decoder.isEnd() || (finiteMode && readProperties >= size)
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

    fun isEof() = curByte == -1

    private fun skipByte(expected: Int) {
        if (curByte != expected) throw CborDecodingException("byte ${printByte(expected)}", curByte)
        readByte()
    }

    fun isNull() = curByte == NULL

    fun nextNull(tag: ULong) = nextNull(ulongArrayOf(tag))
    fun nextNull(tags: ULongArray? = null): Nothing? {
        processTags(tags)
        skipByte(NULL)
        return null
    }

    fun nextBoolean(tag: ULong) = nextBoolean(ulongArrayOf(tag))

    fun nextBoolean(tags: ULongArray? = null): Boolean {
        processTags(tags)
        val ans = when (curByte) {
            TRUE -> true
            FALSE -> false
            else -> throw CborDecodingException("boolean value", curByte)
        }
        readByte()
        return ans
    }

    fun startArray(tag: ULong) = startArray(ulongArrayOf(tag))

    fun startArray(tags: ULongArray? = null) = startSized(tags, BEGIN_ARRAY, HEADER_ARRAY, "array")

    fun startMap(tag: ULong) = startMap(ulongArrayOf(tag))

    fun startMap(tags: ULongArray? = null) = startSized(tags, BEGIN_MAP, HEADER_MAP, "map")

    private fun startSized(
        tags: ULongArray?,
        unboundedHeader: Int,
        boundedHeaderMask: Int,
        collectionType: String
    ): Int {
        processTags(tags)
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

    fun nextByteString(tag: ULong) = nextByteString(ulongArrayOf(tag))
    fun nextByteString(tags: ULongArray? = null): ByteArray {
        processTags(tags)
        if ((curByte and 0b111_00000) != HEADER_BYTE_STRING.toInt())
            throw CborDecodingException("start of byte string", curByte)
        val arr = readBytes()
        readByte()
        return arr
    }

    fun nextString(tag: ULong) = nextString(ulongArrayOf(tag))
    fun nextString(tags: ULongArray? = null) = nextTaggedString(tags).first

    //used for reading the tag names and names of tagged keys (of maps, and serialized classes)
    fun nextTaggedString() = nextTaggedString(null)

    private fun nextTaggedString(tags: ULongArray?): Pair<String, ULongArray?> {
        val collectedTags = processTags(tags)
        if ((curByte and 0b111_00000) != HEADER_STRING.toInt())
            throw CborDecodingException("start of string", curByte)
        val arr = readBytes()
        val ans = arr.decodeToString()
        readByte()
        return ans to collectedTags
    }

    private fun readBytes(): ByteArray =
        if (curByte and 0b000_11111 == ADDITIONAL_INFORMATION_INDEFINITE_LENGTH) {
            readByte()
            readIndefiniteLengthBytes()
        } else {
            val strLen = readNumber().toInt()
            input.readExactNBytes(strLen)
        }

    private fun processTags(tags: ULongArray?): ULongArray? {
        var index = 0
        val collectedTags = mutableListOf<ULong>()
        while ((curByte and 0b111_00000) == HEADER_TAG) {
            val readTag = readNumber().toULong() // This is the tag number
            collectedTags += readTag
            tags?.let {
                if (index++ > it.size) throw CborDecodingException("More tags found than the ${it.size} tags specified.")
                if (readTag != it[index - 1]) throw CborDecodingException("CBOR tag $readTag does not match expected tag $it")
            }
            readByte()
        }

        return (if (collectedTags.isEmpty()) null else collectedTags.toULongArray()).also { collected ->
            tags?.let {
                if (!it.contentEquals(collected)) throw CborDecodingException(
                    "CBOR tags ${
                        collected?.joinToString(
                            prefix = "[",
                            postfix = "]"
                        ) { it.toString() }
                    } do not match expected tags ${
                        it.joinToString(
                            prefix = "[",
                            postfix = "]"
                        ) { it.toString() }
                    }"
                )
            }
        }
    }

    /**
     * Used for reading the tags and either string (element name) or number (serial label)
     */
    fun nextTaggedStringOrNumber(): Triple<String?, Long?, ULongArray?> {
        val collectedTags = processTags(null)
        if ((curByte and 0b111_00000) == HEADER_STRING.toInt()) {
            val arr = readBytes()
            val ans = arr.decodeToString()
            readByte()
            return Triple(ans, null, collectedTags)
        } else {
            val res = readNumber()
            readByte()
            return Triple(null, res, collectedTags)
        }
    }

    fun nextNumber(tag: ULong): Long = nextNumber(ulongArrayOf(tag))
    fun nextNumber(tags: ULongArray? = null): Long {
        processTags(tags)
        val res = readNumber()
        readByte()
        return res
    }
    fun nextTaggedNumber() = nextTaggedNumber(null)

    private fun nextTaggedNumber(tags: ULongArray?): Pair<Long, ULongArray?> {
        val collectedTags = processTags(tags)
        val res = readNumber()
        readByte()
        return res to collectedTags
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

    fun nextFloat(tag: ULong) = nextFloat(ulongArrayOf(tag))

    fun nextFloat(tags: ULongArray? = null): Float {
        processTags(tags)
        val res = when (curByte) {
            NEXT_FLOAT -> Float.fromBits(readInt())
            NEXT_HALF -> floatFromHalfBits(readShort())
            else -> throw CborDecodingException("float header", curByte)
        }
        readByte()
        return res
    }

    fun nextDouble(tag: ULong) = nextDouble(ulongArrayOf(tag))

    fun nextDouble(tags: ULongArray? = null): Double {
        processTags(tags)
        val res = when (curByte) {
            NEXT_DOUBLE -> Double.fromBits(readLong())
            NEXT_FLOAT -> Float.fromBits(readInt()).toDouble()
            NEXT_HALF -> floatFromHalfBits(readShort()).toDouble()
            else -> throw CborDecodingException("double header", curByte)
        }
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

    private fun readShort(): Short {
        val highByte = input.read()
        val lowByte = input.read()
        return (highByte shl 8 or lowByte).toShort()
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
     * Skips the current value element. Bytes are processed to determine the element type (and corresponding length), to
     * determine how many bytes to skip.
     *
     * For primitive (finite length) elements (e.g. unsigned integer, text string), their length is read and
     * corresponding number of bytes are skipped.
     *
     * For elements that contain children (e.g. array, map), the child count is read and added to a "length stack"
     * (which represents the "number of elements" at each depth of the CBOR data structure). When a child element has
     * been skipped, the "length stack" is [pruned][prune]. For indefinite length elements, a special marker is added to
     * the "length stack" which is only popped from the "length stack" when a CBOR [break][isEnd] is encountered.
     */
    fun skipElement(tags: ULongArray? = null) {
        val lengthStack = mutableListOf<Int>()

        processTags(tags)

        do {
            if (isEof()) throw CborDecodingException("Unexpected EOF while skipping element")

            if (isIndefinite()) {
                lengthStack.add(LENGTH_STACK_INDEFINITE)
            } else if (isEnd()) {
                if (lengthStack.removeLastOrNull() != LENGTH_STACK_INDEFINITE)
                    throw CborDecodingException("next data item", curByte)
                prune(lengthStack)
            } else {
                val header = curByte and 0b111_00000
                val length = elementLength()
                if (header == HEADER_ARRAY || header == HEADER_MAP) {
                    if (length > 0) lengthStack.add(length)
                    processTags(tags)
                } else {
                    input.skip(length)
                    prune(lengthStack)
                }
            }

            readByte()
        } while (lengthStack.isNotEmpty())
    }

    fun skipElement(singleTag: ULong?) = skipElement(singleTag?.let { ulongArrayOf(it) })

    /**
     * Removes an item from the top of the [lengthStack], cascading the removal if the item represents the last item
     * (i.e. a length value of `1`) at its stack depth.
     *
     * For example, pruning a [lengthStack] of `[3, 2, 1, 1]` would result in `[3, 1]`.
     */
    private fun prune(lengthStack: MutableList<Int>) {
        for (i in lengthStack.lastIndex downTo 0) {
            when (lengthStack[i]) {
                LENGTH_STACK_INDEFINITE -> break
                1 -> lengthStack.removeAt(i)
                else -> {
                    lengthStack[i] = lengthStack[i] - 1
                    break
                }
            }
        }
    }

    /**
     * Determines if [curByte] represents an indefinite length CBOR item.
     *
     * Per [RFC 7049: 2.2. Indefinite Lengths for Some Major Types](https://tools.ietf.org/html/rfc7049#section-2.2):
     * > Four CBOR items (arrays, maps, byte strings, and text strings) can be encoded with an indefinite length
     */
    private fun isIndefinite(): Boolean {
        val majorType = curByte and 0b111_00000
        val value = curByte and 0b000_11111

        return value == ADDITIONAL_INFORMATION_INDEFINITE_LENGTH &&
            (majorType == HEADER_ARRAY || majorType == HEADER_MAP ||
                majorType == HEADER_BYTE_STRING.toInt() || majorType == HEADER_STRING.toInt())
    }

    /**
     * Determines the length of the CBOR item represented by [curByte]; length has specific meaning based on the type:
     *
     * | Major type          | Length represents number of... |
     * |---------------------|--------------------------------|
     * | 0. unsigned integer | bytes                          |
     * | 1. negative integer | bytes                          |
     * | 2. byte string      | bytes                          |
     * | 3. string           | bytes                          |
     * | 4. array            | data items (values)            |
     * | 5. map              | sub-items (keys + values)      |
     * | 6. tag              | bytes                          |
     */
    private fun elementLength(): Int {
        val majorType = curByte and 0b111_00000
        val additionalInformation = curByte and 0b000_11111

        return when (majorType) {
            HEADER_BYTE_STRING.toInt(), HEADER_STRING.toInt(), HEADER_ARRAY -> readNumber().toInt()
            HEADER_MAP -> readNumber().toInt() * 2
            else -> when (additionalInformation) {
                24 -> 1
                25 -> 2
                26 -> 4
                27 -> 8
                else -> 0
            }
        }
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
        throw SerializationException(
            "$serialName does not contain element with name '$name." +
                " You can enable 'CborBuilder.ignoreUnknownKeys' property to ignore unknown keys"
        )
    return index
}

private fun Iterable<ByteArray>.flatten(): ByteArray {
    val output = ByteArray(sumOf { it.size })
    var position = 0
    for (chunk in this) {
        chunk.copyInto(output, position)
        position += chunk.size
    }

    return output
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.isByteString(index: Int): Boolean {
    return kotlin.runCatching { getElementAnnotations(index).find { it is ByteString } != null }.getOrDefault(false)
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getValueTags(index: Int): ULongArray? {
    return kotlin.runCatching { (getElementAnnotations(index).find { it is ValueTags } as ValueTags?)?.tags }.getOrNull()
}
private fun SerialDescriptor.isInlineByteString(): Boolean {
    // inline item classes should only have 1 item
    return isInline && isByteString(0)
}


@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getKeyTags(index: Int): ULongArray? {
    return kotlin.runCatching { (getElementAnnotations(index).find { it is KeyTags } as KeyTags?)?.tags }.getOrNull()
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.isByteString(): Boolean {
    return annotations.find { it is ByteString } != null
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getValueTags(): ULongArray? {
    return (annotations.find { it is ValueTags } as ValueTags?)?.tags
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getKeyTags(): ULongArray? {
    return (annotations.find { it is KeyTags } as KeyTags?)?.tags
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getSerialLabel(index: Int): Long? {
    return kotlin.runCatching { getElementAnnotations(index).filterIsInstance<SerialLabel>().firstOrNull()?.label }.getOrNull()
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getElementNameForSerialLabel(label: Long): String? {
    return elementNames.firstOrNull { getSerialLabel(getElementIndex(it)) == label }
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getArrayTags(): ULongArray? {
    return annotations.filterIsInstance<CborArray>().firstOrNull()?.tag
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.hasArrayTag(): Boolean {
    return annotations.filterIsInstance<CborArray>().isNotEmpty()
}

private val normalizeBaseBits = SINGLE_PRECISION_NORMALIZE_BASE.toBits()


/*
 * For details about half-precision floating-point numbers see https://tools.ietf.org/html/rfc7049#appendix-D
 */
private fun floatFromHalfBits(bits: Short): Float {
    val intBits = bits.toInt()

    val negative = (intBits and 0x8000) != 0
    val halfExp = intBits shr 10 and HALF_PRECISION_MAX_EXPONENT
    val halfMant = intBits and HALF_PRECISION_MAX_MANTISSA

    val exp: Int
    val mant: Int

    when (halfExp) {
        HALF_PRECISION_MAX_EXPONENT -> {
            // if exponent maximal - value is NaN or Infinity
            exp = SINGLE_PRECISION_MAX_EXPONENT
            mant = halfMant
        }

        0 -> {
            if (halfMant == 0) {
                // if exponent and mantissa are zero - value is zero
                mant = 0
                exp = 0
            } else {
                // if exponent is zero and mantissa non-zero - value denormalized. normalize it
                var res = Float.fromBits(normalizeBaseBits + halfMant)
                res -= SINGLE_PRECISION_NORMALIZE_BASE
                return if (negative) -res else res
            }
        }

        else -> {
            // normalized value
            exp = (halfExp + (SINGLE_PRECISION_EXPONENT_BIAS - HALF_PRECISION_EXPONENT_BIAS))
            mant = halfMant
        }
    }

    val res = Float.fromBits((exp shl 23) or (mant shl 13))
    return if (negative) -res else res
}
