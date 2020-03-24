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
import kotlinx.serialization.protobuf.internal.*
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

    internal open inner class ProtobufEncoder(private val writer: ProtobufWriter) : TaggedEncoder<ProtoDesc>() {
        public override val context
            get() = this@ProtoBuf.context

        override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = encodeDefaults

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = when (descriptor.kind) {
            StructureKind.LIST -> RepeatedEncoder(writer, currentTag)
            StructureKind.CLASS, StructureKind.OBJECT, is PolymorphicKind -> ObjectEncoder(currentTagOrNull, writer)
            StructureKind.MAP -> MapRepeatedEncoder(currentTag, writer)
            else -> throw SerializationException("Primitives are not supported at top-level")
        }

        override fun encodeTaggedInt(tag: ProtoDesc, value: Int) = writer.writeInt(value, tag.protoId, tag.numberType)
        override fun encodeTaggedByte(tag: ProtoDesc, value: Byte) = writer.writeInt(value.toInt(), tag.protoId, tag.numberType)
        override fun encodeTaggedShort(tag: ProtoDesc, value: Short) = writer.writeInt(value.toInt(), tag.protoId, tag.numberType)
        override fun encodeTaggedLong(tag: ProtoDesc, value: Long) = writer.writeLong(value, tag.protoId, tag.numberType)

        override fun encodeTaggedFloat(tag: ProtoDesc, value: Float) = writer.writeFloat(value, tag.protoId)
        override fun encodeTaggedDouble(tag: ProtoDesc, value: Double) = writer.writeDouble(value, tag.protoId)
        override fun encodeTaggedBoolean(tag: ProtoDesc, value: Boolean) = writer.writeInt(if (value) 1 else 0, tag.protoId, ProtoNumberType.DEFAULT)
        override fun encodeTaggedChar(tag: ProtoDesc, value: Char) = writer.writeInt(value.toInt(), tag.protoId, tag.numberType)

        override fun encodeTaggedString(tag: ProtoDesc, value: String) = writer.writeString(value, tag.protoId)
        override fun encodeTaggedEnum(
            tag: ProtoDesc,
            enumDescription: SerialDescriptor,
            ordinal: Int
        ): Unit = writer.writeInt(
            extractProtoId(enumDescription, ordinal, zeroBasedDefault = true),
            tag.protoId,
            ProtoNumberType.DEFAULT
        )

        override fun SerialDescriptor.getTag(index: Int) = extractParameters(index)

        override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) = when {
            serializer is MapLikeSerializer<*, *, *, *> -> {
                serializeMap(serializer as SerializationStrategy<T>, value)
            }
            serializer.descriptor == ByteArraySerializer().descriptor -> writer.writeBytes(
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

    internal open inner class ObjectEncoder(
        val parentTag: ProtoDesc?,
        private val parentWriter: ProtobufWriter,
        private val stream: ByteArrayOutput = ByteArrayOutput()
    ) : ProtobufEncoder(ProtobufWriter(stream)) {
        override fun endEncode(descriptor: SerialDescriptor) {
            if (parentTag != null) {
                parentWriter.writeBytes(stream.toByteArray(), parentTag.protoId)
            } else {
                parentWriter.out.write(stream.toByteArray())
            }
        }
    }

    internal inner class MapRepeatedEncoder(
        parentTag: ProtoDesc,
        parentWriter: ProtobufWriter
    ) : ObjectEncoder(parentTag, parentWriter) {
        override fun SerialDescriptor.getTag(index: Int): ProtoDesc =
            if (index % 2 == 0) ProtoDesc(1, (parentTag!!.numberType))
            else ProtoDesc(2, (parentTag!!.numberType))
    }

    internal inner class RepeatedEncoder(
        writer: ProtobufWriter,
        @JvmField val curTag: ProtoDesc
    ) : ProtobufEncoder(writer) {
        override fun SerialDescriptor.getTag(index: Int) = curTag
    }

    private open inner class ProtobufDecoder(
        @JvmField val reader: ProtobufReader,
        descriptor: SerialDescriptor
    ) : TaggedDecoder<ProtoDesc>() {
        override val context: SerialModule
            get() = this@ProtoBuf.context

        private var indexCache: IntArray? = null
        private var sparseIndexCache: MutableMap<Int, Int>? = null

        init {
            populateCache(descriptor)
        }

        public fun populateCache(descriptor: SerialDescriptor) {
            val elements = descriptor.elementsCount
            if (elements < 32) {
                /*
                 * If we have reasonably small count of elements, try to build sequential
                 * array for the fast-path. Fast-path implies that elements are not marked with @ProtoId
                 * explicitly or are monotonic and incremental (maybe, 1-indexed)
                 */
                val cache = IntArray(elements + 1)
                for (i in 0 until elements) {
                    val protoId = extractProtoId(descriptor, i, false)
                    if (protoId <= elements) {
                        cache[protoId] = i
                    } else {
                        return populateCacheMap(descriptor, elements)
                    }
                }
                indexCache = cache
            } else {
                populateCacheMap(descriptor, elements)
            }
        }

        private fun populateCacheMap(descriptor: SerialDescriptor, elements: Int) {
            val map = HashMap<Int, Int>(elements)
            for (i in 0 until elements) {
                map[extractProtoId(descriptor, i, false)] = i
            }
            sparseIndexCache = map
        }

        private fun getIndexByTag(protoTag: Int): Int {
            val array = indexCache
            if (array != null) {
                return array.getOrElse(protoTag) { -1 }
            }
            return getIndexByTagSlowPath(protoTag)
        }

        private fun getIndexByTagSlowPath(
            protoTag: Int
        ): Int = sparseIndexCache!!.getOrElse(protoTag) { -1 }

        private fun findIndexByTag(descriptor: SerialDescriptor, protoTag: Int): Int {
            // Fast-path: tags are incremental, 1-based
            if (protoTag < descriptor.elementsCount) {
                val protoId = extractProtoId(descriptor, protoTag, true)
                if (protoId == protoTag) return protoTag
            }
            return findIndexByTagSlowPath(descriptor, protoTag)
        }

        private fun findIndexByTagSlowPath(desc: SerialDescriptor, protoTag: Int): Int {
            for (i in 0 until desc.elementsCount) {
                val protoId = extractProtoId(desc, i, true)
                if (protoId == protoTag) return i
            }

            return -1
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            when (descriptor.kind) {
                StructureKind.LIST -> RepeatedDecoder(reader, currentTag, descriptor)
                StructureKind.CLASS, StructureKind.OBJECT, is PolymorphicKind ->
                    ProtobufDecoder(makeDelimited(reader, currentTagOrNull), descriptor)
                StructureKind.MAP -> MapEntryReader(makeDelimited(reader, currentTagOrNull), currentTagOrNull, descriptor)
                else -> throw SerializationException("Primitives are not supported at top-level")
            }

        override fun endStructure(descriptor: SerialDescriptor) {
            // Nothing
        }

        override fun decodeTaggedBoolean(tag: ProtoDesc): Boolean = reader.nextBoolean()
        override fun decodeTaggedByte(tag: ProtoDesc): Byte = reader.nextInt(tag.numberType).toByte()
        override fun decodeTaggedShort(tag: ProtoDesc): Short = reader.nextInt(tag.numberType).toShort()
        override fun decodeTaggedInt(tag: ProtoDesc): Int = reader.nextInt(tag.numberType)
        override fun decodeTaggedLong(tag: ProtoDesc): Long = reader.nextLong(tag.numberType)
        override fun decodeTaggedFloat(tag: ProtoDesc): Float = reader.nextFloat()
        override fun decodeTaggedDouble(tag: ProtoDesc): Double = reader.nextDouble()
        override fun decodeTaggedChar(tag: ProtoDesc): Char = reader.nextInt(tag.numberType).toChar()
        override fun decodeTaggedString(tag: ProtoDesc): String = reader.nextString()
        override fun decodeTaggedEnum(tag: ProtoDesc, enumDescription: SerialDescriptor): Int =
            findIndexByTag(enumDescription, reader.nextInt(ProtoNumberType.DEFAULT))

        @Suppress("UNCHECKED_CAST")
        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>, previousValue: T?): T = when {
            deserializer is MapLikeSerializer<*, *, *, *> -> {
                deserializeMap(deserializer as DeserializationStrategy<T>, previousValue)
            }
            deserializer.descriptor == ByteArraySerializer().descriptor -> {
                val newValue = decoder.nextObject()
                (if (previousValue == null) newValue else (previousValue as ByteArray) + newValue) as T
            }
            deserializer is AbstractCollectionSerializer<*, *, *> ->
                (deserializer as AbstractCollectionSerializer<*, T, *>).merge(this, previousValue)
            else -> deserializer.deserialize(this)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> deserializeMap(deserializer: DeserializationStrategy<T>, oldValue: T?): T {
            val serializer = (deserializer as MapLikeSerializer<Any?, Any?, T, *>)
            val mapEntrySerial = MapEntrySerializer(serializer.keySerializer, serializer.valueSerializer)
            val oldSet = (oldValue as? Map<Any?, Any?>)?.entries

            @Suppress("DEPRECATION_ERROR") // to use .merge from LinkedHashSetSer
            val setOfEntries = LinkedHashSetSerializer(mapEntrySerial).merge(this, oldSet)
            return setOfEntries.associateBy({ it.key }, { it.value }) as T
        }

        override fun SerialDescriptor.getTag(index: Int) = extractParameters(index)

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            while (true) {
                if (reader.currentId == -1) // EOF
                    return READ_DONE
                val index = getIndexByTag(reader.currentId)
                if (index == -1) { // not found
                    reader.skipElement()
                } else { // not found
                    return index
                }
            }
        }
    }

    private inner class RepeatedDecoder(
        decoder: ProtobufReader,
        private val targetTag: ProtoDesc,
        descriptor: SerialDescriptor
    ) : ProtobufDecoder(decoder, descriptor) {
        private var index = -1

        override fun decodeElementIndex(descriptor: SerialDescriptor) =
            if (reader.currentId == targetTag.protoId) ++index else READ_DONE

        override fun SerialDescriptor.getTag(index: Int): ProtoDesc = targetTag
    }

    private inner class MapEntryReader(
        decoder: ProtobufReader,
        @JvmField val parentTag: ProtoDesc?,
        descriptor: SerialDescriptor
    ) : ProtobufDecoder(decoder, descriptor) {
        override fun SerialDescriptor.getTag(index: Int): ProtoDesc =
            if (index % 2 == 0) ProtoDesc(1, (parentTag?.numberType ?: ProtoNumberType.DEFAULT))
            else ProtoDesc(2, (parentTag?.numberType ?: ProtoNumberType.DEFAULT))
    }

    public companion object Default : BinaryFormat by ProtoBuf() {
        private fun makeDelimited(decoder2: ProtobufReader, parentTag: ProtoDesc?): ProtobufReader {
            if (parentTag == null) return decoder2
            // TODO use array slice instead of array copy
            val bytes = decoder2.nextObject()
            return ProtobufReader(ByteArrayInput(bytes))
        }

        internal const val VARINT = 0
        internal const val i64 = 1
        internal const val SIZE_DELIMITED = 2
        internal const val i32 = 5
    }

    override fun <T> dump(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val output = ByteArrayOutput()
        val encoder = ProtobufEncoder(ProtobufWriter(output))
        encoder.encode(serializer, value)
        return output.toByteArray()
    }

    override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val input = ByteArrayInput(bytes)
        val decoder = ProtobufDecoder(ProtobufReader(input), deserializer.descriptor)
        return decoder.decode(deserializer)
    }
}
