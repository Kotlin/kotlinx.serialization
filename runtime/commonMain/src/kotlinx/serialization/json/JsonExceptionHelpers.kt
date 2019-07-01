/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("FunctionName")

package kotlinx.serialization.json

import kotlinx.serialization.SerialDescriptor

internal fun JsonInvalidValueInStrictModeException(value: Float) = JsonEncodingException(
    "$value is not a valid float as per JSON spec.\n" +
            "You can disable strict mode to serialize such values"
)

internal fun JsonInvalidValueInStrictModeException(value: Double) = JsonEncodingException(
    "$value is not a valid double as per JSON spec.\n" +
            "You can disable strict mode to serialize such values"
)


internal fun jsonUnknownKeyException(position: Int, key: String) = JsonDecodingException(
    position,
    "Strict JSON encountered unknown key: $key\n" +
            "You can disable strict mode to skip unknown keys"
)


internal fun JsonMapInvalidKeyKind(keyDescriptor: SerialDescriptor) = JsonException(
    "Value of type ${keyDescriptor.name} can't be used in json as map key. " +
            "It should have either primitive or enum kind, but its kind is ${keyDescriptor.kind}.\n" +
            "You can convert such maps to arrays [key1, value1, key2, value2,...] with 'allowStructuredMapKeys' flag in JsonConfiguration."
)
