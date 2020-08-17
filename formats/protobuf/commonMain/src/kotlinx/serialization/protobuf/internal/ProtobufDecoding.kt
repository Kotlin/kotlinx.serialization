/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.protobuf.*
import kotlin.jvm.*

internal open class ProtobufDecoder(
    @JvmField protected val proto: ProtoBuf,
    @JvmField protected val reader: ProtobufReader,
    @JvmField protected val descriptor: SerialDescriptor
) : ProtobufTaggedDecoder() {
    override val serializersModule: SerializersModule
        get() = proto.serializersModule

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
            StructureKind.LIST -> RepeatedDecoder(proto, reader, currentTagOrDefault, descriptor)
            StructureKind.CLASS, StructureKind.OBJECT, is PolymorphicKind -> {
                val tag = currentTagOrDefault
                // Do not create redundant copy
                if (tag == MISSING_TAG && this.descriptor == descriptor) return this
                return ProtobufDecoder(proto, makeDelimited(reader, tag), descriptor)
            }
            StructureKind.MAP -> MapEntryReader(proto, makeDelimitedForced(reader, currentTagOrDefault), currentTagOrDefault, descriptor)
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
            reader.readInt(tag.integerType)
        }
    }
    override fun decodeTaggedLong(tag: ProtoDesc): Long {
        return if (tag == MISSING_TAG) {
            reader.readLongNoTag()
        } else {
            reader.readLong(tag.integerType)
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
            reader.readByteArrayNoTag()
        } else {
            reader.readByteArray()
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

private class RepeatedDecoder(
    proto: ProtoBuf,
    decoder: ProtobufReader,
    currentTag: ProtoDesc,
    descriptor: SerialDescriptor
) : ProtobufDecoder(proto, decoder, descriptor) {
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

private class MapEntryReader(
    proto: ProtoBuf,
    decoder: ProtobufReader,
    @JvmField val parentTag: ProtoDesc,
    descriptor: SerialDescriptor
) : ProtobufDecoder(proto, decoder, descriptor) {
    override fun SerialDescriptor.getTag(index: Int): ProtoDesc =
        if (index % 2 == 0) ProtoDesc(1, (parentTag.integerType))
        else ProtoDesc(2, (parentTag.integerType))
}

private fun makeDelimited(decoder: ProtobufReader, parentTag: ProtoDesc): ProtobufReader {
    val tagless = parentTag == MISSING_TAG
    val input = if (tagless) decoder.objectTaglessInput() else decoder.objectInput()
    return ProtobufReader(input)
}

private fun makeDelimitedForced(decoder: ProtobufReader, parentTag: ProtoDesc): ProtobufReader {
    val tagless = parentTag == MISSING_TAG
    val input = if (tagless) decoder.objectTaglessInput() else decoder.objectInput()
    return ProtobufReader(input)
}
