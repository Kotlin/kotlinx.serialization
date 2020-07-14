/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.builtins

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*


/**
 * Serializer that encodes and decodes [Long] as its string representation.
 *
 * Intended to be used for interoperability with external clients (mainly JavaScript ones),
 * where numbers can't be parsed correctly if they exceed
 * [`abs(2^53-1)`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER).
 */
public object LongAsStringSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.serialization.LongAsStringSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Long {
        return decoder.decodeString().toLong()
    }
}
