/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.java

import kotlinx.serialization.*
import kotlinx.serialization.internal.*

public object StringBuilderSerializer : KSerializer<StringBuilder> {
    override fun serialize(encoder: Encoder, obj: StringBuilder) {
        encoder.encodeString(obj.toString())
    }

    override fun deserialize(decoder: Decoder): StringBuilder = StringBuilder(decoder.decodeString())

    override val descriptor: SerialDescriptor
        get() = StringDescriptor
}
