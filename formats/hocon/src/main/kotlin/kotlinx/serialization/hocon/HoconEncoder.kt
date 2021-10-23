@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.hocon

import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueFactory
import com.typesafe.config.ConfigValueType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.findPolymorphicSerializer
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.internal.NamedValueEncoder
import kotlinx.serialization.modules.SerializersModule

@InternalSerializationApi
abstract class AbstractHoconEncoder(
    protected val hocon: Hocon,
    private val valueConsumer: (ConfigValue) -> Unit,
) : NamedValueEncoder() {

    override val serializersModule: SerializersModule
        get() = hocon.serializersModule

    private var writeDiscriminator: Boolean = false

    override fun elementName(descriptor: SerialDescriptor, index: Int): String {
        return descriptor.getConventionElementName(index, hocon.useConfigNamingConvention)
    }

    override fun composeName(parentName: String, childName: String): String = childName

    protected abstract fun encodeTaggedConfigValue(tag: String, value: ConfigValue)
    protected abstract fun getCurrent(): ConfigValue

    override fun encodeTaggedValue(tag: String, value: Any) = encodeTaggedConfigValue(tag, configValueOf(value))
    override fun encodeTaggedNull(tag: String) = encodeTaggedConfigValue(tag, configValueOf(null))
    override fun encodeTaggedChar(tag: String, value: Char) = encodeTaggedString(tag, value.toString())

    override fun encodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor, ordinal: Int) {
        encodeTaggedString(tag, enumDescriptor.getElementName(ordinal))
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = hocon.encodeDefaults

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if (serializer !is AbstractPolymorphicSerializer<*> || hocon.useArrayPolymorphism) {
            serializer.serialize(this, value)
            return
        }

        @Suppress("UNCHECKED_CAST")
        val casted = serializer as AbstractPolymorphicSerializer<Any>
        val actualSerializer = casted.findPolymorphicSerializer(this, value as Any)
        writeDiscriminator = true

        actualSerializer.serialize(this, value)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val consumer =
            if (currentTagOrNull == null) valueConsumer
            else { value -> encodeTaggedConfigValue(currentTag, value) }
        val kind = descriptor.hoconKind(hocon.useArrayPolymorphism)

        return when {
            kind.listLike -> HoconConfigListEncoder(hocon, consumer)
            kind.objLike -> HoconConfigEncoder(hocon, consumer)
            kind == StructureKind.MAP -> HoconConfigMapEncoder(hocon, consumer)
            else -> this
        }.also { encoder ->
            if (writeDiscriminator) {
                encoder.encodeTaggedString(hocon.classDiscriminator, descriptor.serialName)
                writeDiscriminator = false
            }
        }
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

@InternalSerializationApi
class HoconConfigListEncoder(hocon: Hocon, configConsumer: (ConfigValue) -> Unit) :
    AbstractHoconEncoder(hocon, configConsumer) {

    private val values = mutableListOf<ConfigValue>()

    override fun elementName(descriptor: SerialDescriptor, index: Int): String = index.toString()

    override fun encodeTaggedConfigValue(tag: String, value: ConfigValue) {
        values.add(tag.toInt(), value)
    }

    override fun getCurrent(): ConfigValue = ConfigValueFactory.fromIterable(values)
}

@InternalSerializationApi
class HoconConfigMapEncoder(hocon: Hocon, configConsumer: (ConfigValue) -> Unit) :
    AbstractHoconEncoder(hocon, configConsumer) {

    private val configMap = mutableMapOf<String, ConfigValue>()

    private lateinit var key: String
    private var isKey: Boolean = true

    override fun encodeTaggedConfigValue(tag: String, value: ConfigValue) {
        if (isKey) {
            key = when (value.valueType()) {
                ConfigValueType.OBJECT -> TODO("Throw reasonable exception")
                ConfigValueType.LIST -> TODO("Throw reasonable exception")
                else -> value.unwrapped().toString()
            }
            isKey = false
        } else {
            configMap[key] = value
            isKey = true
        }
    }

    override fun getCurrent(): ConfigValue = ConfigValueFactory.fromMap(configMap)
}
