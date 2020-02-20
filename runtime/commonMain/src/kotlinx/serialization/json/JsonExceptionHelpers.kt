/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("FunctionName")

package kotlinx.serialization.json

import kotlinx.serialization.SerialDescriptor

internal fun InvalidFloatingPoint(value: Number, type: String) = JsonEncodingException(
    "$value is not a valid $type as per JSON specification. " +
            "You can enable 'serializeSpecialFloatingPointValues' property to serialize such values"
)


internal fun InvalidFloatingPoint(value: Number, key: String, type: String) = JsonEncodingException(
    "$value with key $key is not a valid $type as per JSON specification. " +
            "You can enable 'serializeSpecialFloatingPointValues' property to serialize such values"
)

internal fun jsonUnknownKeyException(position: Int, key: String) = JsonDecodingException(
    position,
    "Strict JSON encountered unknown key: $key\n" +
            "You can enable 'ignoreUnknownKeys' property to skip unknown keys"
)


internal fun JsonMapInvalidKeyKind(keyDescriptor: SerialDescriptor) = JsonException(
    "Value of type ${keyDescriptor.serialName} can't be used in json as map key. " +
            "It should have either primitive or enum kind, but its kind is ${keyDescriptor.kind}.\n" +
            "You can convert such maps to arrays [key1, value1, key2, value2,...] with 'allowStructuredMapKeys' property in JsonConfiguration"
)
