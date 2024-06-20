/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.cbor.*
import kotlinx.serialization.cbor.internal.CborWriter.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import kotlin.experimental.*

private const val FALSE = 0xf4
private const val TRUE = 0xf5
private const val NULL = 0xf6
private const val EMPTY_MAP = 0xa0

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

//value classes are only inlined on the JVM, so we use a typealias and extensions instead
private typealias Stack = MutableList<Token>

private fun Stack(): Stack = mutableListOf()
private fun Stack.push(value: Token) = add(value)
private fun Stack.pop() = removeLast()
private fun Stack.peek() = last()

// Writes class as map [fieldName, fieldValue]
internal open class CborWriter(
    private val cbor: Cbor,
    protected val output: ByteArrayOutput,
) : AbstractEncoder() {


    private var encodeByteArrayAsByteString = false

    /**
     * A single token to be encoded.
     *
     * Since encoding requires two passes (to count all actually to-be-encoded children of any structure), a token
     * contains references to function which will later on actually write to a [ByteArrayOutput] to avoid pre-allocation
     * of fire-and-forget [ByteArray]s
     */
    inner class Token(
        /**
         * Serial descriptor for future reference
         */
        var descriptor: SerialDescriptor?,

        /**
         * reference to function container responsible for actual data encoding
         */
        var data: DeferredEncoding?,

        /**
         * number of children (if this is a structure)
         */
        var numChildren: Int = -1,

        /**
         * [Preamble] of this token containing metadata, possibly referencing tags and labels for deferred writing to a [ByteArrayOutput]
         */
        var preamble: Preamble? = null,
    ) {

        /**
         * writes this token's [preamble] to [output] and invokes the [data] deferred encoding function, also writing to [output]
         */
        fun encode() {
            preamble?.let { it.encode(output) }

            //byteStrings are encoded into the data already, as are primitives
            data?.let { it.encode(output) }
        }

        override fun toString(): String {
            return "(${descriptor?.serialName}:${descriptor?.kind}, data: $data, numChildren: $numChildren)"
        }
    }

    inner class Preamble(
        private val parentDescriptor: SerialDescriptor?,
        private val index: Int,
        private val label: Long?,
        private val name: String?
    ) {

        fun encode(output: ByteArrayOutput): ByteArrayOutput {

            if (parentDescriptor != null) {
                if (!parentDescriptor.hasArrayTag()) {
                    if (cbor.writeKeyTags) parentDescriptor.getKeyTags(index)?.forEach { output.encodeTag(it) }

                    if ((parentDescriptor.kind !is StructureKind.LIST) && (parentDescriptor.kind !is StructureKind.MAP) && (parentDescriptor.kind !is PolymorphicKind)) {
                        //indices are put into the name field. we don't want to write those, as it would result in double writes
                        if (cbor.preferCborLabelsOverNames && label != null) {
                            output.encodeNumber(label)
                        } else if (name != null) {
                            output.encodeString(name)
                        }
                    }
                }
            }

            if (cbor.writeValueTags) {
                parentDescriptor?.getValueTags(index)?.forEach { output.encodeTag(it) }
            }
            return output
        }
    }


    /**
     * Functional interface for deferred writing to a [ByteArrayOutput]
     */
    fun interface DeferredEncoding {
        fun encode(output: ByteArrayOutput)
    }

    /**
     * 2-Tuple containing data to be written to a [ByteArrayOutput] and a reference to the encoding function responsible for doing so
     */
    class DeferredEncode<T>(
        private val data: T,
        private val encodingFunction: ByteArrayOutput.(obj: T) -> Unit
    ) : DeferredEncoding {
        override fun encode(output: ByteArrayOutput) {
            output.apply { encodingFunction(data) }
        }
    }

    /**
     * Contains a reference to an unparameterised encoding function, responsible for writing a constant to a [ByteArrayOutput]
     */
    class DeferredConstant(
        private val encodingFunction: ByteArrayOutput.() -> Unit
    ) : DeferredEncoding {
        override fun encode(output: ByteArrayOutput) {
            output.apply { encodingFunction() }
        }
    }

    //currently processed token
    private var currentToken = Token(null, null, -1, null)

    /**
     * Encoding requires two passes to support definite length encoding.
     *
     * Tokens are pushed to the stack when a structure starts, and popped when a structure ends. In between the number to children, which **actually** need to be written, are counted
     *
     */
    private val structureStack = Stack()

    /**
     * Encoding requires two passes to support definite length encoding.
     *
     * Tokens are added to the encoding queue in the order they need to be written. For definite length encoding, this is a no-brainer as the queue is populated on [encodeElement]
     * For indefinite length encoding, additional tokens are created and added to the encoding queue every time [endStructure] is called.
     *
     * Encoding starts with the first token, so it is added right away
     */
    private val encodingQueue = mutableListOf(currentToken)

    /**
     * To be invoked after the first pass (i.e. processing all tokens, counting all children of structures etc.)
     *
     * Kicks off the second pass, where data is actually encoded  and written to [output]
     */
    fun encode() {
        encodingQueue.forEach { it.encode() }
    }


    override val serializersModule: SerializersModule
        get() = cbor.serializersModule


    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {

        if ((encodeByteArrayAsByteString || cbor.alwaysUseByteString)
            && serializer.descriptor == ByteArraySerializer().descriptor
        ) {
            currentToken.data = DeferredEncode(value as ByteArray, ByteArrayOutput::encodeByteString)
        } else {
            encodeByteArrayAsByteString = encodeByteArrayAsByteString || serializer.descriptor.isInlineByteString()
            super.encodeSerializableValue(serializer, value)
        }
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = cbor.encodeDefaults

    @OptIn(ExperimentalSerializationApi::class)
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        currentToken.numChildren = 0

        //if we start encoding structures directly (i.e. map, class, list, array)
        if (encodingQueue.size == 1) {
            currentToken.descriptor = descriptor
        }

        structureStack.push(currentToken)
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        val beginToken = structureStack.pop()

        val accumulator = mutableListOf<DeferredEncoding>()

        //If this nullpointers, we have a structural problem anyhow
        val beginDescriptor = beginToken.descriptor!!
        val numChildren = beginToken.numChildren

        if (beginDescriptor.hasArrayTag()) {
            beginDescriptor.getArrayTags()?.forEach { accumulator += DeferredEncode(it, ByteArrayOutput::encodeTag) }
            accumulator += if (cbor.writeDefiniteLengths) DeferredEncode(
                numChildren.toULong(),
                ByteArrayOutput::startArray
            )
            else DeferredConstant(ByteArrayOutput::startArray)
        } else {
            when (beginDescriptor.kind) {
                StructureKind.LIST, is PolymorphicKind -> {
                    accumulator += if (cbor.writeDefiniteLengths) DeferredEncode(
                        numChildren.toULong(),
                        ByteArrayOutput::startArray
                    )
                    else DeferredConstant(ByteArrayOutput::startArray)
                }

                is StructureKind.MAP -> {
                    accumulator += if (cbor.writeDefiniteLengths) DeferredEncode(
                        (numChildren / 2).toULong(),
                        ByteArrayOutput::startMap
                    )
                    else DeferredConstant(ByteArrayOutput::startMap)
                }

                else -> {
                    accumulator += if (cbor.writeDefiniteLengths) DeferredEncode(
                        (numChildren).toULong(),
                        ByteArrayOutput::startMap
                    )
                    else DeferredConstant(ByteArrayOutput::startMap)
                }
            }
        }
        beginToken.data = DeferredEncoding {
            accumulator.forEach { it.encode(output) }
        }


        if (!cbor.writeDefiniteLengths) {
            Token(descriptor = descriptor, data = DeferredEncode(BREAK, ByteArrayOutput::write)).also {
                encodingQueue += it
                currentToken = it
            }
        }
    }


    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        val parent = structureStack.peek()
        val name = descriptor.getElementName(index)
        val label = descriptor.getCborLabel(index)

        val preamble = Preamble(parent.descriptor, index, label, name)

        encodeByteArrayAsByteString = descriptor.isByteString(index)
        Token(
            descriptor = descriptor.getElementDescriptor(index),
            data = null,
            preamble = preamble
        ).also {
            encodingQueue += it
            currentToken = it
        }
        structureStack.peek().numChildren += 1
        return true
    }


    //If any of the following functions are called for serializing raw primitives (i.e. something other than a class,
    // list, map or array, no children exist and the root node needs the data
    override fun encodeString(value: String) {
        currentToken.data = DeferredEncode(value, ByteArrayOutput::encodeString)
    }


    override fun encodeFloat(value: Float) {
        currentToken.data = DeferredEncode(value, ByteArrayOutput::encodeFloat)
    }


    override fun encodeDouble(value: Double) {
        currentToken.data = DeferredEncode(value, ByteArrayOutput::encodeDouble)
    }


    override fun encodeChar(value: Char) {
        currentToken.data = DeferredEncode(value.code.toLong(), ByteArrayOutput::encodeNumber)
    }


    override fun encodeByte(value: Byte) {
        currentToken.data = DeferredEncode(value.toLong(), ByteArrayOutput::encodeNumber)
    }


    override fun encodeShort(value: Short) {
        currentToken.data = DeferredEncode(value.toLong(), ByteArrayOutput::encodeNumber)
    }


    override fun encodeInt(value: Int) {
        currentToken.data = DeferredEncode(value.toLong(), ByteArrayOutput::encodeNumber)
    }


    override fun encodeLong(value: Long) {
        currentToken.data = DeferredEncode(value, ByteArrayOutput::encodeNumber)
    }


    override fun encodeBoolean(value: Boolean) {
        currentToken.data = DeferredEncode(value, ByteArrayOutput::encodeBoolean)
    }


    override fun encodeNull() {
        currentToken.data =
            if (currentToken.descriptor?.kind == StructureKind.CLASS) DeferredConstant(ByteArrayOutput::encodeEmptyMap)
            else DeferredConstant(ByteArrayOutput::encodeNull)

    }


    @OptIn(ExperimentalSerializationApi::class) // KT-46731
    override fun encodeEnum(
        enumDescriptor: SerialDescriptor,
        index: Int
    ) {
        currentToken.data = DeferredEncode(enumDescriptor.getElementName(index), ByteArrayOutput::encodeString)
    }


}


private fun ByteArrayOutput.startArray() = write(BEGIN_ARRAY)

private fun ByteArrayOutput.startArray(size: ULong) {
    val encodedNumber = composePositive(size)
    encodedNumber[0] = encodedNumber[0] or HEADER_ARRAY.toUByte().toByte()
    encodedNumber.forEach { this.writeByte(it.toUByte().toInt()) }
}

private fun ByteArrayOutput.startMap() = write(BEGIN_MAP)

private fun ByteArrayOutput.startMap(size: ULong) {
    val encodedNumber = composePositive(size)
    encodedNumber[0] = encodedNumber[0] or HEADER_MAP.toUByte().toByte()
    encodedNumber.forEach { this.writeByte(it.toUByte().toInt()) }
}

private fun ByteArrayOutput.encodeTag(tag: ULong) {
    val encodedTag = composePositive(tag)
    encodedTag[0] = encodedTag[0] or HEADER_TAG.toUByte().toByte()
    encodedTag.forEach { this.writeByte(it.toUByte().toInt()) }
}

internal fun ByteArrayOutput.end() = write(BREAK)

internal fun ByteArrayOutput.encodeNull() = write(NULL)

internal fun ByteArrayOutput.encodeEmptyMap() = write(EMPTY_MAP)

internal fun ByteArrayOutput.writeByte(byteValue: Int) = write(byteValue)

internal fun ByteArrayOutput.pasteBytes(bytes: ByteArray) {
    write(bytes)
}

internal fun ByteArrayOutput.encodeBoolean(value: Boolean) = write(if (value) TRUE else FALSE)

internal fun ByteArrayOutput.encodeNumber(value: Long) = write(composeNumber(value))

internal fun ByteArrayOutput.encodeByteString(data: ByteArray) {
    this.encodeByteArray(data, HEADER_BYTE_STRING)
}

internal fun ByteArrayOutput.encodeString(value: String) {
    this.encodeByteArray(value.encodeToByteArray(), HEADER_STRING)
}

internal fun ByteArrayOutput.encodeByteArray(data: ByteArray, type: Byte) {
    val header = composeNumber(data.size.toLong())
    header[0] = header[0] or type
    write(header)
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

private fun composeNumber(value: Long): ByteArray =
    if (value >= 0) composePositive(value.toULong()) else composeNegative(value)

private fun composePositive(value: ULong): ByteArray = when (value) {
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


private fun SerialDescriptor.isInlineByteString(): Boolean {
    // inline item classes should only have 1 item
    return isInline && isByteString(0)
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getValueTags(index: Int): ULongArray? {
    return kotlin.runCatching { (getElementAnnotations(index).find { it is ValueTags } as ValueTags?)?.tags }
        .getOrNull()
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getKeyTags(index: Int): ULongArray? {
    return kotlin.runCatching { (getElementAnnotations(index).find { it is KeyTags } as KeyTags?)?.tags }.getOrNull()
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getCborLabel(index: Int): Long? {
    return kotlin.runCatching { getElementAnnotations(index).filterIsInstance<CborLabel>().firstOrNull()?.label }
        .getOrNull()
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getElementNameForCborLabel(label: Long): String? {
    return elementNames.firstOrNull { getCborLabel(getElementIndex(it)) == label }
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getArrayTags(): Collection<ULong>? {
    return annotations.filterIsInstance<CborArray>().firstOrNull()?.tag?.map { it.tag }
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
