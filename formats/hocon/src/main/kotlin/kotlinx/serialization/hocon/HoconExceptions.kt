package kotlinx.serialization.hocon

import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueType
import kotlinx.serialization.SerializationException

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
