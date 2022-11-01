/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.hocon

import com.typesafe.config.*
import kotlinx.serialization.*

internal fun SerializerNotFoundException(type: String?) = SerializationException(
    "Polymorphic serializer was not found for " +
            if (type == null) "missing class discriminator ('null')" else "class discriminator '$type'"
)

internal inline fun <reified T> ConfigValueTypeCastException(valueOrigin: ConfigOrigin) = SerializationException(
    "${valueOrigin.description()} required to be of type ${T::class.simpleName}."
)

internal fun InvalidKeyKindException(value: ConfigValue) = SerializationException(
    "Value of type '${value.valueType()}' can't be used in HOCON as a key in the map. " +
            "It should have either primitive or enum kind."
)

internal fun UnsupportedFormatException(serializerName: String) =
    SerializationException("$serializerName must only be applied to Hocon.")
