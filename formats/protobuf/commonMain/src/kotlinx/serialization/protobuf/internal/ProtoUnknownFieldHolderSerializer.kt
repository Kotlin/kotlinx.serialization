/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.protobuf.*

internal object ProtoUnknownFieldHolderSerializer : KSerializer<ProtoUnknownFieldHolder> {
    override val descriptor: SerialDescriptor = UnknownFieldsDescriptor(ByteArraySerializer().descriptor)

    override fun deserialize(decoder: Decoder): ProtoUnknownFieldHolder {
        if (decoder is ProtobufDecoder) {
            return decoder.decodeStructure(descriptor) {
                ProtoUnknownFieldHolder(readRawFieldBytes(this))
            }
        }
        return ProtoUnknownFieldHolder.Empty
    }

    override fun serialize(encoder: Encoder, value: ProtoUnknownFieldHolder) {
        if (encoder is ProtobufEncoder) {
            value.fields.forEach { field ->
                encoder.writeRawBytes(field)
            }
        }
    }
}

/**
 * Reads the complete wire format bytes (tag + value) for the current field
 * from [decoder]. The [currentTag] provides the proto id for the unknown field.
 */
internal fun readRawFieldBytes(decoder: ProtobufDecoder, currentTag: ProtoDesc): ByteArray {
    if (currentTag == MISSING_TAG) throw IllegalStateException("No valid proto tag while reading raw field bytes.")
    val id = currentTag.protoId
    val wireType = decoder.currentType
    val valueBytes = decoder.decodeRawElement()

    val output = ByteArrayOutput()
    val writer = ProtobufWriter(output)
    when (wireType) {
        ProtoWireType.VARINT, ProtoWireType.i64, ProtoWireType.i32 -> {
            writer.writeRawBytes(valueBytes, wireType.wireIntWithTag(id))
        }
        ProtoWireType.SIZE_DELIMITED -> {
            writer.writeBytes(valueBytes, id)
        }
        ProtoWireType.INVALID -> {}
    }
    return output.toByteArray()
}

/**
 * Reads the complete wire format bytes (tag + value) for the current field
 * from [compositeDecoder] using its current tag.
 */
internal fun readRawFieldBytes(compositeDecoder: CompositeDecoder): ByteArray {
    if (compositeDecoder is ProtobufDecoder) {
        return readRawFieldBytes(compositeDecoder, compositeDecoder.currentTag)
    }
    throw ClassCastException("Calling readRawFieldBytes is supported only for ProtobufDecoder")
}

internal class UnknownFieldsDescriptor(private val original: SerialDescriptor) : SerialDescriptor by original {
    override val serialName: String
        get() = "UnknownProtoFieldsHolder[${original.serialName}]"

    override fun equals(other: Any?): Boolean {
        return other is UnknownFieldsDescriptor && other.original == original
    }

    override fun hashCode(): Int {
        return original.hashCode()
    }
}
