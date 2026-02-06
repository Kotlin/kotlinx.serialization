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
    internal val fieldsSerializer = ProtoFieldSerializer

    override val descriptor: SerialDescriptor
        get() = UnknownFieldsDescriptor(fieldsSerializer.descriptor)

    override fun deserialize(decoder: Decoder): ProtoUnknownFieldHolder {
        if (decoder is ProtobufDecoder) {
            return decoder.decodeStructure(descriptor) {
                ProtoUnknownFieldHolder(fieldsSerializer.deserializeComposite(this))
            }
        }
        return ProtoUnknownFieldHolder.Empty
    }

    override fun serialize(encoder: Encoder, value: ProtoUnknownFieldHolder) {
        if (encoder is ProtobufEncoder) {
            value.fields.forEach {
                fieldsSerializer.serialize(encoder, it)
            }
        }
    }
}

internal object ProtoFieldSerializer : KSerializer<ProtoField> {
    private val delegate = ByteArraySerializer()

    override val descriptor: SerialDescriptor
        get() = UnknownFieldsDescriptor(delegate.descriptor)

    fun deserializeComposite(compositeDecoder: CompositeDecoder): ProtoField {
        if (compositeDecoder is ProtobufDecoder) {
            return deserialize(compositeDecoder)
        }
        return ProtoField.Empty
    }

    override fun deserialize(decoder: Decoder): ProtoField {
        if (decoder is ProtobufDecoder) {
            return deserialize(decoder, decoder.currentTag)
        }
        return ProtoField.Empty
    }

    internal fun deserialize(protobufDecoder: ProtobufDecoder, currentTag: ProtoDesc): ProtoField {
        if (currentTag != MISSING_TAG) {
            val id = currentTag.protoId
            val type = protobufDecoder.currentType
            val data = protobufDecoder.decodeRawElement()
            val field = ProtoField(
                id = id,
                wireType = type,
                data = data,
            )
            return field
        }
        return ProtoField.Empty
    }

    override fun serialize(encoder: Encoder, value: ProtoField) {
        if (encoder is ProtobufEncoder) {
            encoder.encodeRawElement(value.id, value.wireType, value.data)
        }
    }
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
