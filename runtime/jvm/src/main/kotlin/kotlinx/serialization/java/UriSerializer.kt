/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.java

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import java.net.*

public object UriSerializer : KSerializer<URI> {
    override fun serialize(encoder: Encoder, obj: URI) {
        encoder.encodeString(obj.toASCIIString())
    }

    override fun deserialize(decoder: Decoder): URI = URI(decoder.decodeString())

    override val descriptor: SerialDescriptor
        get() = StringDescriptor
}
