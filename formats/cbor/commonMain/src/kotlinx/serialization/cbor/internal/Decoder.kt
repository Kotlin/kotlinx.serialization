/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.cbor.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

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

    protected open fun skipBeginToken() = setSize(decoder.startMap())

    @OptIn(ExperimentalSerializationApi::class)
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val re = if (descriptor.hasArrayTag()) {
            CborListReader(cbor, decoder)
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
                    verifyKeyTags(descriptor, index, tags)
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
                verifyKeyTags(descriptor, index, tags)
            }
        }

        decodeByteArrayAsByteString = descriptor.isByteString(index)
        tags = if (cbor.verifyValueTags) descriptor.getValueTags(index) else null
        return index
    }


    private fun decodeElementNameWithTags(descriptor: SerialDescriptor): Pair<String, ULongArray?> {
        var (elemName, cborLabel, tags) = decoder.nextTaggedStringOrNumber()
        if (elemName == null && cborLabel != null) {
            elemName = descriptor.getElementNameForCborLabel(cborLabel)
                ?: throw CborDecodingException("CborLabel unknown: $cborLabel for $descriptor")
        }
        if (elemName == null) {
            throw CborDecodingException("Expected (tagged) string or number, got nothing for $descriptor")
        }
        return elemName to tags
    }

    private fun decodeElementNameWithTagsLenient(descriptor: SerialDescriptor): Pair<String?, ULongArray?> {
        var (elemName, cborLabel, tags) = decoder.nextTaggedStringOrNumber()
        if (elemName == null && cborLabel != null) {
            elemName = descriptor.getElementNameForCborLabel(cborLabel)
        }
        return elemName to tags
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return if ((decodeByteArrayAsByteString || cbor.alwaysUseByteString)
            && deserializer.descriptor == ByteArraySerializer().descriptor
        ) {
            @Suppress("UNCHECKED_CAST")
            decoder.nextByteString(tags) as T
        } else {
            decodeByteArrayAsByteString = decodeByteArrayAsByteString || deserializer.descriptor.isInlineByteString()
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

    private fun verifyKeyTags(descriptor: SerialDescriptor, index: Int, tags: ULongArray?) {
        if (cbor.verifyKeyTags) {
            descriptor.getKeyTags(index)?.let { keyTags ->
                if (!(keyTags contentEquals tags)) throw CborDecodingException("CBOR tags $tags do not match declared tags $keyTags for $descriptor")
            }
        }
    }
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

    fun isNull() = (curByte == NULL || curByte == EMPTY_MAP)

    fun nextNull(tag: ULong) = nextNull(ulongArrayOf(tag))
    fun nextNull(tags: ULongArray? = null): Nothing? {
        processTags(tags)
        if (curByte == NULL) {
            skipByte(NULL)
        } else if (curByte == EMPTY_MAP) {
            skipByte(EMPTY_MAP)
        }
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
            //We only want to compare if tags are actually set, otherwise, we don't care
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

private fun Iterable<ByteArray>.flatten(): ByteArray {
    val output = ByteArray(sumOf { it.size })
    var position = 0
    for (chunk in this) {
        chunk.copyInto(output, position)
        position += chunk.size
    }

    return output
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


@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getElementNameForCborLabel(label: Long): String? {
    return elementNames.firstOrNull { getCborLabel(getElementIndex(it)) == label }
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
