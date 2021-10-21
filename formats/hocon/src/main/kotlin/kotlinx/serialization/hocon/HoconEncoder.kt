@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.hocon

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.internal.NamedValueEncoder
import kotlinx.serialization.modules.SerializersModule

class HoconConfigEncoder(private val hocon: Hocon) : NamedValueEncoder() {

    override val serializersModule: SerializersModule
        get() = hocon.serializersModule

    var conf = ConfigFactory.empty()
        private set

    override fun encodeTaggedValue(tag: String, value: Any) = withTaggedConfigValue(tag, value)
    override fun encodeTaggedNull(tag: String) = withTaggedConfigValue(tag, null)
    override fun encodeTaggedChar(tag: String, value: Char) = encodeTaggedString(tag, value.toString())

    override fun encodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor, ordinal: Int) {
        encodeTaggedString(tag, enumDescriptor.getElementName(ordinal))
    }

    private fun withTaggedConfigValue(tag: String, value: Any?) {
        conf = conf.withValue(tag, ConfigValueFactory.fromAnyRef(value))
    }
}
