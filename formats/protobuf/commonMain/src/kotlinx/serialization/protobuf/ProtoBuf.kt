/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.io.*
import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.builtins.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.protobuf.ProtoBuf.Varint.decodeSignedVarintInt
import kotlinx.serialization.protobuf.ProtoBuf.Varint.decodeSignedVarintLong
import kotlin.jvm.*

/**
 * Implements [encoding][dump] and [decoding][load] classes to/from bytes
 * using [Proto2][https://developers.google.com/protocol-buffers/docs/proto] specification.
 * It is typically used by constructing an application-specific instance, with configured specific behaviour
 * and, if necessary, registered custom serializers (in [SerialModule] provided by [context] constructor parameter).
 *
 * ### Correspondence between Protobuf message definitions and Kotlin classes
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
 * ```
 *
 * By default, protobuf fields ids are being assigned to Kotlin properties in incremental order, i.e.
 * the first property in the class has id 1, the second has id 2, and so forth.
 * If you need a more stable order (e.g. to avoid breaking changes when reordering properties),
 * provide custom ids using [ProtoId] annotation.
 *
 * By default, all numbers are encoded using [varint][https://developers.google.com/protocol-buffers/docs/encoding#varints]
 * encoding. This behaviour can be changed via [ProtoNumberType] annotation.
 *
 * ### Known caveats and limitations
 * Lists are represented as repeated fields. Because format spec says that if the list is empty,
 * there are no elements in the stream with such tag, you must explicitly mark any
 * field of list type with default = emptyList(). Same for maps.
 * There's no special support for `oneof` protobuf fields. However, this implementation
 * supports standard kotlinx.serialization's polymorphic and sealed serializers,
 * using their default form (message of serialName: string and other embedded message with actual content).
 *
 * ### Proto3 support
 * This implementation does not support repeated packed fields, so you won't be able to deserialize
 * Proto3 lists. However, other messages could be decoded. You have to remember that since fields in Proto3
 * messages by default are implicitly optional,
 * corresponding Kotlin properties have to be nullable with default value `null`.
 *
 * ### Usage example
 * ```
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
 * Null values can be used as the default value for optional fields to implement more complex use-cases that rely on
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
public class ProtoBuf(
    public val encodeDefaults: Boolean = true,
    override val context: SerialModule = EmptyModule
) : BinaryFormat {

    internal open inner class ProtobufWriter(private val encoder: ProtobufEncoder) : TaggedEncoder<ProtoDesc>() {
        public override val context
            get() = this@ProtoBuf.context

        override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = encodeDefaults

        override fun beginStructure(
            descriptor: SerialDescriptor,
            vararg typeSerializers: KSerializer<*>
        ): CompositeEncoder = when (descriptor.kind) {
            StructureKind.LIST -> RepeatedWriter(encoder, currentTag)
            StructureKind.CLASS, StructureKind.OBJECT, is PolymorphicKind -> ObjectWriter(currentTagOrNull, encoder)
            StructureKind.MAP -> MapRepeatedWriter(currentTagOrNull, encoder)
            else -> throw SerializationException("Primitives are not supported at top-level")
        }

        override fun encodeTaggedInt(tag: ProtoDesc, value: Int) = encoder.writeInt(value, tag.protoId, tag.numberType)
        override fun encodeTaggedByte(tag: ProtoDesc, value: Byte) = encoder.writeInt(value.toInt(), tag.protoId, tag.numberType)
        override fun encodeTaggedShort(tag: ProtoDesc, value: Short) = encoder.writeInt(value.toInt(), tag.protoId, tag.numberType)
        override fun encodeTaggedLong(tag: ProtoDesc, value: Long) = encoder.writeLong(value, tag.protoId, tag.numberType)
        override fun encodeTaggedFloat(tag: ProtoDesc, value: Float) = encoder.writeFloat(value, tag.protoId)
        override fun encodeTaggedDouble(tag: ProtoDesc, value: Double) = encoder.writeDouble(value, tag.protoId)
        override fun encodeTaggedBoolean(tag: ProtoDesc, value: Boolean) = encoder.writeInt(if (value) 1 else 0, tag.protoId, ProtoNumberType.DEFAULT)
        override fun encodeTaggedChar(tag: ProtoDesc, value: Char) = encoder.writeInt(value.toInt(), tag.protoId, tag.numberType)
        override fun encodeTaggedString(tag: ProtoDesc, value: String) = encoder.writeString(value, tag.protoId)
        override fun encodeTaggedEnum(
            tag: ProtoDesc,
            enumDescription: SerialDescriptor,
            ordinal: Int
        ): Unit = encoder.writeInt(
            extractProtoId(enumDescription, ordinal, zeroBasedDefault = true),
            tag.protoId,
            ProtoNumberType.DEFAULT
        )

        override fun SerialDescriptor.getTag(index: Int) = extractParameters(index)

        @Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
        override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) = when {
            serializer is MapLikeSerializer<*, *, *, *> -> {
                serializeMap(serializer as SerializationStrategy<T>, value)
            }
            serializer.descriptor == ByteArraySerializer().descriptor -> encoder.writeBytes(
                value as ByteArray,
                popTag().protoId
            )
            else -> serializer.serialize(this, value)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> serializeMap(serializer: SerializationStrategy<T>, value: T) {
            // encode maps as collection of map entries, not merged collection of key-values
            val casted = (serializer as MapLikeSerializer<Any?, Any?, T, *>)
            val mapEntrySerial = MapEntrySerializer(casted.keySerializer, casted.valueSerializer)
            SetSerializer(mapEntrySerial).serialize(this, (value as Map<*, *>).entries)
        }
    }

    internal open inner class ObjectWriter(
        val parentTag: ProtoDesc?,
        private val parentEncoder: ProtobufEncoder,
        private val stream: ByteArrayOutputStream = ByteArrayOutputStream()
    ) : ProtobufWriter(ProtobufEncoder(stream)) {
        override fun endEncode(descriptor: SerialDescriptor) {
            if (parentTag != null) {
                parentEncoder.writeBytes(stream.toByteArray(), parentTag.protoId)
            } else {
                parentEncoder.out.write(stream.toByteArray())
            }
        }
    }

    internal inner class MapRepeatedWriter(parentTag: ProtoDesc?, parentEncoder: ProtobufEncoder) :
        ObjectWriter(parentTag, parentEncoder) {
        override fun SerialDescriptor.getTag(index: Int): ProtoDesc =
            if (index % 2 == 0) ProtoDesc(1, (parentTag?.numberType ?: ProtoNumberType.DEFAULT))
            else ProtoDesc(2, (parentTag?.numberType ?: ProtoNumberType.DEFAULT))
    }

    internal inner class RepeatedWriter(
        encoder: ProtobufEncoder,
        @JvmField val curTag: ProtoDesc
    ) : ProtobufWriter(encoder) {
        override fun SerialDescriptor.getTag(index: Int) = curTag
    }

    internal class ProtobufEncoder(val out: ByteArrayOutputStream) {

        fun writeBytes(bytes: ByteArray, tag: Int) {
            out.encode32((tag shl 3) or SIZE_DELIMITED)
            out.encode32(bytes.size)
            out.write(bytes)
        }

        fun writeInt(value: Int, tag: Int, format: ProtoNumberType) {
            val wireType = if (format == ProtoNumberType.FIXED) i32 else VARINT
            out.encode32((tag shl 3) or wireType)
            out.encode32(value, format)
        }

        fun writeLong(value: Long, tag: Int, format: ProtoNumberType) {
            val wireType = if (format == ProtoNumberType.FIXED) i64 else VARINT
            out.encode32((tag shl 3) or wireType)
            out.encode64(value, format)
        }

        fun writeString(value: String, tag: Int) {
            val bytes = value.encodeToByteArray()
            writeBytes(bytes, tag)
        }

        fun writeDouble(value: Double, tag: Int) {
            out.encode32((tag shl 3) or i64)
            out.writeLong(value.reverseBytes())
        }

        fun writeFloat(value: Float, tag: Int) {
            out.encode32((tag shl 3) or i32)
            out.writeInt(value.reverseBytes())
        }

        private fun OutputStream.encode32(
            number: Int,
            format: ProtoNumberType = ProtoNumberType.DEFAULT
        ) {
            when (format) {
                ProtoNumberType.FIXED -> out.writeInt(number.reverseBytes())
                ProtoNumberType.DEFAULT -> encodeVarint64(number.toLong())
                ProtoNumberType.SIGNED -> encodeVarint32(((number shl 1) xor (number shr 31)))
            }
        }

        private fun OutputStream.encode64(number: Long, format: ProtoNumberType = ProtoNumberType.DEFAULT) {
            when (format) {
                ProtoNumberType.FIXED -> out.writeLong(number.reverseBytes())
                ProtoNumberType.DEFAULT -> encodeVarint64(number)
                ProtoNumberType.SIGNED -> encodeVarint64((number shl 1) xor (number shr 63))
            }
        }
    }

    private open inner class ProtobufReader(@JvmField val decoder: ProtobufDecoder) : TaggedDecoder<ProtoDesc>() {
        override val context: SerialModule
            get() = this@ProtoBuf.context

        private var indexCache: IntArray? = null
        private var sparseIndexCache: MutableMap<Int, Int>? = null

        private fun getIndexByTag(descriptor: SerialDescriptor, serialId: Int, zeroBasedDefault: Boolean): Int {
            if (serialId < 32) {
                // Fast path
                var cache = indexCache
                if (cache == null) {
                    cache = IntArray(32) { -2 }
                    indexCache = cache
                }
                if (cache[serialId] == -2) {
                    cache[serialId] = findIndexByTag(descriptor, serialId, zeroBasedDefault)
                }
                return cache[serialId]
            }

            return getIndexByTagSlowPath(serialId, descriptor, zeroBasedDefault)
        }

        private fun getIndexByTagSlowPath(
            serialId: Int,
            descriptor: SerialDescriptor,
            zeroBasedDefault: Boolean
        ): Int {
            var cache = sparseIndexCache
            if (cache == null) {
                cache = mutableMapOf()
                sparseIndexCache = cache
            }
            return cache.getOrPut(serialId) { findIndexByTag(descriptor, serialId, zeroBasedDefault) }
        }

        private fun findIndexByTag(desc: SerialDescriptor, serialId: Int, zeroBasedDefault: Boolean = false): Int {
            for (i in 0 until desc.elementsCount) {
                val protoId = extractProtoId(
                    desc,
                    i,
                    zeroBasedDefault
                )
                if (protoId == serialId) return i
            }

            return -1
        }

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder =
            when (descriptor.kind) {
                StructureKind.LIST -> RepeatedReader(decoder, currentTag)
                StructureKind.CLASS, StructureKind.OBJECT, is PolymorphicKind ->
                    ProtobufReader(makeDelimited(decoder, currentTagOrNull))
                StructureKind.MAP -> MapEntryReader(makeDelimited(decoder, currentTagOrNull), currentTagOrNull)
                else -> throw SerializationException("Primitives are not supported at top-level")
            }

        override fun endStructure(descriptor: SerialDescriptor) {
            // Nothing
        }

        override fun decodeTaggedBoolean(tag: ProtoDesc): Boolean = when (val i = decoder.nextInt(ProtoNumberType.DEFAULT)) {
            0 -> false
            1 -> true
            else -> throw ProtobufDecodingException("Expected boolean value (0 or 1), found $i")
        }

        override fun decodeTaggedByte(tag: ProtoDesc): Byte = decoder.nextInt(tag.numberType).toByte()
        override fun decodeTaggedShort(tag: ProtoDesc): Short = decoder.nextInt(tag.numberType).toShort()

        override fun decodeTaggedInt(tag: ProtoDesc): Int {
            return decoder.nextInt(tag.numberType)
        }
        override fun decodeTaggedLong(tag: ProtoDesc): Long = decoder.nextLong(tag.numberType)
        override fun decodeTaggedFloat(tag: ProtoDesc): Float = decoder.nextFloat()
        override fun decodeTaggedDouble(tag: ProtoDesc): Double = decoder.nextDouble()
        override fun decodeTaggedChar(tag: ProtoDesc): Char = decoder.nextInt(tag.numberType).toChar()
        override fun decodeTaggedString(tag: ProtoDesc): String = decoder.nextString()
        override fun decodeTaggedEnum(tag: ProtoDesc, enumDescription: SerialDescriptor): Int =
            findIndexByTag(enumDescription, decoder.nextInt(ProtoNumberType.DEFAULT), zeroBasedDefault = true)

        @Suppress("UNCHECKED_CAST")
        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T = when {
            deserializer is MapLikeSerializer<*, *, *, *> -> {
                deserializeMap(deserializer as DeserializationStrategy<T>)
            }
            deserializer.descriptor == ByteArraySerializer().descriptor -> decoder.nextObject() as T
            else -> deserializer.deserialize(this)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> deserializeMap(deserializer: DeserializationStrategy<T>): T {
            // encode maps as collection of map entries, not merged collection of key-values
            val serializer = (deserializer as MapLikeSerializer<Any?, Any?, T, *>)
            val mapEntrySerial = MapEntrySerializer(serializer.keySerializer, serializer.valueSerializer)
            val setOfEntries = SetSerializer(mapEntrySerial).deserialize(this)
            return setOfEntries.associateBy({ it.key }, { it.value }) as T
        }

        override fun SerialDescriptor.getTag(index: Int) = extractParameters(index)

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            while (true) {
                if (decoder.currentId == -1) // EOF
                    return READ_DONE
                val index = getIndexByTag(descriptor, decoder.currentId, false)
                if (index == -1) { // not found
                    decoder.skipElement()
                } else { // not found
                    return index
                }
            }
        }
    }

    private inner class RepeatedReader(
        decoder: ProtobufDecoder,
        private val targetTag: ProtoDesc
    ) : ProtobufReader(decoder) {
        private var index = -1

        override fun decodeElementIndex(descriptor: SerialDescriptor) =
            if (decoder.currentId == targetTag.protoId) ++index else READ_DONE

        override fun SerialDescriptor.getTag(index: Int): ProtoDesc = targetTag
    }

    private inner class MapEntryReader(decoder: ProtobufDecoder, val parentTag: ProtoDesc?) : ProtobufReader(decoder) {
        override fun SerialDescriptor.getTag(index: Int): ProtoDesc =
                if (index % 2 == 0) ProtoDesc(1, (parentTag?.numberType ?: ProtoNumberType.DEFAULT))
                else ProtoDesc(2, (parentTag?.numberType ?: ProtoNumberType.DEFAULT))
    }

    internal class ProtobufDecoder(private val input: ByteArrayInputStream) {
        @JvmField
        public var currentId = -1
        @JvmField
        public var currentType = -1

        init {
            readTag()
        }

        private fun readTag() {
            val header = decode32(eofAllowed = true)
            if (header == -1) {
                currentId = -1
                currentType = -1
            } else {
                currentId = header ushr 3
                currentType = header and 0b111
            }
        }

        fun skipElement() {
            when (currentType) {
                VARINT -> nextInt(ProtoNumberType.DEFAULT)
                i64 -> nextLong(ProtoNumberType.FIXED)
                SIZE_DELIMITED -> nextObject()
                i32 -> nextInt(ProtoNumberType.FIXED)
                else -> throw ProtobufDecodingException("Unsupported start group or end group wire type")
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        private inline fun assertWireType(expected: Int) {
            if (currentType != expected) throw ProtobufDecodingException("Expected wire type $expected, but found $currentType")
        }

        fun nextObject(): ByteArray {
            assertWireType(SIZE_DELIMITED)
            val length = decode32()
            check(length >= 0)
            val result = input.readExactNBytes(length)
            readTag()
            return result
        }

        private fun InputStream.readExactNBytes(bytes: Int): ByteArray {
            val array = ByteArray(bytes)
            var read = 0
            while (read < bytes) {
                val i = this.read(array, read, bytes - read)
                if (i == -1) error("Unexpected EOF")
                read += i
            }
            return array
        }

        fun nextInt(format: ProtoNumberType): Int {
            val wireType = if (format == ProtoNumberType.FIXED) i32 else VARINT
            assertWireType(wireType)
            val result = decode32(format)
            readTag()
            return result
        }

        fun nextLong(format: ProtoNumberType): Long {
            val wireType = if (format == ProtoNumberType.FIXED) i64 else VARINT
            assertWireType(wireType)
            val result = decode64(format)
            readTag()
            return result
        }

        fun nextFloat(): Float {
            assertWireType(i32)
            val result = Float.fromBits(readIntLittleEndian())
            readTag()
            return result
        }

        private fun readIntLittleEndian(): Int {
            // TODO this could be optimized by extracting method to the IS
            var result = 0
            for (i in 0..3) {
                val byte = input.read() and 0x000000FF
                result = result or (byte shl (i * 8))
            }
            return result
        }

        private fun readLongLittleEndian(): Long {
            // TODO this could be optimized by extracting method to the IS
            var result = 0L
            for (i in 0..7) {
                val byte = (input.read() and 0x000000FF).toLong()
                result = result or (byte shl (i * 8))
            }
            return result
        }

        fun nextDouble(): Double {
            assertWireType(i64)
            val result = Double.fromBits(readLongLittleEndian())
            readTag()
            return result
        }

        fun nextString(): String {
            assertWireType(SIZE_DELIMITED)
            val length = decode32()
            check(length >= 0)
            val result = input.readString(length)
            readTag()
            return result
        }

        private fun decode32(format: ProtoNumberType = ProtoNumberType.DEFAULT, eofAllowed: Boolean = false): Int = when (format) {
            ProtoNumberType.DEFAULT -> input.readVarint64(eofAllowed).toInt()
            ProtoNumberType.SIGNED -> decodeSignedVarintInt(input)
            ProtoNumberType.FIXED -> readIntLittleEndian()
        }

        private fun decode64(format: ProtoNumberType = ProtoNumberType.DEFAULT): Long = when (format) {
            ProtoNumberType.DEFAULT -> input.readVarint64(false)
            ProtoNumberType.SIGNED -> decodeSignedVarintLong(input)
            ProtoNumberType.FIXED -> readLongLittleEndian()
        }
    }

    /**
     *  Source for all varint operations:
     *  https://github.com/addthis/stream-lib/blob/master/src/main/java/com/clearspring/analytics/util/Varint.java
     */
    internal object Varint {
        internal fun decodeSignedVarintInt(input: InputStream): Int {
            val raw = input.readVarint32()
            val temp = raw shl 31 shr 31 xor raw shr 1
            // This extra step lets us deal with the largest signed values by treating
            // negative results from read unsigned methods as like unsigned values.
            // Must re-flip the top bit if the original read value had it set.
            return temp xor (raw and (1 shl 31))
        }

        internal fun decodeSignedVarintLong(input: InputStream): Long {
            val raw = input.readVarint64(false)
            val temp = raw shl 63 shr 63 xor raw shr 1
            // This extra step lets us deal with the largest signed values by treating
            // negative results from read unsigned methods as like unsigned values
            // Must re-flip the top bit if the original read value had it set.
            return temp xor (raw and (1L shl 63))

        }
    }

    public companion object Default : BinaryFormat by ProtoBuf() {
        private fun makeDelimited(decoder: ProtobufDecoder, parentTag: ProtoDesc?): ProtobufDecoder {
            if (parentTag == null) return decoder
            // TODO use array slice instead of array copy
            val bytes = decoder.nextObject()
            return ProtobufDecoder(ByteArrayInputStream(bytes))
        }

        internal const val VARINT = 0
        internal const val i64 = 1
        internal const val SIZE_DELIMITED = 2
        internal const val i32 = 5
    }

    override fun <T> dump(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val encoder = ByteArrayOutputStream()
        val dumper = ProtobufWriter(ProtobufEncoder(encoder))
        dumper.encode(serializer, value)
        return encoder.toByteArray()
    }

    override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val stream = ByteArrayInputStream(bytes)
        val reader = ProtobufReader(ProtobufDecoder(stream))
        return reader.decode(deserializer)
    }
}

private fun Float.reverseBytes(): Int = toRawBits().reverseBytes()

private fun Double.reverseBytes(): Long = toRawBits().reverseBytes()
