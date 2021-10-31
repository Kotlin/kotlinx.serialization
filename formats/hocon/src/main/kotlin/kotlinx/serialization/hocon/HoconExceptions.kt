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

internal fun ConfigValueTypeCastException(value: ConfigValue, type: ConfigValueType) = SerializationException(
    "${value.origin().description()} required to be a $type."
)

internal fun InvalidKeyKindException(value: ConfigValue) = SerializationException(
    "Value of type '${value.valueType()}' can't be used in HOCON as a key in the map. " +
            "It should have either primitive or enum kind."
)
