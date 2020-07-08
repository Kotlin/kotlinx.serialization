/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.protobuf.internal.*
import kotlin.jvm.*

/**
 * Implements [encoding][encodeToByteArray] and [decoding][decodeFromByteArray] classes to/from bytes
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
 * val encoded = ProtoBuf.decodeToHexString(MyMessage(15)) // "080f1000182a"
 *
 * // Deserialize from ProtoBuf hex string
 * val decoded = ProtoBuf.decodeFromHexString<MyMessage>(encoded) // MyMessage(first=15, second=0, third=42)
 *
 * // Serialize to ProtoBuf bytes (omitting default values)
 * val encoded2 = ProtoBuf(encodeDefaults = false).encodeToByteArray( MyMessage(15)) // [0x08, 0x0f]
 *
 * // Deserialize ProtoBuf bytes will use default values of the MyMessage class
 * val decoded2 = ProtoBuf.decodeFromByteArray<MyMessage>(encoded2) // MyMessage(first=15, second=0, third=42)
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
 * val encoded = ProtoBuf(encodeDefaults = false).encodeToByteArray(MyMessage(15)) // [0x08, 0x0f]
 *
 * // Deserialize ProtoBuf bytes
 * val decoded = ProtoBuf.decodeFromByteArray<MyMessage>(encoded) // MyMessage(first=15, _second=null, _third=null)
 * decoded.hasSecond()     // false
 * decoded.second          // 0
 * decoded.hasThird()      // false
 * decoded.third           // 42
 *
 * // Serialize to ProtoBuf bytes
 * val encoded2 = ProtoBuf(encodeDefaults = false).encodeToByteArray(MyMessage(15, 0, 0)) // [0x08, 0x0f, 0x10, 0x00, 0x18, 0x00]
 *
 * // Deserialize ProtoBuf bytes
 * val decoded2 = ProtoBuf.decodeFromByteArray<MyMessage>(encoded2) // MyMessage(first=15, _second=0, _third=0)
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

    internal open inner class ProtobufEncoder(
        private val writer: ProtobufWriter,
        @JvmField val descriptor: SerialDescriptor
    ) : ProtobufTaggedEncoder() {
        public override val serializersModule
            get() = this@ProtoBuf.context

        override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = encodeDefaults

        override fun beginCollection(
            descriptor: SerialDescriptor,
            collectionSize: Int
        ): CompositeEncoder = when (descriptor.kind) {
            StructureKind.LIST -> {
                val tag = currentTagOrDefault
                if (tag == MISSING_TAG) {
                    writer.writeInt(collectionSize)
                }
                RepeatedEncoder(writer, tag, descriptor)
            }
            StructureKind.MAP -> {
                // Size and missing tag are managed by the implementation that delegated to the list
                MapRepeatedEncoder(currentTag, writer, descriptor)
            }
            else -> throw SerializationException("This serial kind is not supported as collection: $descriptor")
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = when (descriptor.kind) {
            StructureKind.LIST -> RepeatedEncoder(writer, currentTagOrDefault, descriptor)
            StructureKind.CLASS, StructureKind.OBJECT, is PolymorphicKind -> {
                val tag = currentTagOrDefault
                if (tag == MISSING_TAG && descriptor == this.descriptor) this
                else ObjectEncoder(currentTagOrDefault, writer, descriptor =  descriptor)
            }
            StructureKind.MAP -> MapRepeatedEncoder(currentTagOrDefault, writer, descriptor)
            else -> throw SerializationException("This serial kind is not supported as structure: $descriptor")
        }

        override fun encodeTaggedInt(tag: ProtoDesc, value: Int) {
            if (tag == MISSING_TAG) {
                writer.writeInt(value)
            } else {
                writer.writeInt(value, tag.protoId, tag.numberType)
            }
        }

        override fun encodeTaggedByte(tag: ProtoDesc, value: Byte) = encodeTaggedInt(tag, value.toInt())
        override fun encodeTaggedShort(tag: ProtoDesc, value: Short) = encodeTaggedInt(tag, value.toInt())
        override fun encodeTaggedBoolean(tag: ProtoDesc, value: Boolean) = encodeTaggedInt(tag, if (value) 1 else 0)
        override fun encodeTaggedChar(tag: ProtoDesc, value: Char) = encodeTaggedInt(tag, value.toInt())

        override fun encodeTaggedLong(tag: ProtoDesc, value: Long) {
            if (tag == MISSING_TAG) {
                writer.writeLong(value)
            } else {
                writer.writeLong(value, tag.protoId, tag.numberType)
            }
        }

        override fun encodeTaggedFloat(tag: ProtoDesc, value: Float) {
            if (tag == MISSING_TAG) {
                writer.writeFloat(value)
            } else {
                writer.writeFloat(value, tag.protoId)
            }
        }

        override fun encodeTaggedDouble(tag: ProtoDesc, value: Double) {
            if (tag == MISSING_TAG) {
                writer.writeDouble(value)
            } else {
                writer.writeDouble(value, tag.protoId)
            }
        }

        override fun encodeTaggedString(tag: ProtoDesc, value: String) {
            if (tag == MISSING_TAG) {
                writer.writeString(value)
            } else {
                writer.writeString(value, tag.protoId)
            }
        }

        override fun encodeTaggedEnum(
            tag: ProtoDesc,
            enumDescription: SerialDescriptor,
            ordinal: Int
        ) {
            if (tag == MISSING_TAG) {
                writer.writeInt(extractProtoId(enumDescription, ordinal, zeroBasedDefault = true))
            } else {
                writer.writeInt(
                    extractProtoId(enumDescription, ordinal, zeroBasedDefault = true),
                    tag.protoId,
                    ProtoNumberType.DEFAULT
                )
            }
        }

        override fun SerialDescriptor.getTag(index: Int) = extractParameters(index)

        override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) = when {
            serializer is MapLikeSerializer<*, *, *, *> -> {
                serializeMap(serializer as SerializationStrategy<T>, value)
            }
            serializer.descriptor == ByteArraySerializer().descriptor -> serializeByteArray(value as ByteArray)
            else -> serializer.serialize(this, value)
        }

        private fun serializeByteArray(value: ByteArray) {
            val tag = popTagOrDefault()
            if (tag == MISSING_TAG) {
                writer.writeBytes(value)
            } else {
                writer.writeBytes(value, tag.protoId)
            }
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
        @JvmField val parentTag: ProtoDesc,
        @JvmField protected val parentWriter: ProtobufWriter,
        @JvmField protected val stream: ByteArrayOutput = ByteArrayOutput(),
        descriptor: SerialDescriptor
    ) : ProtobufEncoder(ProtobufWriter(stream), descriptor) {
        override fun endEncode(descriptor: SerialDescriptor) {
            // TODO this is exactly the lookahead scenario
            if (parentTag != MISSING_TAG) {
                parentWriter.writeBytes(stream.toByteArray(), parentTag.protoId)
            } else {
                parentWriter.out.write(stream.toByteArray())
            }
        }
    }

    internal inner class MapRepeatedEncoder(
        parentTag: ProtoDesc,
        parentWriter: ProtobufWriter,
        descriptor: SerialDescriptor
    ) : ObjectEncoder(parentTag, parentWriter, descriptor = descriptor) {
        override fun SerialDescriptor.getTag(index: Int): ProtoDesc =
            if (index % 2 == 0) ProtoDesc(1, (parentTag.numberType))
            else ProtoDesc(2, (parentTag.numberType))

        override fun endEncode(descriptor: SerialDescriptor) {
            if (parentTag != MISSING_TAG) {
                parentWriter.writeBytes(stream.toByteArray(), parentTag.protoId)
            } else {
                parentWriter.writeBytes(stream.toByteArray())
            }
        }
    }

    internal inner class RepeatedEncoder(
        writer: ProtobufWriter,
        @JvmField val curTag: ProtoDesc,
        descriptor: SerialDescriptor
    ) : ProtobufEncoder(writer, descriptor) {

        override fun SerialDescriptor.getTag(index: Int) = curTag
    }

    private open inner class ProtobufDecoder(
        @JvmField val reader: ProtobufReader,
        @JvmField val descriptor: SerialDescriptor
    ) : ProtobufTaggedDecoder() {
        override val serializersModule: SerialModule
            get() = this@ProtoBuf.context

        // Proto id -> index in serial descriptor cache
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

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            return when (descriptor.kind) {
                StructureKind.LIST -> RepeatedDecoder(reader, currentTagOrDefault, descriptor)
                StructureKind.CLASS, StructureKind.OBJECT, is PolymorphicKind -> {
                    val tag = currentTagOrDefault
                        // Do not create redundant copy
                    if (tag == MISSING_TAG && this.descriptor == descriptor) return this
                    return ProtobufDecoder(makeDelimited(reader, tag), descriptor)
                }
                StructureKind.MAP -> MapEntryReader(makeDelimitedForced(reader, currentTagOrDefault), currentTagOrDefault, descriptor)
                else -> throw SerializationException("Primitives are not supported at top-level")
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            // Nothing
        }

        override fun decodeTaggedBoolean(tag: ProtoDesc): Boolean = when(val value = decodeTaggedInt(tag)) {
            0 -> false
            1 -> true
            else -> throw SerializationException("Unexpected boolean value: $value")
        }

        override fun decodeTaggedByte(tag: ProtoDesc): Byte = decodeTaggedInt(tag).toByte()
        override fun decodeTaggedShort(tag: ProtoDesc): Short = decodeTaggedInt(tag).toShort()
        override fun decodeTaggedInt(tag: ProtoDesc): Int {
            return if (tag == MISSING_TAG) {
                reader.readInt32NoTag()
            } else {
                reader.readInt(tag.numberType)
            }
        }
        override fun decodeTaggedLong(tag: ProtoDesc): Long {
            return if (tag == MISSING_TAG) {
                reader.readLongNoTag()
            } else {
                reader.readLong(tag.numberType)
            }
        }

        override fun decodeTaggedFloat(tag: ProtoDesc): Float {
            return if (tag == MISSING_TAG) {
                reader.readFloatNoTag()
            } else {
                reader.readFloat()
            }
        }
        override fun decodeTaggedDouble(tag: ProtoDesc): Double {
            return if (tag == MISSING_TAG) {
                reader.readDoubleNoTag()
            } else {
                reader.readDouble()
            }
        }
        override fun decodeTaggedChar(tag: ProtoDesc): Char = decodeTaggedInt(tag).toChar()

        override fun decodeTaggedString(tag: ProtoDesc): String {
            return if (tag == MISSING_TAG) {
                reader.readStringNoTag()
            } else {
                reader.readString()
            }
        }

        override fun decodeTaggedEnum(tag: ProtoDesc, enumDescription: SerialDescriptor): Int {
            return findIndexByTag(enumDescription, decodeTaggedInt(tag))
        }

        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T = decodeSerializableValue(deserializer, null)

        @Suppress("UNCHECKED_CAST")
        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>, previousValue: T?): T = when {
            deserializer is MapLikeSerializer<*, *, *, *> -> {
                deserializeMap(deserializer as DeserializationStrategy<T>, previousValue)
            }
            deserializer.descriptor == ByteArraySerializer().descriptor -> deserializeByteArray(previousValue as ByteArray?) as T
            deserializer is AbstractCollectionSerializer<*, *, *> ->
                (deserializer as AbstractCollectionSerializer<*, T, *>).merge(this, previousValue)
            else -> deserializer.deserialize(this)
        }

        private fun deserializeByteArray(previousValue: ByteArray?): ByteArray {
            val tag = currentTagOrDefault
            val array = if (tag == MISSING_TAG) {
                reader.readObjectNoTag()
            } else {
                reader.readObject()
            }
            return if (previousValue == null) array else previousValue + array
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> deserializeMap(deserializer: DeserializationStrategy<T>, previousValue: T?): T {
            val serializer = (deserializer as MapLikeSerializer<Any?, Any?, T, *>)
            val mapEntrySerial = MapEntrySerializer(serializer.keySerializer, serializer.valueSerializer)
            val oldSet = (previousValue as? Map<Any?, Any?>)?.entries
            @Suppress("DEPRECATION_ERROR") // to use .merge from LinkedHashSetSer
            val setOfEntries = LinkedHashSetSerializer(mapEntrySerial).merge(this, oldSet)
            return setOfEntries.associateBy({ it.key }, { it.value }) as T
        }

        override fun SerialDescriptor.getTag(index: Int) = extractParameters(index)

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            while (true) {
                val protoId = reader.readTag()
                if (protoId == -1) { // EOF
                    return CompositeDecoder.DECODE_DONE
                }
                val index = getIndexByTag(protoId)
                if (index == -1) { // not found
                    reader.skipElement()
                } else {
                    return index
                }
            }
        }
    }

    private inner class RepeatedDecoder(
        decoder: ProtobufReader,
        currentTag: ProtoDesc,
        descriptor: SerialDescriptor
    ) : ProtobufDecoder(decoder, descriptor) {
        // Current index
        private var index = -1

        /*
         * For regular messages, it is always a tag.
         * For out-of-spec top-level lists (and maps) the very first varint
         * represents this list size. It is stored in a single variable
         * as negative value and branched based on that fact.
         */
        private val tagOrSize: Long

        init {
            tagOrSize = if (currentTag == MISSING_TAG) {
                val length = reader.readInt32NoTag()
                require(length >= 0) { "Expected positive length for $descriptor, but got $length" }
                -length.toLong()
            } else {
                currentTag
            }
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (tagOrSize > 0) {
                return decodeTaggedListIndex()
            }
            return decodeListIndexNoTag()
        }

        private fun decodeListIndexNoTag(): Int {
            val size = -tagOrSize
            val idx = ++index
            if (idx.toLong() == size) return CompositeDecoder.DECODE_DONE
            return idx
        }

        private fun decodeTaggedListIndex(): Int {
            val protoId = if (index == -1) {
                // For the very first element tag is already read by the parent
                reader.currentId
            } else {
                reader.readTag()
            }

            return if (protoId == tagOrSize.protoId) {
                ++index
            } else {
                // If we read tag of a different message, push it back to the reader and bail out
                reader.pushBackTag()
                CompositeDecoder.DECODE_DONE
            }
        }

        override fun SerialDescriptor.getTag(index: Int): ProtoDesc {
            if (tagOrSize > 0) return tagOrSize
            return MISSING_TAG
        }
    }

    private inner class MapEntryReader(
        decoder: ProtobufReader,
        @JvmField val parentTag: ProtoDesc,
        descriptor: SerialDescriptor
    ) : ProtobufDecoder(decoder, descriptor) {
        override fun SerialDescriptor.getTag(index: Int): ProtoDesc =
            if (index % 2 == 0) ProtoDesc(1, (parentTag.numberType))
            else ProtoDesc(2, (parentTag.numberType))
    }

    public companion object Default : BinaryFormat by ProtoBuf() {
        private fun makeDelimited(decoder: ProtobufReader, parentTag: ProtoDesc): ProtobufReader {
            if (parentTag == MISSING_TAG) return decoder
            // TODO use array slice instead of array copy
            val bytes = decoder.readObject()
            return ProtobufReader(ByteArrayInput(bytes))
        }

        private fun makeDelimitedForced(decoder: ProtobufReader, parentTag: ProtoDesc): ProtobufReader {
            val bytes = if (parentTag == MISSING_TAG) decoder.readObjectNoTag()
            else decoder.readObject()
            return ProtobufReader(ByteArrayInput(bytes))
        }

        internal const val VARINT = 0
        internal const val i64 = 1
        internal const val SIZE_DELIMITED = 2
        internal const val i32 = 5
    }

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val output = ByteArrayOutput()
        val encoder = ProtobufEncoder(ProtobufWriter(output), serializer.descriptor)
        encoder.encodeSerializableValue(serializer, value)
        return output.toByteArray()
    }

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val input = ByteArrayInput(bytes)
        val decoder = ProtobufDecoder(ProtobufReader(input), deserializer.descriptor)
        return decoder.decodeSerializableValue(deserializer)
    }
}
