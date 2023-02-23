package kotlinx.serialization.hocon.serializers

import java.time.Duration as JDuration
import kotlin.time.*
import kotlinx.serialization.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.hocon.*
import kotlinx.serialization.hocon.internal.*

/**
 * Serializer for [java.time.Duration].
 * All possible Hocon duration formats [https://github.com/lightbend/config/blob/main/HOCON.md#duration-format] are accepted for decoding.
 * During encoding, the serializer emits values using time unit short names: d, h, m, s, ms, us, ns.
 * The largest integer time unit is encoded.
 * Example:
 *      120.seconds -> 2 m;
 *      121.seconds -> 121 s;
 *      120.minutes -> 2 h;
 *      122.minutes -> 122 m;
 *      24.hours -> 1 d.
 * When encoding, there is a conversion to [kotlin.time.Duration].
 * All restrictions on the maximum and minimum duration are specified in [kotlin.time.Duration].
 * Usage example:
 * ```
 * @Serializable
 * data class ExampleDuration(
 *  @Serializable(JDurationSerializer::class)
 *   val duration: java.time.Duration
 * )
 * val config = ConfigFactory.parseString("duration = 1 day")
 * val exampleDuration = Hocon.decodeFromConfig(ExampleDuration.serializer(), config)
 * val newConfig = Hocon.encodeToConfig(ExampleDuration.serializer(), exampleDuration)
 * ```
 */
@ExperimentalSerializationApi
@SuppressAnimalSniffer
object JavaDurationSerializer : KSerializer<JDuration> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("hocon.java.time.Duration", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): JDuration =
        if (decoder is HoconDecoder) decoder.decodeConfigValue { conf, path -> conf.decodeJavaDuration(path) }
        else throwUnsupportedFormatException("JavaDurationSerializer")

    override fun serialize(encoder: Encoder, value: JDuration) {
        if (encoder is HoconEncoder) encoder.encodeString(encodeDuration(value.toKotlinDuration()))
        else throwUnsupportedFormatException("JavaDurationSerializer")
    }
}
