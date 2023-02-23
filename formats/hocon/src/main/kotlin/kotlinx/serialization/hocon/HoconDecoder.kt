package kotlinx.serialization.hocon

import com.typesafe.config.Config
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Decoder used by Hocon during deserialization.
 * This interface allows to call methods from the Lightbend/config library on the [Config] object to intercept default deserialization process.
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
 *         if (encoder is AbstractHoconEncoder) encoder.encodeConfigValue(value.root())
 *         else throw SerializationException("This class can be encoded only by Hocon format")
 *     }
 * }
 *
 * val nestedConfig = ConfigFactory.parseString("nested { value = \"test\" }")
 * val globalConfig = Hocon.encodeToConfig(Example(nestedConfig)) // d: { nested: { value = "test" } }
 * val newNestedConfig = Hocon.decodeFromConfig(Example.serializer(), globalConfig)
 * ```
 */
@ExperimentalSerializationApi
sealed interface HoconDecoder {

    /**
     * Decodes the value at the current path from the input.
     * Allows to call methods on a [Config] instance.
     *
     * @param E type of value
     * @param extractValueAtPath lambda for extracting value, where conf - original config object, path - current path expression being decoded.
     * @return result of lambda execution
     */
    fun <E> decodeConfigValue(extractValueAtPath: (conf: Config, path: String) -> E): E
}
