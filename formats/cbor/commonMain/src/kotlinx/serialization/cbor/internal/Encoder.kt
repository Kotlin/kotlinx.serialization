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
import kotlin.experimental.*


//value classes are only inlined on the JVM, so we use a typealias and extensions instead
private typealias Stack = MutableList<CborWriter.Token>

private fun Stack(): Stack = mutableListOf()
private fun Stack.push(value: CborWriter.Token) = add(value)
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


@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.getArrayTags(): Collection<ULong>? {
    return annotations.filterIsInstance<CborArray>().firstOrNull()?.tag?.map { it.tag }
}