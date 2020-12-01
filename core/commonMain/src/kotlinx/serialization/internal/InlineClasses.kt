/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalUnsignedTypes::class)

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

public fun UInt.Companion.serializer(): KSerializer<UInt> = UIntSerializer

@PublishedApi
internal object UIntSerializer : KSerializer<UInt> {
    override val descriptor: SerialDescriptor = InlinePrimitiveDescriptor("kotlin.UInt", Int.serializer())

    override fun serialize(encoder: Encoder, obj: UInt) {
        encoder.encodeInline(descriptor)?.encodeInt(obj.toInt())
    }

    override fun deserialize(decoder: Decoder): UInt {
        return decoder.decodeInline(descriptor).decodeInt().toUInt()
    }
}
