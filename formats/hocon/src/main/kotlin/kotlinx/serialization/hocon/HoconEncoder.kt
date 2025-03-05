package kotlinx.serialization.hocon

import com.typesafe.config.ConfigValue
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Encoder used by Hocon during serialization.
 * This interface allows intercepting serialization process and insertion of arbitrary [ConfigValue] into the output.
 *
 * Usage example (nested config serialization):
 * ```
 * @Serializable
 * data class Example(
 *   @Serializable(NestedConfigSerializer::class)
 *   val d: Config
 * )
 * object NestedConfigSerializer : KSerializer<Config> {
 *     override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("package.Config", PrimitiveKind.STRING)
 *
 *     override fun deserialize(decoder: Decoder): Config =
 *         if (decoder is HoconDecoder) decoder.decodeConfigValue { conf, path -> conf.getConfig(path) }
 *         else throw SerializationException("This class can be decoded only by Hocon format")
 *
 *     override fun serialize(encoder: Encoder, value: Config) {
 *         if (encoder is HoconEncoder) encoder.encodeConfigValue(value.root())
 *         else throw SerializationException("This class can be encoded only by Hocon format")
 *     }
 * }
 * val nestedConfig = ConfigFactory.parseString("nested { value = \"test\" }")
 * val globalConfig = Hocon.encodeToConfig(Example(nestedConfig)) // d: { nested: { value = "test" } }
 * val newNestedConfig = Hocon.decodeFromConfig(Example.serializer(), globalConfig)
 * ```
 */
@ExperimentalSerializationApi
public sealed interface HoconEncoder {

    /**
     * Appends the given [ConfigValue] element to the current output.
     *
     * @param value to insert
     */
    public fun encodeConfigValue(value: ConfigValue)
}
