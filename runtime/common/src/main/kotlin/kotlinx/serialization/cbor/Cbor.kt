/*
 * Copyright 2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.cbor

import kotlinx.io.*
import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.context.*
import kotlinx.serialization.internal.*
import kotlin.experimental.or

class Cbor(val updateMode: UpdateMode = UpdateMode.BANNED, val encodeDefaults: Boolean = true): AbstractSerialFormat(), BinaryFormat {
    // Writes map entry as plain [key, value] pair, without bounds.
    private inner class CborEntryWriter(encoder: CborEncoder) : CborWriter(encoder) {
        override fun writeBeginToken() {
            // no-op
        }

        override fun endStructure(desc: SerialDescriptor) {
            // no-op
        }

        override fun encodeElement(desc: SerialDescriptor, index: Int) = true
    }

    // Differs from List only in start byte
    private inner class CborMapWriter(encoder: CborEncoder) : CborListWriter(encoder) {
        override fun writeBeginToken() = encoder.startMap()
    }

    // Writes all elements consequently, except size - CBOR supports maps and arrays of indefinite length
    private open inner class CborListWriter(encoder: CborEncoder) : CborWriter(encoder) {
        override fun writeBeginToken() = encoder.startArray()

        override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean = true
    }

    // Writes class as map [fieldName, fieldValue]
    private open inner class CborWriter(val encoder: CborEncoder) : ElementValueEncoder() {

        init {
            context = this@Cbor.context
        }

        override fun shouldEncodeElementDefault(desc: SerialDescriptor, index: Int): Boolean = encodeDefaults

        protected open fun writeBeginToken() = encoder.startMap()

        //todo: Write size of map or array if known
        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
            val writer = when (desc.kind) {
                StructureKind.LIST -> CborListWriter(encoder)
                StructureKind.MAP -> CborMapWriter(encoder)
                else -> CborWriter(encoder)
            }
            writer.writeBeginToken()
            return writer
        }

        override fun endStructure(desc: SerialDescriptor) = encoder.end()

        override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
            val name = desc.getElementName(index)
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
            enumDescription: EnumDescriptor,
            ordinal: Int
        ) =
            encoder.encodeString(enumDescription.getElementName(ordinal))
    }

    // For details of representation, see https://tools.ietf.org/html/rfc7049#section-2.1
    class CborEncoder(val output: OutputStream) {

        fun startArray() = output.write(BEGIN_ARRAY)
        fun startMap() = output.write(BEGIN_MAP)
        fun end() = output.write(BREAK)

        fun encodeNull() = output.write(NULL)

        fun encodeBoolean(value: Boolean) = output.write(if (value) TRUE else FALSE)

        fun encodeNumber(value: Long) = output.write(composeNumber(value))

        fun encodeString(value: String) {
            val data = value.toUtf8Bytes()
            val header = composeNumber(data.size.toLong())
            header[0] = header[0] or HEADER_STRING
            output.write(header)
            output.write(data)
        }

        fun encodeFloat(value: Float) {
            val data = ByteBuffer.allocate(5)
                    .put(NEXT_FLOAT.toByte())
                    .putFloat(value)
                    .array()
            output.write(data)
        }

        fun encodeDouble(value: Double) {
            val data = ByteBuffer.allocate(9)
                    .put(NEXT_DOUBLE.toByte())
                    .putDouble(value)
                    .array()
            output.write(data)
        }

        private fun composeNumber(value: Long): ByteArray =
                if (value >= 0) composePositive(value) else composeNegative(value)

        private fun composePositive(value: Long): ByteArray = when (value) {
            in 0..23 -> byteArrayOf(value.toByte())
            in 24..Byte.MAX_VALUE -> byteArrayOf(24, value.toByte())
            in Byte.MAX_VALUE + 1..Short.MAX_VALUE -> ByteBuffer.allocate(3).put(25.toByte()).putShort(value.toShort()).array()
            in Short.MAX_VALUE + 1..Int.MAX_VALUE -> ByteBuffer.allocate(5).put(26.toByte()).putInt(value.toInt()).array()
            in (Int.MAX_VALUE.toLong() + 1..Long.MAX_VALUE) -> ByteBuffer.allocate(9).put(27.toByte()).putLong(value).array()
            else -> throw AssertionError("$value should be positive")
        }

        private fun composeNegative(value: Long): ByteArray {
            val aVal = if (value == Long.MIN_VALUE) Long.MAX_VALUE else -1 - value
            val data = composePositive(aVal)
            data[0] = data[0] or HEADER_NEGATIVE
            return data
        }
    }

    private inner class CborEntryReader(decoder: CborDecoder) : CborReader(decoder) {
        private var ind = 0

        override fun skipBeginToken() {
            // no-op
        }

        override fun endStructure(desc: SerialDescriptor) {
            // no-op
        }

        override fun decodeElementIndex(desc: SerialDescriptor) = when (ind++) {
            0 -> 0
            1 -> 1
            else -> READ_DONE
        }
    }

    private inner class CborMapReader(decoder: CborDecoder) : CborListReader(decoder) {
        override fun skipBeginToken() = decoder.startMap()
    }

    private open inner class CborListReader(decoder: CborDecoder) : CborReader(decoder) {
        private var ind = -1
        private var size = -1
        protected var finiteMode = false

        override fun skipBeginToken() {
            val len = decoder.startArray()
            if (len != -1) {
                finiteMode = true
                size = len
            }
        }

        override fun decodeElementIndex(desc: SerialDescriptor) = if (!finiteMode && decoder.isEnd() || (finiteMode && ind >= size - 1)) READ_DONE else ++ind

        override fun endStructure(desc: SerialDescriptor) {
            if (!finiteMode) decoder.end()
        }
    }

    private open inner class CborReader(val decoder: CborDecoder) : ElementValueDecoder() {

        init {
            context = this@Cbor.context
        }

        override val updateMode: UpdateMode
            get() = this@Cbor.updateMode

        protected open fun skipBeginToken() = decoder.startMap()

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            val re = when (desc.kind) {
                StructureKind.LIST -> CborListReader(decoder)
                StructureKind.MAP -> CborMapReader(decoder)
                else -> CborReader(decoder)
            }
            re.skipBeginToken()
            return re
        }

        override fun endStructure(desc: SerialDescriptor) = decoder.end()

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            if (decoder.isEnd()) return READ_DONE
            val elemName = decoder.nextString()
            return desc.getElementIndexOrThrow(elemName)
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

        override fun decodeEnum(enumDescription: EnumDescriptor): Int =
            enumDescription.getElementIndexOrThrow(decoder.nextString())

    }

    class CborDecoder(val input: InputStream) {
        private var curByte: Int = -1

        init {
            readByte()
        }

        private fun readByte(): Int {
            curByte = input.read()
            return curByte
        }

        private fun skipByte(expected: Int) {
            if (curByte != expected) throw CborDecodingException("byte ${HexConverter.toHexString(expected)}", curByte)
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

        fun startArray(): Int {
            if (curByte == BEGIN_ARRAY) {
                skipByte(BEGIN_ARRAY)
                return -1
            }
            if ((curByte and 0b111_00000) != HEADER_ARRAY)
                throw CborDecodingException("start of array", curByte)
            val arrayLen = readNumber().toInt()
            readByte()
            return arrayLen
        }

        fun startMap() = skipByte(BEGIN_MAP)

        fun isEnd() = curByte == BREAK

        fun end() = skipByte(BREAK)

        fun nextString(): String {
            if ((curByte and 0b111_00000) != HEADER_STRING.toInt())
                throw CborDecodingException("start of string", curByte)
            val strLen = readNumber().toInt()
            val arr = input.readExactNBytes(strLen)
            val ans = stringFromUtf8Bytes(arr)
            readByte()
            return ans
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
                if (negative) return -(value + 1).toLong()
                else return value.toLong()
            }
            val buf = input.readToByteBuffer(bytesToRead)
            val res = when (bytesToRead) {
                1 -> buf.getUnsignedByte().toLong()
                2 -> buf.getUnsignedShort().toLong()
                4 -> buf.getUnsignedInt()
                8 -> buf.getLong()
                else -> throw AssertionError()
            }
            return if (negative) -(res + 1)
            else res
        }

        fun nextFloat(): Float {
            if (curByte != NEXT_FLOAT) throw CborDecodingException("float header", curByte)
            val res = input.readToByteBuffer(4).getFloat()
            readByte()
            return res
        }

        fun nextDouble(): Double {
            if (curByte != NEXT_DOUBLE) throw CborDecodingException("double header", curByte)
            val res = input.readToByteBuffer(8).getDouble()
            readByte()
            return res
        }

    }

    companion object: BinaryFormat {

        private const val FALSE = 0xf4
        private const val TRUE = 0xf5
        private const val NULL = 0xf6

        private const val NEXT_FLOAT = 0xfa
        private const val NEXT_DOUBLE = 0xfb

        private const val BEGIN_ARRAY = 0x9f
        private const val BEGIN_MAP = 0xbf
        private const val BREAK = 0xff

        private const val HEADER_STRING: Byte = 0b011_00000
        private const val HEADER_NEGATIVE: Byte = 0b001_00000
        private const val HEADER_ARRAY: Int = 0b100_00000

        val plain = Cbor()

        override fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray = plain.dump(serializer, obj)
        override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T = plain.load(deserializer, bytes)
        override fun install(module: SerialModule) = plain.install(module)
        override val context: SerialContext get() = plain.context
    }

    override fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray {
        val output = ByteArrayOutputStream()
        val dumper = CborWriter(CborEncoder(output))
        dumper.encode(serializer, obj)
        return output.toByteArray()
    }

    override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val stream = ByteArrayInputStream(bytes)
        val reader = CborReader(CborDecoder(stream))
        return reader.decode(deserializer)
    }
}

class CborDecodingException(expected: String, foundByte: Int) :
    SerializationException("Expected $expected, but found ${HexConverter.toHexString(foundByte)}")
