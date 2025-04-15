/*
 * Copyright 2025-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.builtins

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Serializer that encodes and decodes [Instant] as its second and nanosecond components of the Unix time.
 *
 * JSON example: `{"epochSeconds":1607505416,"nanosecondsOfSecond":124000}`.
 */
@ExperimentalTime
public object InstantComponentSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlinx.serialization.InstantComponentSerializer") {
            element<Long>("epochSeconds")
            element<Long>("nanosecondsOfSecond", isOptional = true)
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Instant =
        decoder.decodeStructure(descriptor) {
            var epochSecondsNotSeen = true
            var epochSeconds: Long = 0
            var nanosecondsOfSecond = 0
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> {
                        epochSecondsNotSeen = false
                        epochSeconds = decodeLongElement(descriptor, 0)
                    }
                    1 -> nanosecondsOfSecond = decodeIntElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            if (epochSecondsNotSeen) throw MissingFieldException(
                missingField = "epochSeconds",
                serialName = descriptor.serialName
            )
            Instant.fromEpochSeconds(epochSeconds, nanosecondsOfSecond)
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.epochSeconds)
            if (value.nanosecondsOfSecond != 0) {
                encodeIntElement(descriptor, 1, value.nanosecondsOfSecond)
            }
        }
    }

}
