package kotlinx.serialization.hocon.serializers

import com.typesafe.config.*
import java.math.BigInteger
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.hocon.*

/**
 * Serializer for [ConfigMemorySize].
 * All possible Hocon size formats [https://github.com/lightbend/config/blob/main/HOCON.md#size-in-bytes-format] are accepted for decoding.
 * During encoding, the serializer emits values using powers of two: byte, KiB, MiB, GiB, TiB, PiB, EiB, ZiB, YiB.
 * Encoding uses the largest possible integer value.
 * Example:
 *  1024 byte -> 1 KiB;
 *  1024 KiB -> 1 MiB;
 *  1025 KiB -> 1025 KiB.
 * Usage example:
 * ```
 * @Serializable
 * data class ConfigMemory(
 *      @Serializable(ConfigMemorySizeSerializer::class)
 *      val size: ConfigMemorySize
 * )
 * val config = ConfigFactory.parseString("size = 1 MiB")
 * val configMemory = Hocon.decodeFromConfig(ConfigMemory.serializer(), config)
 * val newConfig = Hocon.encodeToConfig(ConfigMemory.serializer(), configMemory)
 * ```
 */
@ExperimentalSerializationApi
object ConfigMemorySizeSerializer : KSerializer<ConfigMemorySize> {

    // For powers of two.
    private val memoryUnitFormats = listOf("byte", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "ZiB", "YiB")

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("hocon.com.typesafe.config.ConfigMemorySize", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ConfigMemorySize =
        if (decoder is HoconDecoder) decoder.decodeConfigValue { conf, path -> conf.decodeMemorySize(path) }
        else throwUnsupportedFormatException("ConfigMemorySizeSerializer")

    override fun serialize(encoder: Encoder, value: ConfigMemorySize) {
        if (encoder is HoconEncoder) {
            // We determine that it is divisible by 1024 (2^10).
            // And if it is divisible, then the number itself is shifted to the right by 10.
            // And so on until we find one that is no longer divisible by 1024.
            // ((n & ((1 << m) - 1)) == 0)
            val andVal = BigInteger.valueOf(1023) // ((2^10) - 1) = 0x3ff = 1023
            var bytes = value.toBytesBigInteger()
            var unitIndex = 0
            while (bytes.and(andVal) == BigInteger.ZERO) { // n & 0x3ff == 0
                if (unitIndex < memoryUnitFormats.lastIndex) {
                    bytes = bytes.shiftRight(10)
                    unitIndex++
                } else break
            }
            encoder.encodeString("$bytes ${memoryUnitFormats[unitIndex]}")
        } else {
            throwUnsupportedFormatException("ConfigMemorySizeSerializer")
        }
    }

    private fun Config.decodeMemorySize(path: String): ConfigMemorySize = try {
        getMemorySize(path)
    } catch (e: ConfigException) {
        throw SerializationException("Value at $path cannot be read as ConfigMemorySize because it is not a valid HOCON Size value", e)
    }
}
