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

package kotlinx.serialization.protobuf

import kotlinx.io.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.protobuf.ProtoBuf.Varint.decodeSignedVarintInt
import kotlinx.serialization.protobuf.ProtoBuf.Varint.decodeSignedVarintLong
import kotlinx.serialization.protobuf.ProtoBuf.Varint.decodeVarint
import kotlinx.serialization.protobuf.ProtoBuf.Varint.encodeVarint
import kotlin.reflect.KClass

enum class ProtoNumberType {
    DEFAULT, SIGNED, FIXED
}

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class ProtoType(val type: ProtoNumberType)

typealias ProtoDesc = Pair<Int, ProtoNumberType>

class ProtoBuf(val context: SerialContext? = null) {

    internal open inner class ProtobufWriter(val encoder: ProtobufEncoder) : TaggedOutput<ProtoDesc>() {

        init {
            context = this@ProtoBuf.context
        }

        override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput = when (desc.kind) {
            KSerialClassKind.LIST, KSerialClassKind.MAP, KSerialClassKind.SET -> RepeatedWriter(encoder, currentTag)
            KSerialClassKind.CLASS, KSerialClassKind.OBJECT, KSerialClassKind.SEALED, KSerialClassKind.POLYMORPHIC -> ObjectWriter(currentTagOrNull, encoder)
            KSerialClassKind.ENTRY -> MapEntryWriter(currentTagOrNull, encoder)
            else -> throw SerializationException("Primitives are not supported at top-level")
        }

        override fun writeTaggedInt(tag: ProtoDesc, value: Int) = encoder.writeInt(value, tag.first, tag.second)
        override fun writeTaggedByte(tag: ProtoDesc, value: Byte) = encoder.writeInt(value.toInt(), tag.first, tag.second)
        override fun writeTaggedShort(tag: ProtoDesc, value: Short) = encoder.writeInt(value.toInt(), tag.first, tag.second)
        override fun writeTaggedLong(tag: ProtoDesc, value: Long) = encoder.writeLong(value, tag.first, tag.second)
        override fun writeTaggedFloat(tag: ProtoDesc, value: Float) = encoder.writeFloat(value, tag.first)
        override fun writeTaggedDouble(tag: ProtoDesc, value: Double) = encoder.writeDouble(value, tag.first)
        override fun writeTaggedBoolean(tag: ProtoDesc, value: Boolean) = encoder.writeInt(if (value) 1 else 0, tag.first, ProtoNumberType.DEFAULT)
        override fun writeTaggedChar(tag: ProtoDesc, value: Char) = encoder.writeInt(value.toInt(), tag.first, tag.second)
        override fun writeTaggedString(tag: ProtoDesc, value: String) = encoder.writeString(value, tag.first)
        override fun <E : Enum<E>> writeTaggedEnum(tag: ProtoDesc, enumClass: KClass<E>, value: E) = encoder.writeInt(value.ordinal, tag.first, ProtoNumberType.DEFAULT)

        override fun KSerialClassDesc.getTag(index: Int) = this.getProtoDesc(index)
    }

    internal inner open class ObjectWriter(val parentTag: ProtoDesc?, private val parentEncoder: ProtobufEncoder, private val stream: ByteArrayOutputStream = ByteArrayOutputStream()) : ProtobufWriter(ProtobufEncoder(stream)) {
        override fun writeFinished(desc: KSerialClassDesc) {
            if (parentTag != null) {
                parentEncoder.writeObject(stream.toByteArray(), parentTag.first)
            } else {
                parentEncoder.out.write(stream.toByteArray())
            }
        }
    }

    internal inner class MapEntryWriter(parentTag: ProtoDesc?, parentEncoder: ProtobufEncoder): ObjectWriter(parentTag, parentEncoder) {
        override fun KSerialClassDesc.getTag(index: Int): ProtoDesc =
                if (index == 0) 1 to (parentTag?.second ?: ProtoNumberType.DEFAULT)
                else 2 to (parentTag?.second ?: ProtoNumberType.DEFAULT)
    }

    internal inner class RepeatedWriter(encoder: ProtobufEncoder, val curTag: ProtoDesc) : ProtobufWriter(encoder) {
        override fun KSerialClassDesc.getTag(index: Int) = curTag

        override fun shouldWriteElement(desc: KSerialClassDesc, tag: ProtoDesc, index: Int): Boolean = index != SIZE_INDEX
    }

    internal class ProtobufEncoder(val out: ByteArrayOutputStream) {

        fun writeObject(bytes: ByteArray, tag: Int) {
            val header = encode32((tag shl 3) or SIZE_DELIMITED)
            val len = encode32(bytes.size)
            out.write(header)
            out.write(len)
            out.write(bytes)
        }

        fun writeInt(value: Int, tag: Int, format: ProtoNumberType) {
            val wireType = if (format == ProtoNumberType.FIXED) i32 else VARINT
            val header = encode32((tag shl 3) or wireType)
            val content = encode32(value, format)
            out.write(header)
            out.write(content)
        }

        fun writeLong(value: Long, tag: Int, format: ProtoNumberType) {
            val wireType = if (format == ProtoNumberType.FIXED) i64 else VARINT
            val header = encode32((tag shl 3) or wireType)
            val content = encode64(value, format)
            out.write(header)
            out.write(content)
        }

        fun writeString(value: String, tag: Int) {
            val bytes = value.toUtf8Bytes()
            writeObject(bytes, tag)
        }

        fun writeDouble(value: Double, tag: Int) {
            val header = encode32((tag shl 3) or i64)
            val content = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array()
            out.write(header)
            out.write(content)
        }

        fun writeFloat(value: Float, tag: Int) {
            val header = encode32((tag shl 3) or i32)
            val content = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array()
            out.write(header)
            out.write(content)
        }

        private fun encode32(number: Int, format: ProtoNumberType = ProtoNumberType.DEFAULT): ByteArray =
                when (format) {
                    ProtoNumberType.FIXED -> ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(number).array()
                    ProtoNumberType.DEFAULT -> encodeVarint(number.toLong())
                    ProtoNumberType.SIGNED -> encodeVarint(((number shl 1) xor (number shr 31)))
                }


        private fun encode64(number: Long, format: ProtoNumberType = ProtoNumberType.DEFAULT): ByteArray =
                when (format) {
                    ProtoNumberType.FIXED -> ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(number).array()
                    ProtoNumberType.DEFAULT -> encodeVarint(number)
                    ProtoNumberType.SIGNED -> encodeVarint((number shl 1) xor (number shr 63))
                }
    }

    private open inner class ProtobufReader(val decoder: ProtobufDecoder) : TaggedInput<ProtoDesc>() {

        init {
            context = this@ProtoBuf.context
        }

        private val indexByTag: MutableMap<Int, Int> = mutableMapOf()
        private fun findIndexByTag(desc: KSerialClassDesc, serialId: Int): Int {
            return (0 until desc.associatedFieldsCount).firstOrNull { desc.getTag(it).first == serialId }
                    ?: -1
        }

        override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput = when (desc.kind) {
            KSerialClassKind.LIST, KSerialClassKind.MAP, KSerialClassKind.SET -> RepeatedReader(decoder, currentTag)
            KSerialClassKind.CLASS, KSerialClassKind.OBJECT, KSerialClassKind.SEALED, KSerialClassKind.POLYMORPHIC ->
                ProtobufReader(makeDelimited(decoder, currentTagOrNull))
            KSerialClassKind.ENTRY -> MapEntryReader(makeDelimited(decoder, currentTagOrNull), currentTagOrNull)
            else -> throw SerializationException("Primitives are not supported at top-level")
        }

        override fun readTaggedBoolean(tag: ProtoDesc): Boolean = when (decoder.nextInt(ProtoNumberType.DEFAULT)) {
            0 -> false
            1 -> true
            else -> throw ProtobufDecodingException("Expected boolean value")
        }

        override fun readTaggedByte(tag: ProtoDesc): Byte = decoder.nextInt(tag.second).toByte()
        override fun readTaggedShort(tag: ProtoDesc): Short = decoder.nextInt(tag.second).toShort()
        override fun readTaggedInt(tag: ProtoDesc): Int = decoder.nextInt(tag.second)
        override fun readTaggedLong(tag: ProtoDesc): Long = decoder.nextLong(tag.second)
        override fun readTaggedFloat(tag: ProtoDesc): Float = decoder.nextFloat()
        override fun readTaggedDouble(tag: ProtoDesc): Double = decoder.nextDouble()
        override fun readTaggedChar(tag: ProtoDesc): Char = decoder.nextInt(tag.second).toChar()
        override fun readTaggedString(tag: ProtoDesc): String = decoder.nextString()
        override fun <E : Enum<E>> readTaggedEnum(tag: ProtoDesc, enumClass: KClass<E>): E = enumFromOrdinal(enumClass, decoder.nextInt(ProtoNumberType.DEFAULT))

        override fun KSerialClassDesc.getTag(index: Int) = this.getProtoDesc(index)

        override fun readElement(desc: KSerialClassDesc): Int {
            while (true) {
                if (decoder.curId == -1) // EOF
                    return READ_DONE
                val ind = indexByTag.getOrPut(decoder.curId, { findIndexByTag(desc, decoder.curId) })
                if (ind == -1) // not found
                    decoder.skipElement()
                else return ind
            }
        }
    }

    private inner class RepeatedReader(decoder: ProtobufDecoder, val targetTag: ProtoDesc) : ProtobufReader(decoder) {
        private var ind = 0

        override fun readElement(desc: KSerialClassDesc) = if (decoder.curId == targetTag.first) ++ind else READ_DONE
        override fun KSerialClassDesc.getTag(index: Int): ProtoDesc = targetTag
    }

    private inner class MapEntryReader(decoder: ProtobufDecoder, val parentTag: ProtoDesc?): ProtobufReader(decoder) {
        override fun KSerialClassDesc.getTag(index: Int): ProtoDesc =
                if (index == 0) 1 to (parentTag?.second ?: ProtoNumberType.DEFAULT)
                else 2 to (parentTag?.second ?: ProtoNumberType.DEFAULT)
    }

    internal class ProtobufDecoder(val inp: ByteArrayInputStream) {
        val curId
            get() = curTag.first
        private var curTag: Pair<Int, Int> = -1 to -1

        init {
            readTag()
        }

        private fun readTag(): Pair<Int, Int> {
            val header = decode32(eofAllowed = true)
            curTag = if (header == -1) {
                -1 to -1
            } else {
                val wireType = header and 0b111
                val fieldId = header ushr 3
                fieldId to wireType
            }
            return curTag
        }

        fun skipElement() {
            when(curTag.second) {
                VARINT -> nextInt(ProtoNumberType.DEFAULT)
                i64 -> nextLong(ProtoNumberType.FIXED)
                SIZE_DELIMITED -> nextObject()
                i32 -> nextInt(ProtoNumberType.FIXED)
            }
            readTag()
        }

        fun nextObject(): ByteArray {
            if (curTag.second != SIZE_DELIMITED) throw ProtobufDecodingException("Unexpected wire type: ${curTag.second}")
            val len = decode32()
            check(len >= 0)
            val ans = inp.readExactNBytes(len)
            readTag()
            return ans
        }

        fun nextInt(format: ProtoNumberType): Int {
            val wireType = if (format == ProtoNumberType.FIXED) i32 else VARINT
            if (wireType != curTag.second) throw ProtobufDecodingException("Unexpected wire type: ${curTag.second}")
            val ans = decode32(format)
            readTag()
            return ans
        }

        fun nextLong(format: ProtoNumberType): Long {
            val wireType = if (format == ProtoNumberType.FIXED) i64 else VARINT
            if (wireType != curTag.second) throw ProtobufDecodingException("Unexpected wire type: ${curTag.second}")
            val ans = decode64(format)
            readTag()
            return ans
        }

        fun nextFloat(): Float {
            if (curTag.second != i32) throw ProtobufDecodingException("Unexpected wire type: ${curTag.second}")
            val ans = inp.readToByteBuffer(4).order(ByteOrder.LITTLE_ENDIAN).getFloat()
            readTag()
            return ans
        }

        fun nextDouble(): Double {
            if (curTag.second != i64) throw ProtobufDecodingException("Unexpected wire type: ${curTag.second}")
            val ans = inp.readToByteBuffer(8).order(ByteOrder.LITTLE_ENDIAN).getDouble()
            readTag()
            return ans
        }

        fun nextString(): String {
            val bytes = this.nextObject()
            return stringFromUtf8Bytes(bytes)
        }

        private fun decode32(format: ProtoNumberType = ProtoNumberType.DEFAULT, eofAllowed: Boolean = false): Int = when (format) {
            ProtoNumberType.DEFAULT -> decodeVarint(inp, 64, eofAllowed).toInt()
            ProtoNumberType.SIGNED -> decodeSignedVarintInt(inp)
            ProtoNumberType.FIXED -> inp.readToByteBuffer(4).order(ByteOrder.LITTLE_ENDIAN).getInt()
        }

        private fun decode64(format: ProtoNumberType = ProtoNumberType.DEFAULT): Long = when (format) {
            ProtoNumberType.DEFAULT -> decodeVarint(inp, 64)
            ProtoNumberType.SIGNED -> decodeSignedVarintLong(inp)
            ProtoNumberType.FIXED -> inp.readToByteBuffer(8).order(ByteOrder.LITTLE_ENDIAN).getLong()
        }
    }

    /**
     *  Source for all varint operations:
     *  https://github.com/addthis/stream-lib/blob/master/src/main/java/com/clearspring/analytics/util/Varint.java
     */
    internal object Varint {
        internal fun encodeVarint(inp: Int): ByteArray {
            var value = inp
            val byteArrayList = ByteArray(10)
            var i = 0
            while (value and 0xFFFFFF80.toInt() != 0) {
                byteArrayList[i++] = ((value and 0x7F) or 0x80).toByte()
                value = value ushr 7
            }
            byteArrayList[i] = (value and 0x7F).toByte()
            val out = ByteArray(i + 1)
            while (i >= 0) {
                out[i] = byteArrayList[i]
                i--
            }
            return out
        }

        internal fun encodeVarint(inp: Long): ByteArray {
            var value = inp
            val byteArrayList = ByteArray(10)
            var i = 0
            while (value and 0x7FL.inv() != 0L) {
                byteArrayList[i++] = ((value and 0x7F) or 0x80).toByte()
                value = value ushr 7
            }
            byteArrayList[i] = (value and 0x7F).toByte()
            val out = ByteArray(i + 1)
            while (i >= 0) {
                out[i] = byteArrayList[i]
                i--
            }
            return out
        }

        internal fun decodeVarint(inp: InputStream, bitLimit: Int = 32, eofOnStartAllowed: Boolean = false): Long {
            var result = 0L
            var shift = 0
            var b: Int
            do {
                if (shift >= bitLimit) {
                    // Out of range
                    throw ProtobufDecodingException("Varint too long")
                }
                // Get 7 bits from next byte
                b = inp.read()
                if (b == -1) {
                    if (eofOnStartAllowed && shift == 0) return -1
                    else throw IOException("Unexpected EOF")
                }
                result = result or (b.toLong() and 0x7FL shl shift)
                shift += 7
            } while (b and 0x80 != 0)
            return result
        }

        internal fun decodeSignedVarintInt(inp: InputStream): Int {
            val raw = decodeVarint(inp, 32).toInt()
            val temp = raw shl 31 shr 31 xor raw shr 1
            // This extra step lets us deal with the largest signed values by treating
            // negative results from read unsigned methods as like unsigned values.
            // Must re-flip the top bit if the original read value had it set.
            return temp xor (raw and (1 shl 31))
        }

        internal fun decodeSignedVarintLong(inp: InputStream): Long {
            val raw = decodeVarint(inp, 64)
            val temp = raw shl 63 shr 63 xor raw shr 1
            // This extra step lets us deal with the largest signed values by treating
            // negative results from read unsigned methods as like unsigned values
            // Must re-flip the top bit if the original read value had it set.
            return temp xor (raw and (1L shl 63))

        }
    }

    companion object {
        // todo: make more memory-efficient
        private fun makeDelimited(decoder: ProtobufDecoder, parentTag: ProtoDesc?): ProtobufDecoder {
            if (parentTag == null) return decoder
            val bytes = decoder.nextObject()
            return ProtobufDecoder(ByteArrayInputStream(bytes))
        }

        private fun KSerialClassDesc.getProtoDesc(index: Int): ProtoDesc {
            val tag = this.getAnnotationsForIndex(index).filterIsInstance<SerialId>().single().id
            val format = this.getAnnotationsForIndex(index).filterIsInstance<ProtoType>().onlySingleOrNull()?.type
                    ?: ProtoNumberType.DEFAULT
            return tag to format
        }

        private const val VARINT = 0
        private const val i64 = 1
        private const val SIZE_DELIMITED = 2
        private const val i32 = 5

        val plain = ProtoBuf()

        fun <T: Any> dump(saver: KSerialSaver<T>, obj: T): ByteArray = plain.dump(saver, obj)
        inline fun <reified T : Any> dump(obj: T): ByteArray = plain.dump(obj)
        inline fun <reified T : Any> dumps(obj: T): String = plain.dumps(obj)

        fun <T: Any> load(loader: KSerialLoader<T>, raw: ByteArray): T  = plain.load(loader, raw)
        inline fun <reified T : Any> load(raw: ByteArray): T = plain.load(raw)
        inline fun <reified T : Any> loads(hex: String): T  = plain.loads(hex)
    }

    fun <T : Any> dump(saver: KSerialSaver<T>, obj: T): ByteArray {
        val output = ByteArrayOutputStream()
        val dumper = ProtobufWriter(ProtobufEncoder(output))
        dumper.write(saver, obj)
        return output.toByteArray()
    }

    inline fun <reified T : Any> dump(obj: T): ByteArray = dump(context.klassSerializer(T::class), obj)
    inline fun <reified T : Any> dumps(obj: T): String = HexConverter.printHexBinary(dump(obj), lowerCase = true)

    fun <T : Any> load(loader: KSerialLoader<T>, raw: ByteArray): T {
        val stream = ByteArrayInputStream(raw)
        val reader = ProtobufReader(ProtobufDecoder(stream))
        return reader.read(loader)
    }

    inline fun <reified T : Any> load(raw: ByteArray): T = load(context.klassSerializer(T::class), raw)
    inline fun <reified T : Any> loads(hex: String): T = load(HexConverter.parseHexBinary(hex))

}

class ProtobufDecodingException(message: String) : SerializationException(message)
