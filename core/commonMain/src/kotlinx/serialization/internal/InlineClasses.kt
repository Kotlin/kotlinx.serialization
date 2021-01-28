/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@PublishedApi
@ExperimentalSerializationApi
@ExperimentalUnsignedTypes
internal object UIntSerializer : KSerializer<UInt> {
    override val descriptor: SerialDescriptor = InlinePrimitiveDescriptor("kotlin.UInt", Int.serializer())

    override fun serialize(encoder: Encoder, value: UInt) {
        encoder.encodeInline(descriptor)?.encodeInt(value.toInt())
    }

    override fun deserialize(decoder: Decoder): UInt {
        return decoder.decodeInline(descriptor).decodeInt().toUInt()
    }
}

@PublishedApi
@ExperimentalSerializationApi
@ExperimentalUnsignedTypes
internal object ULongSerializer : KSerializer<ULong> {
    override val descriptor: SerialDescriptor = InlinePrimitiveDescriptor("kotlin.ULong", Long.serializer())

    override fun serialize(encoder: Encoder, value: ULong) {
        encoder.encodeInline(descriptor)?.encodeLong(value.toLong())
    }

    override fun deserialize(decoder: Decoder): ULong {
        return decoder.decodeInline(descriptor).decodeLong().toULong()
    }
}

@PublishedApi
@ExperimentalSerializationApi
@ExperimentalUnsignedTypes
internal object UByteSerializer : KSerializer<UByte> {
    override val descriptor: SerialDescriptor = InlinePrimitiveDescriptor("kotlin.UByte", Byte.serializer())

    override fun serialize(encoder: Encoder, value: UByte) {
        encoder.encodeInline(descriptor)?.encodeByte(value.toByte())
    }

    override fun deserialize(decoder: Decoder): UByte {
        return decoder.decodeInline(descriptor).decodeByte().toUByte()
    }
}

@PublishedApi
@ExperimentalSerializationApi
@ExperimentalUnsignedTypes
internal object UShortSerializer : KSerializer<UShort> {
    override val descriptor: SerialDescriptor = InlinePrimitiveDescriptor("kotlin.UShort", Short.serializer())

    override fun serialize(encoder: Encoder, value: UShort) {
        encoder.encodeInline(descriptor)?.encodeShort(value.toShort())
    }

    override fun deserialize(decoder: Decoder): UShort {
        return decoder.decodeInline(descriptor).decodeShort().toUShort()
    }
}
