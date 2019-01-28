/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.java

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import java.util.concurrent.atomic.*

public object AtomicIntegerSerializer : KSerializer<AtomicInteger> {
    override fun serialize(encoder: Encoder, obj: AtomicInteger) {
        encoder.encodeInt(obj.get())
    }

    override fun deserialize(decoder: Decoder): AtomicInteger = AtomicInteger(decoder.decodeInt())

    override val descriptor: SerialDescriptor
        get() = IntDescriptor
}

public object AtomicLongSerializer : KSerializer<AtomicLong> {
    override fun serialize(encoder: Encoder, obj: AtomicLong) {
        encoder.encodeLong(obj.get())
    }

    override fun deserialize(decoder: Decoder): AtomicLong = AtomicLong(decoder.decodeLong())

    override val descriptor: SerialDescriptor
        get() = LongDescriptor
}

public object AtomicBooleanSerializer : KSerializer<AtomicBoolean> {
    override fun serialize(encoder: Encoder, obj: AtomicBoolean) {
        encoder.encodeBoolean(obj.get())
    }

    override fun deserialize(decoder: Decoder): AtomicBoolean = AtomicBoolean(decoder.decodeBoolean())

    override val descriptor: SerialDescriptor
        get() = BooleanDescriptor
}
