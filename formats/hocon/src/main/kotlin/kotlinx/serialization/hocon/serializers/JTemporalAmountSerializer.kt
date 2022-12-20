package kotlinx.serialization.hocon.serializers

import com.typesafe.config.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.hocon.*
import kotlinx.serialization.hocon.internal.SuppressAnimalSniffer
import java.time.*
import java.time.temporal.TemporalAmount

/**
 * Serializer for [TemporalAmount].
 * For decode using method [com.typesafe.config.Config.getTemporal].
 * This method will first try to get the value as a [Duration], and if unsuccessful, then as a [Period].
 * This means that values like "5m" will be parsed as 5 minutes rather than 5 months!
 * Encoding is available for two implementations: [Duration] and [Period].
 * @see kotlinx.serialization.hocon.serializers.JDurationSerializer
 * @see kotlinx.serialization.hocon.serializers.JPeriodSerializer
 * Usage example:
 * ```
 * @Serializable
 * data class ExampleTemporal(
 *      @Serializable(JTemporalAmountSerializer::class)
 *      val amount: TemporalAmount
 * )
 * val config = ConfigFactory.parseString("amount = 1 y")
 * val exampleAmount = Hocon.decodeFromConfig(ExampleTemporal.serializer(), config)
 * val newConfig = Hocon.encodeToConfig(ExampleTemporal.serializer(), exampleAmount)
 * ```
 */
@ExperimentalSerializationApi
@SuppressAnimalSniffer
object JTemporalAmountSerializer : KSerializer<TemporalAmount> {

    private val valueResolver: (Config, String) -> TemporalAmount = { conf, path -> conf.decodeTemporal(path) }

    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("hocon.java.time.temporal.TemporalAmount", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): TemporalAmount {
        return when (decoder) {
            is Hocon.ConfigReader -> decoder.getValueFromTaggedConfig(decoder.getCurrentTag(), valueResolver)
            is Hocon.ListConfigReader -> decoder.getValueFromTaggedConfig(decoder.getCurrentTag(), valueResolver)
            is Hocon.MapConfigReader -> decoder.getValueFromTaggedConfig(decoder.getCurrentTag(), valueResolver)
            else -> throw UnsupportedFormatException("JTemporalAmountSerializer")
        }
    }

    override fun serialize(encoder: Encoder, value: TemporalAmount) {
        when (value) {
            is Duration -> JDurationSerializer.serialize(encoder, value)
            is Period -> JPeriodSerializer.serialize(encoder, value)
            else -> throw SerializationException("Class ${value::class.java} cannot be serialized in Hocon")
        }
    }

    private fun Config.decodeTemporal(path: String): TemporalAmount = try {
        getTemporal(path)
    } catch (e: ConfigException) {
        throw SerializationException("Value at $path cannot be read as java.time.temporal.TemporalAmount because it is not a valid HOCON value", e)
    }
}
