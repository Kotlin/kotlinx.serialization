package kotlinx.serialization.hocon

import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueType
import kotlinx.serialization.SerializationException

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
