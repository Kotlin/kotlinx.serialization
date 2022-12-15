package kotlinx.serialization.hocon.serializers

import com.typesafe.config.*
import java.time.Period
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.hocon.*
import kotlinx.serialization.hocon.internal.*

/**
 * Serializer for [Period].
 * For decode using [Period format][https://github.com/lightbend/config/blob/main/HOCON.md#period-format].
 * For encode used unit strings for period: d, m, y.
 * Encoding use the largest period unit.
 * Encoding [Period] that contains a day along with a month or year is not possible, because only one period unit is allowed in HOCON.
 * Example:
 *      12 months -> 1 year
 *      1 year 6 months -> 18 months
 *      12 days -> 12 days
 *      1 year 12 days -> throw SerializationException
 *      10 months 2 days -> throw SerializationException
 *      1 year 5 month 5 days -> throw SerializationException
 * Usage example:
 * ```
 * @Serializable
 * data class ExamplePeriod(
 *         @Serializable(JPeriodSerializer::class)
 *         val period: Period
 * )
 * val config = ConfigFactory.parseString("period = 1 y")
 * val examplePeriod = Hocon.decodeFromConfig(ExamplePeriod.serializer(), config)
 * val newConfig = Hocon.encodeToConfig(ExamplePeriod.serializer(), examplePeriod)
 * ```
 */
@ExperimentalSerializationApi
@SuppressAnimalSniffer
object JPeriodSerializer : KSerializer<Period> {

    private val valueResolver: (Config, String) -> Period = { conf, path -> conf.decodePeriod(path) }

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("hocon.java.time.Period", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Period {
        return when (decoder) {
            is Hocon.ConfigReader -> decoder.getValueFromTaggedConfig(decoder.getCurrentTag(), valueResolver)
            is Hocon.ListConfigReader -> decoder.getValueFromTaggedConfig(decoder.getCurrentTag(), valueResolver)
            is Hocon.MapConfigReader -> decoder.getValueFromTaggedConfig(decoder.getCurrentTag(), valueResolver)
            else -> throw UnsupportedFormatException("JPeriodSerializer")
        }
    }

    override fun serialize(encoder: Encoder, value: Period) {
        val normalized = value.normalized()
        val result = if (normalized.years == 0 && normalized.months == 0) {
            "${normalized.days} d"
        } else if (normalized.days == 0) {
            if (normalized.months == 0) "${normalized.years} y"
            else "${normalized.toTotalMonths()} m"
        } else throw SerializationException("Not possible to serialize java.time.Period because only one time unit can be specified in HOCON")
        encoder.encodeString(result)
    }

    private fun Config.decodePeriod(path: String): Period = try {
        getPeriod(path)
    } catch (e: ConfigException) {
        throw SerializationException("Value at $path cannot be read as java.time.Period because it is not a valid HOCON Period Format value", e)
    }
}
