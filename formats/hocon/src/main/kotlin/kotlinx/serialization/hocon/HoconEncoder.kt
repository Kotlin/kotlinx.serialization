@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.hocon

import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.internal.NamedValueEncoder
import kotlinx.serialization.modules.SerializersModule

@InternalSerializationApi
abstract class AbstractHoconEncoder(
    protected val hocon: Hocon,
    private val valueConsumer: (ConfigValue) -> Unit,
) : NamedValueEncoder() {

    override val serializersModule: SerializersModule
        get() = hocon.serializersModule

    override fun composeName(parentName: String, childName: String): String = childName

    protected abstract fun encodeTaggedConfigValue(tag: String, value: ConfigValue)
    protected abstract fun getCurrent(): ConfigValue

    override fun encodeTaggedValue(tag: String, value: Any) = encodeTaggedConfigValue(tag, configValueOf(value))
    override fun encodeTaggedNull(tag: String) = encodeTaggedConfigValue(tag, configValueOf(null))
    override fun encodeTaggedChar(tag: String, value: Char) = encodeTaggedString(tag, value.toString())

    override fun encodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor, ordinal: Int) {
        encodeTaggedString(tag, enumDescriptor.getElementName(ordinal))
    }

    override fun endEncode(descriptor: SerialDescriptor) {
        valueConsumer(getCurrent())
    }

    private fun configValueOf(value: Any?) = ConfigValueFactory.fromAnyRef(value)
}

@InternalSerializationApi
class HoconConfigEncoder(hocon: Hocon, configConsumer: (ConfigValue) -> Unit) :
    AbstractHoconEncoder(hocon, configConsumer) {

    private val configMap = mutableMapOf<String, ConfigValue>()

    override fun encodeTaggedConfigValue(tag: String, value: ConfigValue) {
        configMap[tag] = value
    }

    override fun getCurrent(): ConfigValue = ConfigValueFactory.fromMap(configMap)
}
