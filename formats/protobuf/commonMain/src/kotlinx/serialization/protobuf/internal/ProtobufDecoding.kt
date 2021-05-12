/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalSerializationApi::class)
@file:Suppress("UNCHECKED_CAST", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

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

    /*
    Element decoding marks from given bytes.
    The element number is the same as the bit position.
    Marks for the lowest 64 elements are always stored in a single Long value, higher elements stores in long array.
     */
    private var lowerReadMark: Long = 0
    private val highReadMarks: LongArray?

    private var valueIsNull: Boolean = false

    init {
        highReadMarks = prepareReadMarks(descriptor)
        populateCache(descriptor)
    }

    private fun prepareReadMarks(descriptor: SerialDescriptor): LongArray? {
        val elementsCount = descriptor.elementsCount
        return if (elementsCount <= Long.SIZE_BITS) {
            lowerReadMark = if (elementsCount == Long.SIZE_BITS) {
                // number og bits in the mark is equal to the number of fields
                0
            } else {
                // (1 - elementsCount) bits are always 1 since there are no fields for them
                -1L shl elementsCount
            }
            null
        } else {
            // (elementsCount - 1) because only one Long value is needed to store 64 fields etc
            val slotsCount = (elementsCount - 1) / Long.SIZE_BITS
            val elementsInLastSlot = elementsCount % Long.SIZE_BITS
            val highReadMarks = LongArray(slotsCount)
            // (elementsCount % Long.SIZE_BITS) == 0 this means that the fields occupy all bits in mark
            if (elementsInLastSlot != 0) {
                // all marks except the higher are always 0
                highReadMarks[highReadMarks.lastIndex] = -1L shl elementsCount
            }
            highReadMarks
        }
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

        throw ProtobufDecodingException(
            "$protoTag is not among valid ${descriptor.serialName} enum proto numbers"
        )
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return when (descriptor.kind) {
            StructureKind.LIST -> {
                val tag = currentTagOrDefault
                return if (this.descriptor.kind == StructureKind.LIST && tag != MISSING_TAG && this.descriptor != descriptor) {
                    val reader = makeDelimited(reader, tag)
                    // repeated decoder expects the first tag to be read already
                    reader.readTag()
                    // all elements always have id = 1
                    RepeatedDecoder(proto, reader, ProtoDesc(1, ProtoIntegerType.DEFAULT), descriptor)
                } else {
                    RepeatedDecoder(proto, reader, tag, descriptor)
                }
            }
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
        // Yeah thanks different resolution algorithms
        val mapEntrySerial =
            kotlinx.serialization.builtins.MapEntrySerializer(serializer.keySerializer, serializer.valueSerializer)
        val oldSet = (previousValue as? Map<Any?, Any?>)?.entries
        val setOfEntries = LinkedHashSetSerializer(mapEntrySerial).merge(this, oldSet)
        return setOfEntries.associateBy({ it.key }, { it.value }) as T
    }

    override fun SerialDescriptor.getTag(index: Int) = extractParameters(index)

    private fun findUnreadElementIndex(): Int {
        val elementsCount = descriptor.elementsCount
        while (lowerReadMark != -1L) {
            val index = lowerReadMark.inv().countTrailingZeroBits()
            lowerReadMark = lowerReadMark or (1L shl index)

            if (!descriptor.isElementOptional(index)) {
                val elementDescriptor = descriptor.getElementDescriptor(index)
                val kind = elementDescriptor.kind
                if (kind == StructureKind.MAP || kind == StructureKind.LIST) {
                    return index
                } else if (elementDescriptor.isNullable) {
                    valueIsNull = true
                    return index
                }
            }
        }

        if (elementsCount > Long.SIZE_BITS) {
            val higherMarks = highReadMarks!!

            for (slot in higherMarks.indices) {
                // (slot + 1) because first element in high marks has index 64
                val slotOffset = (slot + 1) * Long.SIZE_BITS
                // store in a variable so as not to frequently use the array
                var mark = higherMarks[slot]

                while (mark != -1L) {
                    val indexInSlot = mark.inv().countTrailingZeroBits()
                    mark = mark or (1L shl indexInSlot)

                    val index = slotOffset + indexInSlot
                    if (!descriptor.isElementOptional(index)) {
                        val elementDescriptor = descriptor.getElementDescriptor(index)
                        val kind = elementDescriptor.kind
                        if (kind == StructureKind.MAP || kind == StructureKind.LIST) {
                            higherMarks[slot] = mark
                            return index
                        } else if (elementDescriptor.isNullable) {
                            higherMarks[slot] = mark
                            valueIsNull = true
                            return index
                        }
                    }
                }

                higherMarks[slot] = mark
            }
            return -1
        }
        return -1
    }

    private fun markElementAsRead(index: Int) {
        if (index < Long.SIZE_BITS) {
            lowerReadMark = lowerReadMark or (1L shl index)
        } else {
            val slot = (index / Long.SIZE_BITS) - 1
            val offsetInSlot = index % Long.SIZE_BITS
            highReadMarks!![slot] = highReadMarks[slot] or (1L shl offsetInSlot)
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (true) {
            val protoId = reader.readTag()
            if (protoId == -1) { // EOF
                val absenceIndex = findUnreadElementIndex()
                return if (absenceIndex == -1) {
                    CompositeDecoder.DECODE_DONE
                } else {
                    absenceIndex
                }
            }
            val index = getIndexByTag(protoId)
            if (index == -1) { // not found
                reader.skipElement()
            } else {
                markElementAsRead(index)
                return index
            }
        }
    }

    override fun decodeNotNullMark(): Boolean {
        return if (valueIsNull) {
            valueIsNull = false
            false
        } else {
            true
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
