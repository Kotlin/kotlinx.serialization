/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import java.math.*

/**
 * Adds the given [BigDecimal] [value] to the resulting JSON object using the given [key].
 *
 * The value is encoded as an unquoted JSON number using [BigDecimal.toString], preserving its precision and scale.
 * A `null` value is added as [JsonNull].
 * This overload applies to values of type [BigDecimal]; values of type [Number] use the numeric overload instead.
 *
 * Returns the previous value associated with [key], or `null` if the key was not present.
 */
@ExperimentalSerializationApi
@IgnorableReturnValue
public fun JsonObjectBuilder.put(key: String, value: BigDecimal?): JsonElement? =
    put(key, JsonUnquotedLiteral(value?.toString()))
