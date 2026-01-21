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

internal open class CborReader(override val cbor: Cbor, protected val parser: CborParser) : AbstractDecoder(),
    CborDecoder {

    protected var size = -1
        private set
    protected var finiteMode = false
        private set
    private var readProperties: Int = 0

    protected var decodeByteArrayAsByteString = false
    protected var tags: ULongArray? = null

    /**
     * Keys that have been seen so far while reading this map.
     *
     * Only used if [Cbor.configuration.forbidDuplicateKeys] is in effect.
     */
    private val seenKeys = mutableSetOf<Any?>()

    protected fun setSize(size: Int) {
        if (size >= 0) {
            finiteMode = true
            this.size = size
        }
    }

    override val serializersModule: SerializersModule
        get() = cbor.serializersModule

    protected open fun skipBeginToken(objectTags: ULongArray?) = setSize(parser.startMap(objectTags))

    @OptIn(ExperimentalSerializationApi::class)
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val re = if (descriptor.hasArrayTag()) {
            CborListReader(cbor, parser)
        } else when (descriptor.kind) {
            StructureKind.LIST, is PolymorphicKind -> CborListReader(cbor, parser)
            StructureKind.MAP -> CborMapReader(cbor, parser)
            else -> CborReader(cbor, parser)
        }
        val objectTags = if (cbor.configuration.verifyObjectTags) descriptor.getObjectTags() else null
        re.skipBeginToken(tags?.let { if (objectTags == null) it else ulongArrayOf(*it, *objectTags) } ?: objectTags)
        return re
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (!finiteMode) parser.end()
    }

    override fun visitKey(key: Any?) {
        if (cbor.configuration.forbidDuplicateKeys) {
            seenKeys.add(key) || throw DuplicateKeyException(key)
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val index = if (cbor.configuration.ignoreUnknownKeys) {
            val knownIndex: Int
            while (true) {
                if (isDone()) return CompositeDecoder.DECODE_DONE
                val (elemName, tags) = decodeElementNameWithTagsLenient(descriptor)
                visitKey(elemName)
                readProperties++

                val index = elemName?.let { descriptor.getElementIndex(it) } ?: CompositeDecoder.UNKNOWN_NAME
                if (index == CompositeDecoder.UNKNOWN_NAME) {
                    parser.skipElement(tags)
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
            visitKey(elemName)
            readProperties++
            descriptor.getElementIndexOrThrow(elemName).also { index ->
                verifyKeyTags(descriptor, index, tags)
            }
        }

        decodeByteArrayAsByteString = descriptor.isByteString(index)
        tags = if (cbor.configuration.verifyValueTags) descriptor.getValueTags(index) else null
        return index
    }


    private fun decodeElementNameWithTags(descriptor: SerialDescriptor): Pair<String, ULongArray?> {
        var (elemName, cborLabel, tags) = parser.nextTaggedStringOrNumber()
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
        var (elemName, cborLabel, tags) = parser.nextTaggedStringOrNumber()
        if (elemName == null && cborLabel != null) {
            elemName = descriptor.getElementNameForCborLabel(cborLabel)
        }
        return elemName to tags
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return if ((decodeByteArrayAsByteString || cbor.configuration.alwaysUseByteString)
            && deserializer.descriptor == ByteArraySerializer().descriptor
        ) {
            @Suppress("UNCHECKED_CAST")
            parser.nextByteString(tags) as T
        } else {
            decodeByteArrayAsByteString = decodeByteArrayAsByteString || deserializer.descriptor.isInlineByteString()
            super<AbstractDecoder>.decodeSerializableValue(deserializer)
        }
    }

    override fun decodeString() = parser.nextString(tags)

    override fun decodeNotNullMark(): Boolean = !parser.isNull()

    override fun decodeDouble() = parser.nextDouble(tags)
    override fun decodeFloat() = parser.nextFloat(tags)

    override fun decodeBoolean() = parser.nextBoolean(tags)

    override fun decodeByte() = parser.nextNumberWithinRange(
        tags, Byte.MIN_VALUE.toLong(), Byte.MAX_VALUE.toLong(), "Byte"
    ).toByte()
    override fun decodeShort() = parser.nextNumberWithinRange(
        tags, Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong(), "Short"
    ).toShort()
    override fun decodeChar() = parser.nextNumberWithinRange(
        tags, Char.MIN_VALUE.code.toLong(), Char.MAX_VALUE.code.toLong(), "Char"
    ).toInt().toChar()
    override fun decodeInt() = parser.nextNumberWithinRange(
        tags, Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong(), "Int"
    ).toInt()
    override fun decodeLong() = parser.nextNumber(tags)

    override fun decodeNull() = parser.nextNull(tags)

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int =
        enumDescriptor.getElementIndexOrThrow(parser.nextString(tags))

    private fun isDone(): Boolean = !finiteMode && parser.isEnd() || (finiteMode && readProperties >= size)

    private fun verifyKeyTags(descriptor: SerialDescriptor, index: Int, tags: ULongArray?) {
        if (cbor.configuration.verifyKeyTags) {
            descriptor.getKeyTags(index)?.let { keyTags ->
                parser.verifyTagsAndThrow(keyTags, tags)
            }
        }
    }
}

internal class CborParser(private val input: ByteArrayInput, private val verifyObjectTags: Boolean) {
    private var curByteOrEof: Int = -1

    private fun peekCurByteOrFail(): Int {
        if (curByteOrEof == -1) throw CborDecodingException("Unexpected end of encoded CBOR document")
        return curByteOrEof
    }

    init {
        readByte()
    }

    @IgnorableReturnValue
    private fun readByte(): Int {
        curByteOrEof = input.read()
        return curByteOrEof
    }

    fun isEof() = curByteOrEof == -1

    private fun skipByte(expected: Int) {
        val byte = peekCurByteOrFail()
        if (byte != expected) throw CborDecodingException("byte ${printByte(expected)}", byte)
        readByte()
    }

    fun isNull() = with(peekCurByteOrFail()) { this == NULL || this == EMPTY_MAP }

    fun nextNull(tags: ULongArray? = null): Nothing? {
        processTags(tags)
        if (isNull()) {
            /* val _ = */ readByte()
            return null
        }
        throw CborDecodingException(
            "null value (${NULL.toHexString()}) or empty map (${EMPTY_MAP.toHexString()})",
            peekCurByteOrFail()
        )
    }

    fun nextBoolean(tags: ULongArray? = null): Boolean {
        processTags(tags)
        val ans = when (val byte = peekCurByteOrFail()) {
            TRUE -> true
            FALSE -> false
            else -> throw CborDecodingException("boolean value", byte)
        }
        readByte()
        return ans
    }

    fun startArray(tags: ULongArray? = null) = startSized(tags, BEGIN_ARRAY, HEADER_ARRAY, "array")

    fun startMap(tags: ULongArray? = null) = startSized(tags, BEGIN_MAP, HEADER_MAP, "map")

    private fun startSized(
        tags: ULongArray?,
        unboundedHeader: Int,
        boundedHeaderMask: Int,
        collectionType: String
    ): Int {
        processTags(tags)
        val header = peekCurByteOrFail()
        if (header == unboundedHeader) {
            skipByte(unboundedHeader)
            return -1
        }
        if ((header and MAJOR_TYPE_MASK) != boundedHeaderMask) {
            if (boundedHeaderMask == HEADER_ARRAY && (header and MAJOR_TYPE_MASK) == HEADER_BYTE_STRING) {
                throw CborDecodingException(
                    "Expected a start of array, " +
                        "but found ${printByte(header)}, which corresponds to the start of a byte string. " +
                        "Make sure you correctly set 'alwaysUseByteString' setting " +
                        "and/or 'kotlinx.serialization.cbor.ByteString' annotation."
                )
            }
            throw CborDecodingException("start of $collectionType", header)
        }
        val majorType = header and MAJOR_TYPE_MASK
        val sizeLimit = if (majorType == HEADER_MAP) Int.MAX_VALUE / 2 else Int.MAX_VALUE
        val size = readUnsignedIntegerIgnoringMajorType { "$collectionType length" }
            .asSizedElementLength(majorType, sizeLimit)
        readByte()
        return size
    }

    fun isEnd() = peekCurByteOrFail() == BREAK

    fun end() = skipByte(BREAK)

    fun nextByteString(tags: ULongArray? = null): ByteArray {
        processTags(tags)
        val header = peekCurByteOrFail()
        if ((header and MAJOR_TYPE_MASK) != HEADER_BYTE_STRING) {
            if (header and MAJOR_TYPE_MASK == HEADER_ARRAY) {
                throw CborDecodingException(
                    "Expected a start of a byte string, " +
                        "but found ${printByte(header)}, which corresponds to the start of an array. " +
                        "Make sure you correctly set 'alwaysUseByteString' setting " +
                        "and/or 'kotlinx.serialization.cbor.ByteString' annotation."
                )
            }
            throw CborDecodingException("start of byte string", header)
        }
        val arr = readBytes()
        readByte()
        return arr
    }

    fun nextString(tags: ULongArray? = null) = nextTaggedString(tags).first

    //used for reading the tag names and names of tagged keys (of maps, and serialized classes)
    private fun nextTaggedString(tags: ULongArray?): Pair<String, ULongArray?> {
        val collectedTags = processTags(tags)
        val headerByte = peekCurByteOrFail()
        if ((headerByte and MAJOR_TYPE_MASK) != HEADER_STRING)
            throw CborDecodingException("start of string", headerByte)
        val arr = readBytes()
        val ans = arr.decodeToString()
        readByte()
        return ans to collectedTags
    }

    private fun readBytes(): ByteArray {
        val headerByte = peekCurByteOrFail()
        return if (headerByte and ADDITIONAL_INFO_MASK == ADDITIONAL_INFORMATION_INDEFINITE_LENGTH) {
            val majorType = headerByte and MAJOR_TYPE_MASK
            readByte()
            readIndefiniteLengthStringChunks(majorType)
        } else {
            val majorType = headerByte and MAJOR_TYPE_MASK
            val strLen = readUnsignedIntegerIgnoringMajorType { "length" }.asSizedElementLength(majorType)
            input.readExactNBytes(strLen)
        }
    }

    @IgnorableReturnValue
    private fun processTags(tags: ULongArray?): ULongArray? {
        var index = 0
        val collectedTags = mutableListOf<ULong>()
        while ((peekCurByteOrFail() and MAJOR_TYPE_MASK) == HEADER_TAG) {
            val readTag = readUnsignedIntegerIgnoringMajorType { "tag" }.toULong() // This is the tag number
            collectedTags += readTag
            // value tags and object tags are intermingled (keyTags are always separate)
            // so this check only holds if we verify both
            if (verifyObjectTags) {
                tags?.let {
                    if (index++ >= it.size) throw CborDecodingException("More tags found than the ${it.size} tags specified")
                }
            }
            readByte()
        }
        return (if (collectedTags.isEmpty()) null else collectedTags.toULongArray()).also { collected ->
            //We only want to compare if tags are actually set, otherwise, we don't care
            tags?.let {
                if (verifyObjectTags) { //again, this check only works if we verify value tags and object tags
                    verifyTagsAndThrow(it, collected)
                } else {
                    // If we don't care for object tags, the best we can do is assure that the collected tags start with
                    // the expected tags. (yes this could co somewhere else, but putting it here groups the code nicely
                    // into if-else branches.
                    if ((collectedTags.size < it.size)
                        || (collectedTags.subList(0, it.size) != it.asList())
                    ) throw CborDecodingException("CBOR tags $collectedTags do not start with specified tags $it")
                }
            }
        }
    }

    internal fun verifyTagsAndThrow(expected: ULongArray, actual: ULongArray?) {
        if (!expected.contentEquals(actual))
            throw CborDecodingException(
                "CBOR tags ${actual?.contentToString()} do not match expected tags ${expected.contentToString()}"
            )
    }

    /**
     * Used for reading the tags and either string (element name) or number (serial label)
     */
    fun nextTaggedStringOrNumber(): Triple<String?, Long?, ULongArray?> {
        val collectedTags = processTags(null)
        val majorType = peekCurByteOrFail()
        if ((majorType and MAJOR_TYPE_MASK) == HEADER_STRING) {
            val arr = readBytes()
            val ans = arr.decodeToString()
            readByte()
            return Triple(ans, null, collectedTags)
        } else {
            val res = readUnsignedIntegerIgnoringMajorType { majorType.majorTypeName }
            readByte()
            return Triple(null, res, collectedTags)
        }
    }

    internal fun nextNumberWithinRange(tags: ULongArray?, from: Long, to: Long, type: String): Long {
        val number = nextNumber(tags)
        if (number !in from..to) {
            throw CborDecodingException("Decoded number $number is not within the range for type $type ([$from..$to])")
        }
        return number
    }

    fun nextNumber(tags: ULongArray? = null): Long {
        processTags(tags)
        val res = readNumber()
        readByte()
        return res
    }

    // Reads a value encoded using rules for the major type 0 (a.k.a. unsigned integers)
    private inline fun readUnsignedIntegerIgnoringMajorType(valueDescriptionForError: () -> String): Long {
        val additionalInfo = peekCurByteOrFail() and ADDITIONAL_INFO_MASK

        if (additionalInfo <= 23) return additionalInfo.toLong()
        val bytesToRead = when (additionalInfo) {
            24 -> 1
            25 -> 2
            26 -> 4
            27 -> 8
            else /* > 27 */ -> throw CborDecodingException(
                "Unexpected value encoding when reading ${valueDescriptionForError()}. " +
                    "Expected addition info value < 28, got $additionalInfo " +
                    "(decoded from ${printByte(peekCurByteOrFail())})"
            )
        }
        return input.readExact(bytesToRead)
    }

    private fun readNumber(): Long {
        val headerByte = peekCurByteOrFail()
        val majorType = headerByte and MAJOR_TYPE_MASK
        if (majorType != HEADER_NEGATIVE.toInt() && majorType != HEADER_POSITIVE.toInt()) {
            throw CborDecodingException("an unsigned or negative integer", headerByte)
        }
        val negative = majorType == HEADER_NEGATIVE.toInt()
        val unsignedValue = readUnsignedIntegerIgnoringMajorType { majorType.majorTypeName }
        return if (negative) -(unsignedValue + 1) else unsignedValue
    }

    private fun ByteArrayInput.readExact(bytes: Int): Long {
        val arr = readExactNBytes(bytes)
        var result = 0L
        for (i in 0 until bytes) {
            result = (result shl 8) or (arr[i].toInt() and 0xFF).toLong()
        }
        return result
    }

    private fun ByteArrayInput.ensureEnoughBytes(bytesCount: Int) {
        if (bytesCount > availableBytes) {
            throw CborDecodingException("Unexpected EOF, available $availableBytes bytes, requested: $bytesCount")
        }
    }

    private fun ByteArrayInput.readExactNBytes(bytesCount: Int): ByteArray {
        ensureEnoughBytes(bytesCount)
        val array = ByteArray(bytesCount)
        val _ = read(array, 0, bytesCount)
        return array
    }

    fun nextFloat(tags: ULongArray? = null): Float {
        processTags(tags)
        val res = when (val headerByte = peekCurByteOrFail()) {
            NEXT_FLOAT -> Float.fromBits(readInt())
            NEXT_HALF -> floatFromHalfBits(readShort())
            else -> throw CborDecodingException("float header", headerByte)
        }
        readByte()
        return res
    }

    fun nextDouble(tags: ULongArray? = null): Double {
        processTags(tags)
        val res = when (val headerByte = peekCurByteOrFail()) {
            NEXT_DOUBLE -> Double.fromBits(readLong())
            NEXT_FLOAT -> Float.fromBits(readInt()).toDouble()
            NEXT_HALF -> floatFromHalfBits(readShort()).toDouble()
            else -> throw CborDecodingException("double header", headerByte)
        }
        readByte()
        return res
    }

    private fun readLong(): Long {
        input.ensureEnoughBytes(Long.SIZE_BYTES)
        var result = 0L
        repeat(Long.SIZE_BYTES) {
            val byte = input.read()
            result = (result shl 8) or byte.toLong()
        }
        return result
    }

    private fun readShort(): Short {
        input.ensureEnoughBytes(Short.SIZE_BYTES)
        val highByte = input.read()
        val lowByte = input.read()
        return (highByte shl 8 or lowByte).toShort()
    }

    private fun readInt(): Int {
        input.ensureEnoughBytes(Int.SIZE_BYTES)
        var result = 0
        repeat(Int.SIZE_BYTES) {
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
    fun skipElement(tags: ULongArray?) {
        val lengthStack = mutableListOf<Int>()

        processTags(tags)

        do {
            if (isIndefinite()) {
                lengthStack.add(LENGTH_STACK_INDEFINITE)
            } else if (isEnd()) {
                if (lengthStack.removeLastOrNull() != LENGTH_STACK_INDEFINITE)
                    throw CborDecodingException("next data item", peekCurByteOrFail())
                prune(lengthStack)
            } else {
                val header = peekCurByteOrFail() and MAJOR_TYPE_MASK
                val length = elementLength()
                if (header == HEADER_TAG) {
                    val _ = readUnsignedIntegerIgnoringMajorType { "tag" }
                } else if (header == HEADER_ARRAY || header == HEADER_MAP) {
                    if (length > 0) lengthStack.add(length)
                    else prune(lengthStack) // empty map or array automatically completes
                } else {
                    input.skip(length)
                    prune(lengthStack)
                }
            }

            readByte()
        } while (lengthStack.isNotEmpty())
    }

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
     * Determines if [peekCurByteOrFail] represents an indefinite length CBOR item.
     *
     * Per [RFC 8949: 3.2. Indefinite Lengths for Some Major Types](https://tools.ietf.org/html/rfc8949#section-3.2):
     * > Four CBOR items (arrays, maps, byte strings, and text strings) can be encoded with an indefinite length
     */
    private fun isIndefinite(): Boolean {
        val curByte = peekCurByteOrFail()
        val majorType = curByte and MAJOR_TYPE_MASK
        val value = curByte and ADDITIONAL_INFO_MASK

        return value == ADDITIONAL_INFORMATION_INDEFINITE_LENGTH &&
            (majorType == HEADER_ARRAY || majorType == HEADER_MAP ||
                majorType == HEADER_BYTE_STRING || majorType == HEADER_STRING)
    }

    /**
     * Determines the length of the CBOR item represented by [peekCurByteOrFail]; length has specific meaning based on the type:
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
        val curByte = peekCurByteOrFail()
        val majorType = curByte and MAJOR_TYPE_MASK
        val additionalInformation = curByte and ADDITIONAL_INFO_MASK

        return when (majorType) {
            HEADER_BYTE_STRING, HEADER_STRING, HEADER_ARRAY
                -> readUnsignedIntegerIgnoringMajorType { "${majorType.majorTypeName} length" }
                    .asSizedElementLength(majorType)
            HEADER_MAP
                -> readUnsignedIntegerIgnoringMajorType { "map length" }
                    .asSizedElementLength(majorType, Int.MAX_VALUE / 2) * 2
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
     * Reads fixed-length chunks constituting indefinite-length byte sequences (either a text, or a byte-string).
     *
     * @param majorType a type of the enclosing indefinite-length sequence ([HEADER_STRING] or [HEADER_BYTE_STRING])
     * @return [ByteArray] containing all of the concatenated bytes found in the buffer.
     */
    private fun readIndefiniteLengthStringChunks(majorType: Int): ByteArray {
        val byteStrings = mutableListOf<ByteArray>()
        do {
            val header = peekCurByteOrFail()
            if (header and MAJOR_TYPE_MASK != majorType) {
                throw CborDecodingException(
                    "a header of a chunk with a major type bits matching $majorType",
                    header
                )
            }
            if (header and ADDITIONAL_INFO_MASK == ADDITIONAL_INFORMATION_INDEFINITE_LENGTH) {
                throw CborDecodingException("a fixed-length chunk", header)
            }
            val length = readUnsignedIntegerIgnoringMajorType { "length of a fixed-length chunk" }
                .asSizedElementLength(majorType)
            byteStrings.add(input.readExactNBytes(length))
            readByte()
        } while (!isEnd())
        return byteStrings.flatten()
    }

    private fun Long.asSizedElementLength(majorType: Int, sizeLimit: Int = Int.MAX_VALUE): Int {
        if (this in 0L..sizeLimit.toLong()) return this.toInt()

        val typeName = majorType.majorTypeName

        if (this < 0) {
            throw CborDecodingException("negative length value was decoded for $typeName: $this")
        }
        throw CborDecodingException("length for $typeName is too large: $this")
    }
}

private val Int.majorTypeName: String
    get() = when (this and MAJOR_TYPE_MASK) {
        HEADER_BYTE_STRING -> "byte string"
        HEADER_STRING -> "string"
        HEADER_ARRAY -> "array"
        HEADER_MAP -> "map"
        HEADER_TAG -> "tag"
        HEADER_POSITIVE.toInt() -> "unsigned integer"
        HEADER_NEGATIVE.toInt() -> "negative integer"
        else -> "<unknown>"
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


private class CborMapReader(cbor: Cbor, decoder: CborParser) : CborListReader(cbor, decoder) {
    override fun skipBeginToken(objectTags: ULongArray?) =
        setSize(parser.startMap(tags?.let { if (objectTags == null) it else ulongArrayOf(*it, *objectTags) }
            ?: objectTags) * 2)
}

private open class CborListReader(cbor: Cbor, decoder: CborParser) : CborReader(cbor, decoder) {
    private var ind = 0

    override fun skipBeginToken(objectTags: ULongArray?) =
        setSize(parser.startArray(tags?.let { if (objectTags == null) it else ulongArrayOf(*it, *objectTags) }
            ?: objectTags))

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (!finiteMode && parser.isEnd() || (finiteMode && ind >= size)) CompositeDecoder.DECODE_DONE else
            ind++.also {
                decodeByteArrayAsByteString = descriptor.isByteString(it)
            }
    }
}


private val normalizeBaseBits = SINGLE_PRECISION_NORMALIZE_BASE.toBits()


/*
 * For details about half-precision floating-point numbers see https://tools.ietf.org/html/rfc8949#name-half-precision
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
