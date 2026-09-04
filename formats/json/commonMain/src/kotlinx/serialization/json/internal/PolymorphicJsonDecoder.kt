/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.json.*


/**
 * An internal extension of [JsonDecoder] that exposes the state shared between nested decoders:
 * the polymorphic discriminator and the current [path][JsonPath].
 * This interface is used when decoding switches to another decoder (e.g. from streaming to tree decoding
 * in [JsonTransformingSerializer] or for polymorphic values) to ensure that things like the class discriminator
 * (i.e., the "type" field) are correctly propagated and that errors are reported with the full path.
 */
internal interface PolymorphicJsonDecoder : JsonDecoder {
    /**
     * The path shared by all decoders participating in the current decoding
     */
    val path: JsonPath

    /**
     * A discriminator of the current [JsonDecoder].
     */
    val discriminator: String?
}
