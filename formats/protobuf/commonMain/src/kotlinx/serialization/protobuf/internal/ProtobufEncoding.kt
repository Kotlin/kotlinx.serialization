/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class)
package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*

import kotlinx.serialization.protobuf.*
import kotlin.jvm.*

internal open class ProtobufEncoder(
    @JvmField protected val proto: ProtoBuf,
    private val writer: ProtobufWriter,
    @JvmField protected val descriptor: SerialDescriptor
) : ProtobufTaggedEncoder() {
    @OptIn(ExperimentalSerializationApi::class) // KT-46731
    public override val serializersModule
        get() = proto.serializersModule

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = proto.encodeDefaults

    override fun beginCollection(
        descriptor: SerialDescriptor,
        collectionSize: Int
    ): CompositeEncoder = when (descriptor.kind) {
        StructureKind.LIST -> {
            val tag = currentTagOrDefault
            if (tag.isPacked && descriptor.getElementDescriptor(0).isPackable) {
                PackedArrayEncoder(proto, writer, currentTagOrDefault, descriptor)
            } else {
                if (tag == MISSING_TAG) {
                    writer.writeInt(collectionSize)
                }
                if (this.descriptor.kind == StructureKind.LIST && tag != MISSING_TAG && this.descriptor != descriptor) {
                    NestedRepeatedEncoder(proto, writer, tag, descriptor)
                } else {
                    RepeatedEncoder(proto, writer, tag, descriptor)
                }
            }
        }
        StructureKind.MAP -> {
            // Size and missing tag are managed by the implementation that delegated to the list
            MapRepeatedEncoder(proto, currentTag, writer, descriptor)
        }
        else -> throw SerializationException("This serial kind is not supported as collection: $descriptor")
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = when (descriptor.kind) {
        StructureKind.LIST -> {
            if (descriptor.getElementDescriptor(0).isPackable && currentTagOrDefault.isPacked) {
                PackedArrayEncoder(proto, writer, currentTagOrDefault, descriptor)
            } else {
                RepeatedEncoder(proto, writer, currentTagOrDefault, descriptor)
            }
        }
        StructureKind.CLASS, StructureKind.OBJECT, is PolymorphicKind -> {
            val tag = currentTagOrDefault
            if (tag == MISSING_TAG && descriptor == this.descriptor) this
            else ObjectEncoder(proto, currentTagOrDefault, writer, descriptor = descriptor)
        }
        StructureKind.MAP -> MapRepeatedEncoder(proto, currentTagOrDefault, writer, descriptor)
        else -> throw SerializationException("This serial kind is not supported as structure: $descriptor")
    }

    override fun encodeTaggedInt(tag: ProtoDesc, value: Int) {
        if (tag == MISSING_TAG) {
            writer.writeInt(value)
        } else {
            writer.writeInt(value, tag.protoId, tag.integerType)
        }
    }

    override fun encodeTaggedByte(tag: ProtoDesc, value: Byte) = encodeTaggedInt(tag, value.toInt())
    override fun encodeTaggedShort(tag: ProtoDesc, value: Short) = encodeTaggedInt(tag, value.toInt())
    override fun encodeTaggedBoolean(tag: ProtoDesc, value: Boolean) = encodeTaggedInt(tag, if (value) 1 else 0)
    override fun encodeTaggedChar(tag: ProtoDesc, value: Char) = encodeTaggedInt(tag, value.code)

    override fun encodeTaggedLong(tag: ProtoDesc, value: Long) {
        if (tag == MISSING_TAG) {
            writer.writeLong(value)
        } else {
            writer.writeLong(value, tag.protoId, tag.integerType)
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
        enumDescriptor: SerialDescriptor,
        ordinal: Int
    ) {
        if (tag == MISSING_TAG) {
            writer.writeInt(extractProtoId(enumDescriptor, ordinal, zeroBasedDefault = true))
        } else {
            writer.writeInt(
                extractProtoId(enumDescriptor, ordinal, zeroBasedDefault = true),
                tag.protoId,
                ProtoIntegerType.DEFAULT
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
        val mapEntrySerial = kotlinx.serialization.builtins.MapEntrySerializer(casted.keySerializer, casted.valueSerializer)
        SetSerializer(mapEntrySerial).serialize(this, (value as Map<*, *>).entries)
    }
}

private open class ObjectEncoder(
    proto: ProtoBuf,
    @JvmField protected val parentTag: ProtoDesc,
    @JvmField protected val parentWriter: ProtobufWriter,
    @JvmField protected val stream: ByteArrayOutput = ByteArrayOutput(),
    descriptor: SerialDescriptor
) : ProtobufEncoder(proto, ProtobufWriter(stream), descriptor) {
    override fun endEncode(descriptor: SerialDescriptor) {
        // TODO this is exactly the lookahead scenario
        if (parentTag != MISSING_TAG) {
            parentWriter.writeOutput(stream, parentTag.protoId)
        } else {
            parentWriter.writeOutput(stream)
        }
    }
}

private class MapRepeatedEncoder(
    proto: ProtoBuf,
    parentTag: ProtoDesc,
    parentWriter: ProtobufWriter,
    descriptor: SerialDescriptor
) : ObjectEncoder(proto, parentTag, parentWriter, descriptor = descriptor) {
    override fun SerialDescriptor.getTag(index: Int): ProtoDesc =
        if (index % 2 == 0) ProtoDesc(1, (parentTag.integerType))
        else ProtoDesc(2, (parentTag.integerType))
}

private class RepeatedEncoder(
    proto: ProtoBuf,
    writer: ProtobufWriter,
    @JvmField val curTag: ProtoDesc,
    descriptor: SerialDescriptor
) : ProtobufEncoder(proto, writer, descriptor) {
    override fun SerialDescriptor.getTag(index: Int) = curTag
}

internal open class NestedRepeatedEncoder(
    proto: ProtoBuf,
    @JvmField val writer: ProtobufWriter,
    @JvmField val curTag: ProtoDesc,
    descriptor: SerialDescriptor,
    @JvmField val stream: ByteArrayOutput = ByteArrayOutput()
) : ProtobufEncoder(proto, ProtobufWriter(stream), descriptor) {
    // all elements always have id = 1
    @OptIn(ExperimentalSerializationApi::class) // KT-46731
    override fun SerialDescriptor.getTag(index: Int) = ProtoDesc(1, ProtoIntegerType.DEFAULT)

    override fun endEncode(descriptor: SerialDescriptor) {
        writer.writeOutput(stream, curTag.protoId)
    }
}
