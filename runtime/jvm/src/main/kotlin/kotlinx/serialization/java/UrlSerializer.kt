/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.java

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import java.net.*

public object UrlSerializer : KSerializer<URL> {
    override fun serialize(encoder: Encoder, obj: URL) {
        encoder.encodeString(obj.toExternalForm())
    }

    override fun deserialize(decoder: Decoder): URL = URL(decoder.decodeString())

    override val descriptor: SerialDescriptor
        get() = StringDescriptor
}
