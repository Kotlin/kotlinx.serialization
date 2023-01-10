/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration


@PublishedApi
internal object DurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.time.Duration", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeString(value.toIsoString())
    }

    override fun deserialize(decoder: Decoder): Duration {
        return Duration.parseIsoString(decoder.decodeString())
    }
}

@PublishedApi
internal object NothingSerializer : KSerializer<Nothing> {
    override val descriptor: SerialDescriptor = NothingSerialDescriptor

    override fun serialize(encoder: Encoder, value: Nothing) {
        throw SerializationException("'kotlin.Nothing' cannot be serialized")
    }

    override fun deserialize(decoder: Decoder): Nothing {
        throw SerializationException("'kotlin.Nothing' does not have instances")
    }
}
