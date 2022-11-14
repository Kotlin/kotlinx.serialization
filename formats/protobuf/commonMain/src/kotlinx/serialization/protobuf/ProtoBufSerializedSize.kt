package kotlinx.serialization.protobuf

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.builtins.MapEntrySerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.internal.MapLikeSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.protobuf.internal.*
import kotlin.jvm.JvmField

/**
 * Returns the number of bytes required to encode this [message][value]. The size is computed on the first call
 * and memoized.
 */
@ExperimentalSerializationApi
public fun <T : Any> ProtoBuf.getOrComputeSerializedSize(serializer: SerializationStrategy<T>, value: T): Int {
    val key = value.serializedSizeCacheKey
    return memoizedSerializedSizes.getOrPut(serializer.descriptor, key) {
        val calculator = ProtoBufSerializedSizeCalculator(this, serializer.descriptor)
        calculator.encodeSerializableValue(serializer, value)
        calculator.serializedSize
    }
}

/*
 * This cache probably is better placed in `Protobuf` class, to associate each instance with different cache. Though,
 * this change needs some discussion first.
 */
//TODO: needs @ThreadLocal?
private val memoizedSerializedSizes = createSerializedSizeCache()

internal expect fun createSerializedSizeCache(): SerializedSizeCache

/**
 * A key for associating an instance of class with a given [SerialDescriptor].
 */
internal typealias SerializedSizeCacheKey = Int

// Returns the object's hashcode, which acts as a key for `SerializedSizeCache`.
private val Any.serializedSizeCacheKey: SerializedSizeCacheKey get() = this.hashCode()

internal typealias SerializedData = Map<SerializedSizeCacheKey, Int>

/**
 * A storage to memoize a computed `serializedSize`.
 */
// Note: js-impl & native-impl are not based on concurrent-safe structures.
internal interface SerializedSizeCache {
    /**
     * Returns the `serializedSize` associated with the given [key] and [descriptor], if found else null.
     */
    operator fun get(descriptor: SerialDescriptor, key: SerializedSizeCacheKey): Int?

    /**
     * Sets the `serializedSize` and associates it with the given [key] and [descriptor].
     */
    operator fun set(descriptor: SerialDescriptor, key: SerializedSizeCacheKey, serializedSize: Int)
}

internal fun SerializedSizeCache.getOrPut(
    descriptor: SerialDescriptor,
    key: SerializedSizeCacheKey,
    defaultValue: () -> Int
): Int {
    get(descriptor, key)?.let { return it }
    val value = defaultValue()
    set(descriptor, key, value)
    return value
}

/**
 * Internal helper to pass around serialized size.
 * Should not be leaked to the outside world.
 */
internal data class SerializedSizePointer(var value: Int)

/**
 * A calculator to compute the required bytes a protobuf message needs to be encoded. The core idea is to compute
 * the required bytes for each field separately, and accumulate them in the end by storing the resulted bytes in
 * [serializedSize].
 *
 * For more details see [Protobuf-encoding](https://protobuf.dev/programming-guides/encoding/).
 */
@ExperimentalSerializationApi
private open class ProtoBufSerializedSizeCalculator(
    private val proto: ProtoBuf,
    val descriptor: SerialDescriptor,
    /**
     * A pointer for [serializedSize], to allow updating size in nested calls.
     */
    private val serializedSizePointer: SerializedSizePointer = SerializedSizePointer(-1)
) : ProtobufTaggedEncoder() {
    /*
     * Accumulates the bytes, which are required to encode a "value". Each calculator stores the result in this var.
     */
    var serializedSize
        get() = serializedSizePointer.value
        set(value) {
            serializedSizePointer.value = value
        }

    override val serializersModule: SerializersModule get() = proto.serializersModule

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = proto.encodeDefaults

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        return when (descriptor.kind) {
            StructureKind.LIST -> {
                val tag = currentTagOrDefault
                if (tag.isPacked && descriptor.getElementDescriptor(0).isPackable) {
                    PackedArrayCalculator(proto, currentTagOrDefault, descriptor, serializedSizePointer)
                } else {
                    if (serializedSize == -1) serializedSize = 0
                    if (this is RepeatedCalculator) {
                        this
                    } else {
                        RepeatedCalculator(proto, tag, descriptor, serializedSizePointer)
                    }
                }
            }

            StructureKind.MAP -> MapRepeatedCalculator(proto, currentTagOrDefault, descriptor, serializedSizePointer)
            else -> throw SerializationException("This serial kind is not supported as collection: $descriptor")
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (serializedSize == -1) serializedSize = 0
        // delegate to proper calculator, e.g. class, map, list, etc.
        return when (descriptor.kind) {
            StructureKind.LIST -> {
                if (descriptor.getElementDescriptor(0).isPackable && currentTagOrDefault.isPacked) {
                    PackedArrayCalculator(proto, currentTagOrDefault, descriptor, serializedSizePointer)
                } else {
                    RepeatedCalculator(proto, currentTagOrDefault, descriptor, serializedSizePointer)
                }
            }

            StructureKind.CLASS, StructureKind.OBJECT, is PolymorphicKind -> this
            StructureKind.MAP -> MapRepeatedCalculator(proto, currentTagOrDefault, descriptor, serializedSizePointer)
            else -> throw SerializationException("This serial kind is not supported as structure: $descriptor")
        }
    }

    override fun endEncode(descriptor: SerialDescriptor) {}

    override fun SerialDescriptor.getTag(index: Int): ProtoDesc = extractParameters(index)

    /*
     * Note: This API is meant to be used once for every instance of `this` class. This is because, every calculator
     * stores its resulted size in "serializedSize" and it does not reset in the end.
     */
    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        return when {
            serializer is MapLikeSerializer<*, *, *, *> ->
                computeMapSize(serializer as SerializationStrategy<T>, value)

            serializer.descriptor == ByteArraySerializer().descriptor -> computeByteArraySize(value as ByteArray)

            // This path is specifically only for computing size of "Messages" (objects).
            (serializer.descriptor.kind is StructureKind.CLASS ||
                    serializer.descriptor.kind is PolymorphicKind ||
                    serializer.descriptor.kind is StructureKind.OBJECT) &&
                    serializer.descriptor.kind !is PrimitiveKind &&
                    serializer.descriptor != this.descriptor -> computeMessageSize(serializer, value)

            serializer.descriptor != this.descriptor &&
                    serializer.descriptor.kind is StructureKind.LIST &&
                    serializer.descriptor.isChildDescriptorPrimitive() &&
                    serializer.descriptor.isNotPacked() // packed fields are computed through different path.
            -> computeRepeatedPrimitive(serializer, value)

            serializer.descriptor != this.descriptor &&
                    serializer.descriptor.kind is StructureKind.LIST &&
                    // ensure child is not primitive, since repeated primitives are computed through different path.
                    serializer.descriptor.isNotChildDescriptorPrimitive()
            -> computeRepeatedMessageSize(serializer, value)


            else -> serializer.serialize(this, value)
        }
    }

    private fun SerialDescriptor.isNotPacked(): Boolean =
        !(getElementDescriptor(0).isPackable && currentTagOrDefault.isPacked)

    override fun encodeTaggedInt(tag: ProtoDesc, value: Int) {
        serializedSize += if (tag == MISSING_TAG) {
            computeIntSizeNoTag(value, tag.integerType)
        } else {
            computeIntSize(value, tag.protoId, tag.integerType)
        }
    }

    override fun encodeTaggedLong(tag: ProtoDesc, value: Long) {
        serializedSize += if (tag == MISSING_TAG) {
            computeLongSizeNoTag(value, tag.integerType)
        } else {
            computeLongSize(value, tag.protoId, tag.integerType)
        }
    }

    override fun encodeTaggedByte(tag: ProtoDesc, value: Byte) {
        serializedSize += if (tag == MISSING_TAG) {
            computeIntSizeNoTag(value.toInt(), tag.integerType)
        } else {
            computeIntSize(value.toInt(), tag.protoId, tag.integerType)
        }

    }

    override fun encodeTaggedShort(tag: ProtoDesc, value: Short) {
        serializedSize += if (tag == MISSING_TAG) {
            computeIntSizeNoTag(value.toInt(), tag.integerType)
        } else {
            computeIntSize(value.toInt(), tag.protoId, tag.integerType)
        }
    }

    override fun encodeTaggedFloat(tag: ProtoDesc, value: Float) {
        serializedSize += if (tag == MISSING_TAG) {
            getFixed32SizeNoTag()
        } else {
            computeFloatSize(tag.protoId)
        }
    }

    override fun encodeTaggedDouble(tag: ProtoDesc, value: Double) {
        serializedSize += if (tag == MISSING_TAG) {
            getFixed64SizeNoTag()
        } else {
            computeDoubleSize(tag.protoId)
        }
    }

    override fun encodeTaggedBoolean(tag: ProtoDesc, value: Boolean) {
        serializedSize += if (tag == MISSING_TAG) {
            1
        } else {
            computeBooleanSize(tag.protoId)
        }
    }

    override fun encodeTaggedChar(tag: ProtoDesc, value: Char) {
        serializedSize += if (tag == MISSING_TAG) {
            computeIntSizeNoTag(value.code, tag.integerType)
        } else {
            computeIntSize(value.code, tag.protoId, tag.integerType)
        }
    }

    override fun encodeTaggedString(tag: ProtoDesc, value: String) {
        serializedSize += if (tag == MISSING_TAG) {
            computeStringSizeNoTag(value)
        } else {
            computeStringSize(value, tag.protoId)
        }
    }

    override fun encodeTaggedEnum(tag: ProtoDesc, enumDescriptor: SerialDescriptor, ordinal: Int) {
        serializedSize += if (tag == MISSING_TAG) {
            computeEnumSizeNoTag(extractProtoId(enumDescriptor, ordinal, zeroBasedDefault = true))
        } else {
            computeEnumSize(
                extractProtoId(enumDescriptor, ordinal, zeroBasedDefault = true),
                tag.protoId,
                ProtoIntegerType.DEFAULT
            )
        }
    }

    private fun computeByteArraySize(value: ByteArray) {
        val tag = popTagOrDefault()
        serializedSize += if (tag == MISSING_TAG) {
            computeByteArraySizeNoTag(value)
        } else {
            computeByteArraySize(value, tag.protoId)
        }
    }

    private fun <T> computeMessageSize(serializer: SerializationStrategy<T>, value: T) {
        val tag = currentTagOrDefault
        val size = proto.computeMessageSize(serializer, value, tag.protoId)
        serializedSize += size
    }

    private fun <T> computeRepeatedMessageSize(serializer: SerializationStrategy<T>, value: T) {
        val tag = popTag() // tag is required for calculating repeated objects
        // each object in collection should be calculated with the same tag.
        val calculator = RepeatedCalculator(proto, tag, serializer.descriptor)
        calculator.encodeSerializableValue(serializer, value)
        serializedSize += calculator.serializedSize
    }

    private fun <T> computeRepeatedPrimitive(serializer: SerializationStrategy<T>, value: T) {
        val calculator = PrimitiveRepeatedCalculator(proto, currentTagOrDefault, serializer.descriptor)
        calculator.encodeSerializableValue(serializer, value)
        serializedSize += calculator.serializedSize
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> computeMapSize(serializer: SerializationStrategy<T>, value: T) {
        // maps are encoded as collection of map entries, not merged collection of key-values
        val casted = (serializer as MapLikeSerializer<Any?, Any?, T, *>)
        val mapEntrySerial = MapEntrySerializer(casted.keySerializer, casted.valueSerializer)
        val entries = (value as Map<*, *>).entries
        // calculate each entry separately through computeMessageSize(). We do not need to use computeRepeatedMessageSize(),
        // as we already have our message (entry) and there is no need to unwrap the collection.
        for (entry in entries) computeMessageSize(mapEntrySerial, entry)
    }
}

@ExperimentalSerializationApi
private open class ObjectSizeCalculator(
    proto: ProtoBuf,
    @JvmField protected val parentTag: ProtoDesc,
    descriptor: SerialDescriptor,
    serializedSizePointer: SerializedSizePointer = SerializedSizePointer(-1)
) : ProtoBufSerializedSizeCalculator(proto, descriptor, serializedSizePointer)

@ExperimentalSerializationApi
private open class RepeatedCalculator(
    proto: ProtoBuf,
    @JvmField val curTag: ProtoDesc,
    descriptor: SerialDescriptor,
    serializedWrapper: SerializedSizePointer = SerializedSizePointer(-1)
) : ObjectSizeCalculator(proto, curTag, descriptor, serializedWrapper) {
    init {
        if (serializedSize == -1) serializedSize = 0
    }

    override fun SerialDescriptor.getTag(index: Int) = curTag
}

/*
 * Helper class to compute repeated primitives. The mental model is similar to this:
 * tagSize = computeTagSize(tag)
 * size = tagSize + computeElementSizeNoTag(type, value)
 *
 * To compute size we need 2 things;
 * 1) compute elements without their tag.
 * 2) compute tags for every element separately.
 */
@ExperimentalSerializationApi
private class PrimitiveRepeatedCalculator(
    proto: ProtoBuf,
    // The actual tag of field.
    curTag: ProtoDesc,
    descriptor: SerialDescriptor,
    serializedSizePointer: SerializedSizePointer = SerializedSizePointer(-1)
) : RepeatedCalculator(proto, curTag, descriptor, serializedSizePointer) {

    // Triggers computers to choose `MISSING_TAG` path
    override fun SerialDescriptor.getTag(index: Int): ProtoDesc = MISSING_TAG

    /*
     * Compute tagSize for every primitive and then delegate computing.
     */

    override fun encodeTaggedBoolean(tag: ProtoDesc, value: Boolean) {
        if (curTag != MISSING_TAG) serializedSize += computeTagSize(curTag.protoId)
        super.encodeTaggedBoolean(tag, value)
    }

    override fun encodeTaggedByte(tag: ProtoDesc, value: Byte) {
        if (curTag != MISSING_TAG) serializedSize += computeTagSize(curTag.protoId)
        super.encodeTaggedByte(tag, value)
    }

    override fun encodeTaggedInt(tag: ProtoDesc, value: Int) {
        if (curTag != MISSING_TAG) serializedSize += computeTagSize(curTag.protoId)
        super.encodeTaggedInt(tag, value)
    }

    override fun encodeTaggedLong(tag: ProtoDesc, value: Long) {
        if (curTag != MISSING_TAG) serializedSize += computeTagSize(curTag.protoId)
        super.encodeTaggedLong(tag, value)
    }

    override fun encodeTaggedShort(tag: ProtoDesc, value: Short) {
        if (curTag != MISSING_TAG) serializedSize += computeTagSize(curTag.protoId)
        super.encodeTaggedShort(tag, value)
    }
}

@ExperimentalSerializationApi
private class MapRepeatedCalculator(
    proto: ProtoBuf,
    parentTag: ProtoDesc,
    descriptor: SerialDescriptor,
    serializedSizePointer: SerializedSizePointer
) : ObjectSizeCalculator(proto, parentTag, descriptor, serializedSizePointer) {
    init {
        if (serializedSize == -1) serializedSize = 0
    }

    override fun SerialDescriptor.getTag(index: Int): ProtoDesc =
        if (index % 2 == 0) ProtoDesc(1, (parentTag.integerType))
        else ProtoDesc(2, (parentTag.integerType))
}

@OptIn(ExperimentalSerializationApi::class)
private open class NestedRepeatedCalculator(
    proto: ProtoBuf,
    @JvmField val curTag: ProtoDesc,
    descriptor: SerialDescriptor,
    serializedSizePointer: SerializedSizePointer
) : ObjectSizeCalculator(proto, curTag, descriptor, serializedSizePointer) {
    init {
        if (serializedSize == -1) serializedSize = 0
    }

    // all elements always have id = 1
    override fun SerialDescriptor.getTag(index: Int) = ProtoDesc(1, ProtoIntegerType.DEFAULT)

}

@OptIn(ExperimentalSerializationApi::class)
private class PackedArrayCalculator(
    proto: ProtoBuf,
    curTag: ProtoDesc,
    descriptor: SerialDescriptor,
    // Parent size to be updated after computing the size.
    private val parentSerializedSize: SerializedSizePointer
) : NestedRepeatedCalculator(
    proto,
    curTag,
    descriptor,
    /* SerializedSize to be used as result container. The final tag is computed through this result. */
    SerializedSizePointer(-1)
) {
    // Triggers computers to choose `MISSING_TAG` path
    override fun SerialDescriptor.getTag(index: Int): ProtoDesc = MISSING_TAG

    override fun endEncode(descriptor: SerialDescriptor) {
        if (serializedSize == 0) return // empty collection
        serializedSize += computeUInt32SizeNoTag(serializedSize) // compute varint based on result of "serializedSize".
        // Since repeated fields are encoded as single LEN record that contains each element concatenated, then tag
        // should be computed once for whole message.
        val tag = computeTagSize(curTag.protoId)
        parentSerializedSize.value += tag + serializedSize // update parentSize
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        throw SerializationException("Packing only supports primitive number types")
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        throw SerializationException("Packing only supports primitive number types")
    }

    override fun encodeTaggedString(tag: ProtoDesc, value: String) {
        throw SerializationException("Packing only supports primitive number types")
    }
}

// computers

@OptIn(ExperimentalSerializationApi::class)
private fun computeLongSize(value: Long, tag: Int, format: ProtoIntegerType): Int {
    val tagSize = computeTagSize(tag)
    return tagSize + computeLongSizeNoTag(value, format)
}

@OptIn(ExperimentalSerializationApi::class)
private fun computeLongSizeNoTag(value: Long, format: ProtoIntegerType): Int {
    return when (format) {
        ProtoIntegerType.DEFAULT -> computeInt64SizeNoTag(value)
        ProtoIntegerType.SIGNED -> computeSInt64SizeNoTag(value)
        ProtoIntegerType.FIXED -> getFixed64SizeNoTag()
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun computeIntSize(value: Int, tag: Int, format: ProtoIntegerType): Int {
    val tagSize = computeTagSize(tag)
    return tagSize + computeIntSizeNoTag(value, format)
}

@OptIn(ExperimentalSerializationApi::class)
private fun computeIntSizeNoTag(value: Int, format: ProtoIntegerType): Int {
    return when (format) {
        //TODO: ProtobufWriter actually serializes default as varint64, should we align?
        ProtoIntegerType.DEFAULT -> computeInt32SizeNoTag(value)
        ProtoIntegerType.SIGNED -> computeSInt32SizeNoTag(value)
        ProtoIntegerType.FIXED -> getFixed32SizeNoTag()
    }
}

private fun computeFloatSize(tag: Int): Int {
    val tagSize = computeTagSize(tag)
    // floats have wire type of `I32` which means the size is fixed
    return tagSize + getFixed32SizeNoTag()
}

private fun computeDoubleSize(tag: Int): Int {
    val tagSize = computeTagSize(tag)
    // doubles have wire type of `I64` which means the size is fixed
    return tagSize + getFixed64SizeNoTag()
}

/*
 * Booleans encode as either `00` or `01`, per proto-spec.
 */
private fun computeBooleanSize(tag: Int): Int {
    val tagSize = computeTagSize(tag)
    return tagSize + 1
}

private fun computeStringSize(value: String, tag: Int): Int {
    val tagSize = computeTagSize(tag)
    return tagSize + computeStringSizeNoTag(value)
}

/*
 * Enums are encoded as `int32` per proto-spec.
 */
@OptIn(ExperimentalSerializationApi::class)
private fun computeEnumSize(value: Int, tag: Int, format: ProtoIntegerType): Int = computeIntSize(value, tag, format)

private fun computeByteArraySize(value: ByteArray, tag: Int): Int {
    val tagSize = computeTagSize(tag)
    return tagSize + computeByteArraySizeNoTag(value)
}

@OptIn(ExperimentalSerializationApi::class)
private fun <T> ProtoBuf.computeMessageSize(
    serializer: SerializationStrategy<T>,
    value: T,
    tag: Int
): Int {
    val tagSize = computeTagSize(tag)
    return tagSize + computeMessageSizeNoTag(serializer, value)
}

@OptIn(ExperimentalSerializationApi::class)
private fun <T> ProtoBuf.computeMessageSizeNoTag(serializer: SerializationStrategy<T>, value: T): Int =
    computeLengthDelimitedFieldSize(computeSerializedMessageSize(serializer, value))

@OptIn(ExperimentalSerializationApi::class)
private fun <T> ProtoBuf.computeSerializedMessageSize(serializer: SerializationStrategy<T>, value: T): Int {
    val calculator = ProtoBufSerializedSizeCalculator(this, serializer.descriptor)
    calculator.encodeSerializableValue(serializer, value)
    return calculator.serializedSize
}

// computers without tag

private fun computeEnumSizeNoTag(value: Int): Int = computeInt32SizeNoTag(value)

private fun computeByteArraySizeNoTag(value: ByteArray): Int = computeLengthDelimitedFieldSize(value.size)

private fun computeStringSizeNoTag(value: String): Int {
    // java's implementation uses a custom method for some optimizations.
    return computeLengthDelimitedFieldSize(value.length)
}

private fun computeLengthDelimitedFieldSize(length: Int): Int = computeUInt32SizeNoTag(length) + length

//TODO: should this also be named "compute" for consistency? It does not involve computing though.
private fun getFixed64SizeNoTag(): Int = FIXED64_SIZE
private fun computeSInt64SizeNoTag(value: Long): Int = computeUInt64SizeNoTag(encodeZigZag64(value))
private fun computeInt64SizeNoTag(value: Long): Int = computeUInt64SizeNoTag(value)

//TODO: should this also be named "compute" for consistency? It does not involve computing though.
private fun getFixed32SizeNoTag() = FIXED32_SIZE
private fun computeSInt32SizeNoTag(value: Int) = computeUInt32SizeNoTag((encodeZigZag32(value)))
private fun computeInt32SizeNoTag(value: Int) =
    if (value >= 0) computeUInt32SizeNoTag(value) else MAX_VARINT_SIZE

/** Compute the number of bytes that would be needed to encode an uint32 field. */
private fun computeUInt32SizeNoTag(value: Int): Int = when {
    value and (0.inv() shl 7) == 0 -> 1
    value and (0.inv() shl 14) == 0 -> 2
    value and (0.inv() shl 21) == 0 -> 3
    value and (0.inv() shl 28) == 0 -> 4
    else -> 5 // max varint32 size
}

/** Compute the number of bytes that would be needed to encode an uint64 field. */
private fun computeUInt64SizeNoTag(value: Long): Int {
    var _value = value
    // handle first two most common cases
    if ((_value and (0L.inv() shl 7)) == 0L) {
        return 1
    }
    if (_value < 0L) {
        // To encode a negative number all ten bytes must be used.
        return 10
    }
    // rest cases
    var size = 2
    if ((_value and (0L.inv() shl 35)) != 0L) {
        size += 4
        _value = value ushr 28
    }
    if ((_value and (0L.inv() shl 21)) != 0L) {
        size += 2
        _value = value ushr 14
    }
    if ((_value and (0L.inv() shl 14)) != 0L) {
        size += 1
    }
    return size
}

// helpers

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.isChildDescriptorPrimitive(): Boolean {
    val child = runCatching { this.getElementDescriptor(0) }.getOrElse { return false }
    return child.kind is PrimitiveKind
}

private fun SerialDescriptor.isNotChildDescriptorPrimitive(): Boolean = !isChildDescriptorPrimitive()

// per protobuf spec 1-10 bytes
private const val MAX_VARINT_SIZE = 10

// after decoding the varint representing a field, the low 3 bits tell us the wire type,
// and the rest of the integer tells us the field number.
private const val TAG_TYPE_BITS = 3

/**
 * See [Scalar type values](https://developers.google.com/protocol-buffers/docs/proto#scalar).
 */

private const val FIXED32_SIZE = 4
private const val FIXED64_SIZE = 8

// tag utils

// proto-spec: The “tag” of a record is encoded as a varint formed from the field number and the wire type (0).
private fun computeTagSize(protoId: Int): Int = computeUInt32SizeNoTag(makeTag(protoId, 0))
private fun makeTag(protoId: Int, wireType: Int): Int = protoId shl TAG_TYPE_BITS or wireType

// stream utils

private fun encodeZigZag64(value: Long): Long = (value shl 1) xor (value shr 63)

private fun encodeZigZag32(value: Int): Int = ((value shl 1) xor (value shr 31))