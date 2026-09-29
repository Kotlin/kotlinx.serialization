/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.json.JsonDecoder

/**
 * An internal extension of [JsonDecoder] that exposes the polymorphic discriminator.
 * This interface is used during the transformation process to ensure that the
 * class discriminator (e.g., the "type" field) is correctly propagated between
 * decoders, especially when using [JsonTransformingSerializer].
 */
internal interface PolymorphicJsonDecoder : JsonDecoder {
    /**
     * A discriminator of the current [JsonDecoder].
     */
    val discriminator: String?
}
