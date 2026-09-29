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
    override val descriptor: SerialDescriptor = SerialDescriptor(
        "kotlinx.serialization.protobuf.ProtoUnknownFieldHolder",
        ByteArraySerializer().descriptor,
    )

    override fun deserialize(decoder: Decoder): ProtoUnknownFieldHolder {
        return decoder.asProtobufDecoder().decodeStructure(descriptor) {
            this.asProtobufDecoder().let {
                ProtoUnknownFieldHolder(readRawFieldBytes(it, it.currentTag))
            }
        }
    }

    override fun serialize(encoder: Encoder, value: ProtoUnknownFieldHolder) {
        encoder.asProtobufEncoder().writeRawBytes(value.fields)
    }
}

private fun Decoder.asProtobufDecoder(): ProtobufDecoder = this as? ProtobufDecoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Protobuf format." +
            "Expected Decoder to be ProtobufDecoder, got ${this::class}"
    )

private fun CompositeDecoder.asProtobufDecoder(): ProtobufDecoder = this as? ProtobufDecoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Protobuf format." +
            "Expected Decoder to be ProtobufDecoder, got ${this::class}"
    )

private fun Encoder.asProtobufEncoder() = this as? ProtobufEncoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Protobuf format." +
            "Expected Encoder to be ProtobufEncoder, got ${this::class}"
    )

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
