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

internal open class CborReader(override val cbor: Cbor, protected val parser: CborParserInterface) : AbstractDecoder(),
    CborDecoder {

    override fun decodeCborElement(): CborElement =
        when (parser) {
            is CborParser -> CborTreeReader(cbor.configuration, parser).read()
            is StructuredCborParser -> parser.layer.current
        }


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

    protected open fun skipBeginToken(objectTags: ULongArray?) = setSize(parser.startMap(objectTags))

    @OptIn(ExperimentalSerializationApi::class)
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val re = if (descriptor.hasArrayTag()) {
            CborArrayReader(cbor, parser)
        } else when (descriptor.kind) {
            StructureKind.LIST, is PolymorphicKind -> CborArrayReader(cbor, parser)
            StructureKind.MAP -> CborMapReader(cbor, parser)
            else -> CborReader(cbor, parser)
        }
        val objectTags = if (cbor.configuration.verifyObjectTags) descriptor.getObjectTags() else null
        re.skipBeginToken(tags?.let { if (objectTags == null) it else ulongArrayOf(*it, *objectTags) } ?: objectTags)
        return re
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (!finiteMode || parser is StructuredCborParser) parser.end()
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val index = if (cbor.configuration.ignoreUnknownKeys) {
            val knownIndex: Int
            while (true) {
                if (isDone()) return CompositeDecoder.DECODE_DONE
                val (elemName, tags) = decodeElementNameWithTagsLenient(descriptor)
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
        return if (deserializer is CborSerializer) {
            val collectedTags = parser.processTags(tags) ?: EMPTY_TAGS
            deserializer.deserialize(this).also { value ->
                (value as? CborElement)?.tags = collectedTags
            }
        } else if ((decodeByteArrayAsByteString || cbor.configuration.alwaysUseByteString)
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

    private fun nextNumberWithinRange(from: Long, to: Long, type: String): Long {
        val number = parser.nextNumber(tags)
        if (number !in from..to) {
            throw CborDecodingException("Decoded number $number is not within the range for type $type ([$from..$to])")
        }
        return number
    }

    override fun decodeByte() = nextNumberWithinRange(Byte.MIN_VALUE.toLong(), Byte.MAX_VALUE.toLong(), "Byte").toByte()
    override fun decodeShort() = nextNumberWithinRange(Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong(), "Short").toShort()
    override fun decodeChar() =
        nextNumberWithinRange(Char.MIN_VALUE.code.toLong(), Char.MAX_VALUE.code.toLong(), "Char").toInt().toChar()
    override fun decodeInt() = nextNumberWithinRange(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong(), "Int").toInt()
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

internal class CborParser(private val input: ByteArrayInput, private val verifyObjectTags: Boolean) :
    CborParserInterface {
    private var curByteOrEof: Int = -1

    internal val curByte: Int
        get() = curByteOrEof

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

    override fun isNull(): Boolean = with(peekCurByteOrFail()) { this == NULL || this == EMPTY_MAP }

    override fun nextNull(tags: ULongArray?): Nothing? {
        processTags(tags)
        if (isNull()) {
            val _ = readByte()
            return null
        }
        throw CborDecodingException(
            "null value (${NULL.toHexString()}) or empty map (${EMPTY_MAP.toHexString()})",
            peekCurByteOrFail()
        )
    }

    override fun nextBoolean(tags: ULongArray?): Boolean {
        processTags(tags)
        val ans = when (val byte = peekCurByteOrFail()) {
            TRUE -> true
            FALSE -> false
            else -> throw CborDecodingException("boolean value", byte)
        }
        readByte()
        return ans
    }

    override fun startArray(tags: ULongArray?): Int = startSized(tags, BEGIN_ARRAY, HEADER_ARRAY, "array")

    override fun startMap(tags: ULongArray?): Int = startSized(tags, BEGIN_MAP, HEADER_MAP, "map")

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

    override fun isEnd(): Boolean = peekCurByteOrFail() == BREAK

    override fun end() {
        skipByte(BREAK)
    }

    override fun nextByteString(tags: ULongArray?): ByteArray {
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

    override fun nextString(tags: ULongArray?): String = nextTaggedString(tags).first

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
    override fun processTags(tags: ULongArray?): ULongArray? {
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
                    if ((collectedTags.size < it.size) || (collectedTags.subList(0, it.size) != it.asList())) {
                        throw CborDecodingException("CBOR tags $collectedTags do not start with specified tags $it")
                    }
                }
            }
        }
    }

    override fun verifyTagsAndThrow(expected: ULongArray, actual: ULongArray?) {
        if (!expected.contentEquals(actual))
            throw CborDecodingException(
                "CBOR tags ${actual?.contentToString()} do not match expected tags ${expected.contentToString()}"
            )
    }

    /**
     * Used for reading the tags and either string (element name) or number (serial label)
     */
    override fun nextTaggedStringOrNumber(): Triple<String?, Long?, ULongArray?> {
        val collectedTags = processTags(null)
        val majorType = peekCurByteOrFail()
        return if ((majorType and MAJOR_TYPE_MASK) == HEADER_STRING) {
            val arr = readBytes()
            val ans = arr.decodeToString()
            readByte()
            Triple(ans, null, collectedTags)
        } else {
            val res = readUnsignedIntegerIgnoringMajorType { majorType.majorTypeName }
            readByte()
            Triple(null, res, collectedTags)
        }
    }

    override fun nextNumber(tags: ULongArray?): Long {
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
            else -> throw CborDecodingException(
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

    override fun nextFloat(tags: ULongArray?): Float {
        processTags(tags)
        val res = when (val headerByte = peekCurByteOrFail()) {
            NEXT_FLOAT -> Float.fromBits(readInt())
            NEXT_HALF -> floatFromHalfBits(readShort())
            else -> throw CborDecodingException("float header", headerByte)
        }
        readByte()
        return res
    }

    override fun nextDouble(tags: ULongArray?): Double {
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
    override fun skipElement(tags: ULongArray?) {
        val lengthStack = mutableListOf<Int>()

        if (isEof()) throw CborDecodingException("Unexpected EOF while skipping element")
        processTags(tags)

        do {
            if (isEof()) throw CborDecodingException("Unexpected EOF while skipping element")

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
            HEADER_BYTE_STRING, HEADER_STRING, HEADER_ARRAY ->
                readUnsignedIntegerIgnoringMajorType { "${majorType.majorTypeName} length" }.asSizedElementLength(majorType)
            HEADER_MAP ->
                readUnsignedIntegerIgnoringMajorType { "map length" }.asSizedElementLength(majorType, Int.MAX_VALUE / 2) * 2
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

    internal fun nextULong(tags: ULongArray? = null): ULong {
        processTags(tags)
        val res = readUnsignedIntegerIgnoringMajorType { "unsigned integer" }
        readByte()
        return res.toULong()
    }

    internal fun nextTag(): ULong {
        val header = peekCurByteOrFail()
        if ((header and MAJOR_TYPE_MASK) != HEADER_TAG) {
            throw CborDecodingException("start of tag", header)
        }
        val tag = readUnsignedIntegerIgnoringMajorType { "tag" }.toULong()
        readByte()
        return tag
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

/**
 * Iterator that keeps a reference to the current element and allows peeking at the next element.
 * Works for single elements (where current is directly set to the element) and for collections (where current
 * will be first set after `startMap` or `startArray`
 */
internal class PeekingIterator private constructor(
    internal val isStructure: Boolean,
    private val iter: ListIterator<CborElement>
) : Iterator<CborElement> by iter {

    lateinit var current: CborElement
        private set

    override fun next(): CborElement = iter.next().also { current = it }

    fun peek() = if (hasNext()) {
        val next = iter.next()
        val _ = iter.previous()
        next
    } else null

    companion object {
        operator fun invoke(single: CborElement): PeekingIterator =
            PeekingIterator(false, listOf(single).listIterator()).also { val _ = it.next() }

        operator fun invoke(iter: ListIterator<CborElement>): PeekingIterator =
            PeekingIterator(true, iter)
    }
}

/**
 * CBOR parser that operates on [CborElement] instead of bytes. Closely mirrors the behaviour of [CborParser], so the
 * [CborDecoder] can remain largely unchanged.
 */
internal class StructuredCborParser(internal val element: CborElement, private val verifyObjectTags: Boolean) :
    CborParserInterface {

    internal var layer: PeekingIterator = PeekingIterator(element)
        private set


    private val layerStack = ArrayDeque<PeekingIterator>()

    // map needs special treatment because keys and values are laid out as a list alternating between key and value to
    // mirror the byte-layout of a cbor map.
    override fun isNull() =
        if (layer.isStructure) layer.peek().let {
            it is CborNull ||
                /*THIS IS NOT CBOR-COMPLIANT but KxS-proprietary handling of nullable classes*/
                (it is CborMap && it.isEmpty())
        } else layer.current is CborNull

    override fun isEnd() = !layer.hasNext()

    override fun end() {
        // Reset iterators when ending a structure
        layer = layerStack.removeLast()
    }

    override fun startArray(tags: ULongArray?): Int {
        processTags(tags)
        if (layer.current !is CborArray) {
            throw CborDecodingException("Expected array, got ${layer.current::class.simpleName}")
        }
        layerStack += layer
        val list = layer.current as CborArray
        layer = PeekingIterator(list.listIterator())
        return list.size //we could just return -1 and let the current layer run out of elements to never run into inconsistencies
        // if we do keep it like this, any inconsistencies serve as a canary for implementation bugs
    }

    override fun startMap(tags: ULongArray?): Int {
        processTags(tags)
        if (layer.current !is CborMap) {
            throw CborDecodingException("Expected map, got ${layer.current::class.simpleName}")
        }
        layerStack += layer

        val map = layer.current as CborMap
        // zip key, value, key, value, ... pairs to mirror byte-layout of CBOR map, so decoding this here works the same
        // as decoding from bytes
        layer = PeekingIterator(map.entries.flatMap { listOf(it.key, it.value) }.listIterator())
        return map.size//we could just return -1 and let the current layer run out of elements to never run into inconsistencies
        // if we do keep it like this, any inconsistencies serve as a canary for implementation bugs
    }

    override fun nextNull(tags: ULongArray?): Nothing? {
        processTags(tags)
        if (layer.current !is CborNull) {
            /* THIS IS NOT CBOR-COMPLIANT but KxS-proprietary handling of nullable classes*/
            if (layer.current is CborMap && (layer.current as CborMap).isEmpty())
                return null
            throw CborDecodingException("Expected null, got ${layer.current::class.simpleName}")
        }
        return null
    }

    override fun nextBoolean(tags: ULongArray?): Boolean {
        processTags(tags)
        if (layer.current !is CborBoolean) {
            throw CborDecodingException("Expected boolean, got ${layer.current::class.simpleName}")
        }
        return (layer.current as CborBoolean).value
    }

    override fun nextNumber(tags: ULongArray?): Long {
        processTags(tags)
        if (layer.current !is CborInteger) {
            throw CborDecodingException("Expected number, got ${layer.current::class.simpleName}")
        }
        return (layer.current as CborInteger).longOrNull
            ?: throw CborDecodingException("${layer.current} cannot be represented as Long")
    }

    override fun nextString(tags: ULongArray?): String {
        processTags(tags)
        if (layer.current !is CborString) {
            throw CborDecodingException("Expected string, got ${layer.current::class.simpleName}")
        }
        return (layer.current as CborString).value
    }

    override fun nextByteString(tags: ULongArray?): ByteArray {
        processTags(tags)
        if (layer.current !is CborByteString) {
            throw CborDecodingException("Expected byte string, got ${layer.current::class.simpleName}")
        }
        return (layer.current as CborByteString).value
    }

    override fun nextDouble(tags: ULongArray?): Double {
        processTags(tags)
        return when (layer.current) {
            is CborFloat -> (layer.current as CborFloat).value
            else -> throw CborDecodingException("Expected double, got ${layer.current::class.simpleName}")
        }
    }

    override fun nextFloat(tags: ULongArray?): Float {
        return nextDouble(tags).toFloat()
    }

    override fun nextTaggedStringOrNumber(): Triple<String?, Long?, ULongArray?> {
        val tags = processTags(null)

        return when (val key = layer.current) {
            is CborString -> Triple(key.value, null, tags)
            is CborInteger -> Triple(
                null,
                key.longOrNull ?: throw CborDecodingException("$key cannot be represented as Long"),
                tags
            )
            else -> throw CborDecodingException("Expected string or number key, got ${key::class.simpleName}")
        }
    }

    /**
     * Verify the current element's object tags and advance to the next element if inside a list/map.
     * The reason this method mixes two behaviours is that decoding a primitive is invoked on a single element.
     * `decodeElementIndex`, etc. is invoked on an iterable and there are key tags and value tags
     */
    @IgnorableReturnValue
    override fun processTags(tags: ULongArray?): ULongArray? {

        // If we're in a list/map, advance to the next element
        if (layer.hasNext()) {
            val _ = layer.next()
        }
        // if we're at a primitive, we only process tags

        // Store collected tags for verification
        val collectedTags = if (layer.current.tags.isEmpty()) null else layer.current.tags

        // Verify tags if needed
        if (verifyObjectTags) {
            tags?.let {
                verifyTagsAndThrow(it, collectedTags)
            }
        }

        return collectedTags
    }

    override fun verifyTagsAndThrow(expected: ULongArray, actual: ULongArray?) {
        if (!expected.contentEquals(actual)) {
            throw CborDecodingException(
                "CBOR tags ${actual?.contentToString()} do not match expected tags ${expected.contentToString()}"
            )
        }
    }

    override fun skipElement(tags: ULongArray?) {
        // Process tags but don't do anything with the element
        processTags(tags)
    }
}


private class CborMapReader(cbor: Cbor, decoder: CborParserInterface) : CborArrayReader(cbor, decoder) {
    override fun skipBeginToken(objectTags: ULongArray?) =
        setSize(parser.startMap(tags?.let { if (objectTags == null) it else ulongArrayOf(*it, *objectTags) }
            ?: objectTags) * 2)
}

private open class CborArrayReader(cbor: Cbor, decoder: CborParserInterface) : CborReader(cbor, decoder) {
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
