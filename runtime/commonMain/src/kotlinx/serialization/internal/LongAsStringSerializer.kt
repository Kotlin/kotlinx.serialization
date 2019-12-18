/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.StringSerializer

object LongAsStringSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = StringSerializer.descriptor

    override fun serialize(encoder: Encoder, obj: Long) {
        encoder.encodeString(obj.toString())
    }

    override fun deserialize(decoder: Decoder): Long {
        return decoder.decodeString().toLong()
    }
}
