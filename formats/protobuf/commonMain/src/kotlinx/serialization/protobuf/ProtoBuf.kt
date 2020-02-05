/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.io.*
import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.protobuf.ProtoBuf.Varint.decodeSignedVarintInt
import kotlinx.serialization.protobuf.ProtoBuf.Varint.decodeSignedVarintLong
import kotlinx.serialization.protobuf.ProtoBuf.Varint.decodeVarint
import kotlinx.serialization.protobuf.ProtoBuf.Varint.encodeVarint

/**
 * The main entry point to work with ProtoBuf serialization.
 * It is typically used by constructing an application-specific instance, with configured ProtoBuf-specific behaviour
 * ([encodeDefaults] constructor parameter) and, if necessary, registered
 * custom serializers (in [SerialModule] provided by [context] constructor parameter).
 *
 * ## Usage Example
 * Given a ProtoBuf definition with one required field, one optional field and one optional field with a custom default
 * value:
 * ```
 * message MyMessage {
 *     required int32 first = 1;
 *     optional int32 second = 2;
 *     optional int32 third = 3 [default = 42];
 * }
 * ```
 *
 * The corresponding [Serializable] class should match the ProtoBuf definition and should use the same default values:
 * ```
 * @Serializable
 * data class MyMessage(val first: Int, val second: Int = 0, val third: Int = 42)
 *
 * // Serialize to ProtoBuf hex string
 * val encoded = ProtoBuf.dumps(MyMessage.serializer(), MyMessage(15)) // "080f1000182a"
 *
 * // Deserialize from ProtoBuf hex string
 * val decoded = ProtoBuf.loads<MyMessage>(MyMessage.serializer(), encoded) // MyMessage(first=15, second=0, third=42)
 *
 * // Serialize to ProtoBuf bytes (omitting default values)
 * val encoded2 = ProtoBuf(encodeDefaults = false).dump(MyMessage.serializer(), MyMessage(15)) // [0x08, 0x0f]
 *
 * // Deserialize ProtoBuf bytes will use default values of the MyMessage class
 * val decoded2 = ProtoBuf.load<MyMessage>(MyMessage.serializer(), encoded2) // MyMessage(first=15, second=0, third=42)
 * ```
 *
 * ### Check existence of optional fields
 * Null values can be used as default value for optional fields to implement more complex use-cases that rely on
 * checking if a field was set or not. This requires the use of a custom ProtoBuf instance with
 * `ProtoBuf(encodeDefaults = false)`.
 *
 * ```
 * @Serializable
 * data class MyMessage(val first: Int, private val _second: Int? = null, private val _third: Int? = null) {
 *
 *     val second: Int
 *         get() = _second ?: 0
 *
 *     val third: Int
 *         get() = _third ?: 42
 *
 *     fun hasSecond() = _second != null
 *
 *     fun hasThird() = _third != null
 * }
 *
 * // Serialize to ProtoBuf bytes (encodeDefaults=false is required if null values are used)
 * val encoded = ProtoBuf(encodeDefaults = false).dump(MyMessage(15)) // [0x08, 0x0f]
 *
 * // Deserialize ProtoBuf bytes
 * val decoded = ProtoBuf.load<MyMessage>(MyMessage.serializer(), encoded) // MyMessage(first=15, _second=null, _third=null)
 * decoded.hasSecond()     // false
 * decoded.second          // 0
 * decoded.hasThird()      // false
 * decoded.third           // 42
 *
 * // Serialize to ProtoBuf bytes
 * val encoded2 = ProtoBuf(encodeDefaults = false).dumps(MyMessage.serializer(), MyMessage(15, 0, 0)) // [0x08, 0x0f, 0x10, 0x00, 0x18, 0x00]
 *
 * // Deserialize ProtoBuf bytes
 * val decoded2 = ProtoBuf.loads<MyMessage>(MyMessage.serializer(), encoded2) // MyMessage(first=15, _second=0, _third=0)
 * decoded.hasSecond()     // true
 * decoded.second          // 0
 * decoded.hasThird()      // true
 * decoded.third           // 0
 * ```
 *
 * @param encodeDefaults specifies whether default values are encoded.
 * @param context application-specific [SerialModule] to provide custom serializers.
 */
class ProtoBuf(
        val encodeDefaults: Boolean = true,
        context: SerialModule = EmptyModule
) : AbstractSerialFormat(context), BinaryFormat {

    internal open inner class ProtobufWriter(val encoder: ProtobufEncoder) : TaggedEncoder<ProtoDesc>() {
        public override val context
            get() = this@ProtoBuf.context

        override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = encodeDefaults

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeSerializers: KSerializer<*>): CompositeEncoder = when (descriptor.kind) {
            StructureKind.LIST -> RepeatedWriter(encoder, currentTag)
            StructureKind.CLASS, UnionKind.OBJECT, is PolymorphicKind -> ObjectWriter(currentTagOrNull, encoder)
            StructureKind.MAP -> MapRepeatedWriter(currentTagOrNull, encoder)
            else -> throw SerializationException("Primitives are not supported at top-level")
        }

        override fun encodeTaggedInt(tag: ProtoDesc, value: Int) = encoder.writeInt(value, tag.first, tag.second)
        override fun encodeTaggedByte(tag: ProtoDesc, value: Byte) = encoder.writeInt(value.toInt(), tag.first, tag.second)
        override fun encodeTaggedShort(tag: ProtoDesc, value: Short) = encoder.writeInt(value.toInt(), tag.first, tag.second)
        override fun encodeTaggedLong(tag: ProtoDesc, value: Long) = encoder.writeLong(value, tag.first, tag.second)
        override fun encodeTaggedFloat(tag: ProtoDesc, value: Float) = encoder.writeFloat(value, tag.first)
        override fun encodeTaggedDouble(tag: ProtoDesc, value: Double) = encoder.writeDouble(value, tag.first)
        override fun encodeTaggedBoolean(tag: ProtoDesc, value: Boolean) = encoder.writeInt(if (value) 1 else 0, tag.first, ProtoNumberType.DEFAULT)
        override fun encodeTaggedChar(tag: ProtoDesc, value: Char) = encoder.writeInt(value.toInt(), tag.first, tag.second)
        override fun encodeTaggedString(tag: ProtoDesc, value: String) = encoder.writeString(value, tag.first)
        override fun encodeTaggedEnum(
            tag: ProtoDesc,
            enumDescription: SerialDescriptor,
            ordinal: Int
        ) = encoder.writeInt(
            extractParameters(enumDescription, ordinal, zeroBasedDefault = true).first,
            tag.first,
            ProtoNumberType.DEFAULT
        )

        override fun SerialDescriptor.getTag(index: Int) = this.getProtoDesc(index)

        @Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
        override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) = when {
            // encode maps as collection of map entries, not merged collection of key-values
            serializer.descriptor is MapLikeDescriptor -> {
                val serializer = (serializer as MapLikeSerializer<Any?, Any?, T, *>)
                val mapEntrySerial = MapEntrySerializer(serializer.keySerializer, serializer.valueSerializer)
                HashSetSerializer(mapEntrySerial).serialize(this, (value as Map<*, *>).entries)
            }
            serializer.descriptor == ByteArraySerializer.descriptor -> encoder.writeBytes(value as ByteArray, popTag().first)
            else -> serializer.serialize(this, value)
        }
    }

    internal open inner class ObjectWriter(
        val parentTag: ProtoDesc?, private val parentEncoder: ProtobufEncoder,
        private val stream: ByteArrayOutputStream = ByteArrayOutputStream()
    ) : ProtobufWriter(ProtobufEncoder(stream)) {
        override fun endEncode(desc: SerialDescriptor) {
            if (parentTag != null) {
                parentEncoder.writeBytes(stream.toByteArray(), parentTag.first)
            } else {
                parentEncoder.out.write(stream.toByteArray())
            }
        }
    }

    internal inner class MapRepeatedWriter(parentTag: ProtoDesc?, parentEncoder: ProtobufEncoder): ObjectWriter(parentTag, parentEncoder) {
        override fun SerialDescriptor.getTag(index: Int): ProtoDesc =
                if (index % 2 == 0) 1 to (parentTag?.second ?: ProtoNumberType.DEFAULT)
                else 2 to (parentTag?.second ?: ProtoNumberType.DEFAULT)
    }

    internal inner class RepeatedWriter(encoder: ProtobufEncoder, val curTag: ProtoDesc) : ProtobufWriter(encoder) {
        override fun SerialDescriptor.getTag(index: Int) = curTag
    }

    internal class ProtobufEncoder(val out: ByteArrayOutputStream) {

        fun writeBytes(bytes: ByteArray, tag: Int) {
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
            writeBytes(bytes, tag)
        }

        fun writeDouble(value: Double, tag: Int) {
            val header = encode32((tag shl 3) or i64)
            val content = value.toLittleEndian().toByteArray()
            out.write(header)
            out.write(content)
        }

        fun writeFloat(value: Float, tag: Int) {
            val header = encode32((tag shl 3) or i32)
            val content = value.toLittleEndian().toByteArray()
            out.write(header)
            out.write(content)
        }

        private fun encode32(number: Int, format: ProtoNumberType = ProtoNumberType.DEFAULT): ByteArray =
                when (format) {
                    ProtoNumberType.FIXED -> number.toLittleEndian().toByteArray()
                    ProtoNumberType.DEFAULT -> encodeVarint(number.toLong())
                    ProtoNumberType.SIGNED -> encodeVarint(((number shl 1) xor (number shr 31)))
                }


        private fun encode64(number: Long, format: ProtoNumberType = ProtoNumberType.DEFAULT): ByteArray =
                when (format) {
                    ProtoNumberType.FIXED -> number.toLittleEndian().toByteArray()
                    ProtoNumberType.DEFAULT -> encodeVarint(number)
                    ProtoNumberType.SIGNED -> encodeVarint((number shl 1) xor (number shr 63))
                }
    }

    private open inner class ProtobufReader(val decoder: ProtobufDecoder) : TaggedDecoder<ProtoDesc>() {
        override val context: SerialModule
            get() = this@ProtoBuf.context

        private val indexByTag: MutableMap<Int, Int> = mutableMapOf()
        private fun findIndexByTag(desc: SerialDescriptor, serialId: Int, zeroBasedDefault: Boolean = false): Int =
            (0 until desc.elementsCount).firstOrNull {
                extractParameters(
                    desc,
                    it,
                    zeroBasedDefault
                ).first == serialId
            } ?: -1

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder = when (desc.kind) {
            StructureKind.LIST -> RepeatedReader(decoder, currentTag)
            StructureKind.CLASS, UnionKind.OBJECT, is PolymorphicKind ->
                ProtobufReader(makeDelimited(decoder, currentTagOrNull))
            StructureKind.MAP -> MapEntryReader(makeDelimited(decoder, currentTagOrNull), currentTagOrNull)
            else -> throw SerializationException("Primitives are not supported at top-level")
        }

        override fun decodeTaggedBoolean(tag: ProtoDesc): Boolean = when (val i = decoder.nextInt(ProtoNumberType.DEFAULT)) {
            0 -> false
            1 -> true
            else -> throw ProtobufDecodingException("Expected boolean value (0 or 1), found $i")
        }

        override fun decodeTaggedByte(tag: ProtoDesc): Byte = decoder.nextInt(tag.second).toByte()
        override fun decodeTaggedShort(tag: ProtoDesc): Short = decoder.nextInt(tag.second).toShort()
        override fun decodeTaggedInt(tag: ProtoDesc): Int = decoder.nextInt(tag.second)
        override fun decodeTaggedLong(tag: ProtoDesc): Long = decoder.nextLong(tag.second)
        override fun decodeTaggedFloat(tag: ProtoDesc): Float = decoder.nextFloat()
        override fun decodeTaggedDouble(tag: ProtoDesc): Double = decoder.nextDouble()
        override fun decodeTaggedChar(tag: ProtoDesc): Char = decoder.nextInt(tag.second).toChar()
        override fun decodeTaggedString(tag: ProtoDesc): String = decoder.nextString()
        override fun decodeTaggedEnum(tag: ProtoDesc, enumDescription: SerialDescriptor): Int =
            findIndexByTag(enumDescription, decoder.nextInt(ProtoNumberType.DEFAULT), zeroBasedDefault = true)

        @Suppress("UNCHECKED_CAST")
        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T = when {
            // encode maps as collection of map entries, not merged collection of key-values
            deserializer.descriptor is MapLikeDescriptor -> {
                val serializer = (deserializer as MapLikeSerializer<Any?, Any?, T, *>)
                val mapEntrySerial = MapEntrySerializer(serializer.keySerializer, serializer.valueSerializer)
                val setOfEntries = HashSetSerializer(mapEntrySerial).deserialize(this)
                setOfEntries.associateBy({ it.key }, {it.value}) as T
            }
            deserializer.descriptor == ByteArraySerializer.descriptor -> decoder.nextObject() as T
            else -> deserializer.deserialize(this)
        }

        override fun SerialDescriptor.getTag(index: Int) = this.getProtoDesc(index)

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            while (true) {
                if (decoder.curId == -1) // EOF
                    return READ_DONE
                val ind = indexByTag.getOrPut(decoder.curId) { findIndexByTag(descriptor, decoder.curId) }
                if (ind == -1) // not found
                    decoder.skipElement()
                else return ind
            }
        }
    }

    private inner class RepeatedReader(decoder: ProtobufDecoder, val targetTag: ProtoDesc) : ProtobufReader(decoder) {
        private var ind = -1

        override fun decodeElementIndex(descriptor: SerialDescriptor) =
            if (decoder.curId == targetTag.first) ++ind else READ_DONE

        override fun SerialDescriptor.getTag(index: Int): ProtoDesc = targetTag
    }

    private inner class MapEntryReader(decoder: ProtobufDecoder, val parentTag: ProtoDesc?): ProtobufReader(decoder) {
        override fun SerialDescriptor.getTag(index: Int): ProtoDesc =
                if (index % 2 == 0) 1 to (parentTag?.second ?: ProtoNumberType.DEFAULT)
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
                else -> throw ProtobufDecodingException("Unsupported start group or end group wire type")
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        private inline fun assertWireType(expected: Int) {
            if (curTag.second != expected) throw ProtobufDecodingException("Expected wire type $expected, but found ${curTag.second}")
        }

        fun nextObject(): ByteArray {
            assertWireType(SIZE_DELIMITED)
            val len = decode32()
            check(len >= 0)
            val ans = inp.readExactNBytes(len)
            readTag()
            return ans
        }

        fun nextInt(format: ProtoNumberType): Int {
            val wireType = if (format == ProtoNumberType.FIXED) i32 else VARINT
            assertWireType(wireType)
            val ans = decode32(format)
            readTag()
            return ans
        }

        fun nextLong(format: ProtoNumberType): Long {
            val wireType = if (format == ProtoNumberType.FIXED) i64 else VARINT
            assertWireType(wireType)
            val ans = decode64(format)
            readTag()
            return ans
        }

        fun nextFloat(): Float {
            assertWireType(i32)
            val ans = readIntLittleEndian()
            readTag()
            return Float.fromBits(ans)
        }

        private fun readIntLittleEndian(): Int {
            var result = 0
            for (i in 0..3) {
                val byte = inp.read()
                result = (result shl 8) or byte
            }
            return result.toLittleEndian()
        }

        private fun readLongLittleEndian(): Long {
            var result = 0L
            for (i in 0..7) {
                val byte = inp.read()
                result = (result shl 8) or byte.toLong()
            }
            return result.toLittleEndian()
        }

        fun nextDouble(): Double {
            assertWireType(i64)
            val ans = readLongLittleEndian()
            readTag()
            return Double.fromBits(ans)
        }

        fun nextString(): String {
            val bytes = this.nextObject()
            return stringFromUtf8Bytes(bytes)
        }

        private fun decode32(format: ProtoNumberType = ProtoNumberType.DEFAULT, eofAllowed: Boolean = false): Int = when (format) {
            ProtoNumberType.DEFAULT -> decodeVarint(inp, 64, eofAllowed).toInt()
            ProtoNumberType.SIGNED -> decodeSignedVarintInt(inp)
            ProtoNumberType.FIXED -> readIntLittleEndian()
        }

        private fun decode64(format: ProtoNumberType = ProtoNumberType.DEFAULT): Long = when (format) {
            ProtoNumberType.DEFAULT -> decodeVarint(inp, 64)
            ProtoNumberType.SIGNED -> decodeSignedVarintLong(inp)
            ProtoNumberType.FIXED -> readLongLittleEndian()
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
                    throw ProtobufDecodingException("Varint too long: exceeded $bitLimit bits")
                }
                // Get 7 bits from next byte
                b = inp.read()
                if (b == -1) {
                    if (eofOnStartAllowed && shift == 0) return -1
                    else error("Unexpected EOF")
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

    companion object: BinaryFormat {
        public override val context: SerialModule get() = plain.context

        // todo: make more memory-efficient
        private fun makeDelimited(decoder: ProtobufDecoder, parentTag: ProtoDesc?): ProtobufDecoder {
            if (parentTag == null) return decoder
            val bytes = decoder.nextObject()
            return ProtobufDecoder(ByteArrayInputStream(bytes))
        }

        private fun SerialDescriptor.getProtoDesc(index: Int): ProtoDesc {
            return extractParameters(this, index)
        }

        internal const val VARINT = 0
        internal const val i64 = 1
        internal const val SIZE_DELIMITED = 2
        internal const val i32 = 5

        val plain = ProtoBuf()

        override fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray = plain.dump(serializer, obj)
        override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T = plain.load(deserializer, bytes)
    }

    override fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray {
        val encoder = ByteArrayOutputStream()
        val dumper = ProtobufWriter(ProtobufEncoder(encoder))
        dumper.encode(serializer, obj)
        return encoder.toByteArray()
    }

    override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val stream = ByteArrayInputStream(bytes)
        val reader = ProtobufReader(ProtobufDecoder(stream))
        return reader.decode(deserializer)
    }

}

private fun Short.toLittleEndian(): Short = (((this.toInt() and 0xff) shl 8) or ((this.toInt() and 0xffff) ushr 8)).toShort()

private fun Int.toLittleEndian(): Int =
    ((this and 0xffff).toShort().toLittleEndian().toInt() shl 16) or ((this ushr 16).toShort().toLittleEndian().toInt() and 0xffff)

private fun Long.toLittleEndian(): Long =
    ((this and 0xffffffff).toInt().toLittleEndian().toLong() shl 32) or ((this ushr 32).toInt().toLittleEndian().toLong() and 0xffffffff)

private fun Float.toLittleEndian(): Int = toRawBits().toLittleEndian()

private fun Double.toLittleEndian(): Long = toRawBits().toLittleEndian()

private fun Int.toByteArray(): ByteArray {
    val value = this
    val result = ByteArray(4)
    for (i in 3 downTo 0) {
        result[3 - i] = (value shr i * 8).toByte()
    }
    return result
}

private fun Long.toByteArray(): ByteArray {
    val value = this
    val result = ByteArray(8)
    for (i in 7 downTo 0) {
        result[7 - i] = (value shr i * 8).toByte()
    }
    return result
}
