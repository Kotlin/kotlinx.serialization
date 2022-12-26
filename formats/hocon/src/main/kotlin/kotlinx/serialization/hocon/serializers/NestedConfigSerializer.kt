package kotlinx.serialization.hocon.serializers

import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.hocon.AbstractHoconEncoder
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.UnsupportedFormatException

/**
 * Serializer for nested [Config].
 * For decode using method [com.typesafe.config.Config.getConfig].
 * Usage example:
 * ```
 * @Serializable
 * data class ExampleNestedConfig(
 *   @Serializable(NestedConfigSerializer::class)
 *   val nested: Config
 * )
 * val config = ConfigFactory.parseString("""
 *   nested: { conf: { value = "test" } }
 * """.trimIndent())
 * val exampleNestedConfig = Hocon.decodeFromConfig(ExampleNestedConfig.serializer(), config)
 * val newConfig = Hocon.encodeToConfig(ExampleNestedConfig.serializer(), exampleNestedConfig)
 * ```
 */
@ExperimentalSerializationApi
object NestedConfigSerializer : KSerializer<Config> {

    private val valueResolver: (Config, String) -> Config = { conf, path -> conf.decodeConfig(path) }

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("hocon.com.typesafe.config.Config", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Config {
        return when (decoder) {
            is Hocon.ConfigReader -> decoder.getValueFromTaggedConfig(decoder.getCurrentTag(), valueResolver)
            is Hocon.ListConfigReader -> decoder.getValueFromTaggedConfig(decoder.getCurrentTag(), valueResolver)
            is Hocon.MapConfigReader -> decoder.getValueFromTaggedConfig(decoder.getCurrentTag(), valueResolver)
            else -> throw UnsupportedFormatException("ConfigSerializer")
        }
    }

    override fun serialize(encoder: Encoder, value: Config) {
        if (encoder is AbstractHoconEncoder) {
            encoder.encodeCurrentTagConfigValue(value.root())
        } else throw UnsupportedFormatException("ConfigSerializer")
    }

    private fun Config.decodeConfig(path: String): Config = try {
        getConfig(path)
    } catch (e: ConfigException) {
        throw SerializationException("Value at $path cannot be read as Config because it is not a valid HOCON config value")
    }
}
