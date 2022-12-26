package kotlinx.serialization.hocon.serializers

import com.typesafe.config.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.hocon.*

/**
 * Serializer for [ConfigValue].
 * For decode using method [com.typesafe.config.Config.getValue].
 * Usage example:
 * ```
 * @Serializable
 * data class ExampleConfigValue(
 *  @Serializable(ConfigValueSerializer::class)
 *  val value: ConfigValue
 * )
 * val config = ConfigFactory.parseString("""
 *  value: { compute = "test" }
 * """.trimIndent())
 * val exampleConfigValue = Hocon.decodeFromConfig(ExampleConfigValue.serializer(), config)
 * val newConfig = Hocon.encodeToConfig(ExampleConfigValue.serializer(), exampleConfigValue)
 * ```
 */
@ExperimentalSerializationApi
object ConfigValueSerializer : KSerializer<ConfigValue> {

    private val valueResolver: (Config, String) -> ConfigValue = { conf, path -> conf.decodeConfigValue(path) }

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("hocon.com.typesafe.config.ConfigValue", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ConfigValue {
        return when (decoder) {
            is Hocon.ConfigReader -> decoder.getValueFromTaggedConfig(decoder.getCurrentTag(), valueResolver)
            is Hocon.ListConfigReader -> decoder.getValueFromTaggedConfig(decoder.getCurrentTag(), valueResolver)
            is Hocon.MapConfigReader -> decoder.getValueFromTaggedConfig(decoder.getCurrentTag(), valueResolver)
            else -> throw UnsupportedFormatException("ConfigValueSerializer")
        }
    }

    override fun serialize(encoder: Encoder, value: ConfigValue) {
        if (encoder is AbstractHoconEncoder) {
            encoder.encodeCurrentTagConfigValue(value)
        } else throw UnsupportedFormatException("ConfigValueSerializer")
    }

    private fun Config.decodeConfigValue(path: String): ConfigValue = try {
        getValue(path)
    } catch (e: ConfigException) {
        throw SerializationException("Value at $path cannot be read as ConfigValue because it is not a valid HOCON config value")
    }
}
