package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * Use this class if you'll need to serialize a complex type as a byte string before encoding it,
 * i.e. as it is the case with the protected header in COSE structures.
 *
 * An example for a COSE header data class would be:
 *
 * ```
 * @Serializable
 * data class CoseHeader(
 *     @SerialLabel(1)
 *     @SerialName("alg")
 *     val alg: Int? = null
 * )
 *
 * @Serializable
 * data class CoseSigned(
 *     @ByteString
 *     @SerialLabel(1)
 *     @SerialName("protectedHeader")
 *     val protectedHeader: ByteStringWrapper<CoseHeader>,
 * )
 * ```
 *
 * Serializing this `CoseHeader` object would result in `a10143a10126`, in diagnostic notation:
 *
 * ```
 * A1           # map(1)
 *    01        # unsigned(1)
 *    43        # bytes(3)
 *       A10126 # "\xA1\u0001&"
 * ```
 *
 * so the `protectedHeader` got serialized first and then encoded as a `@ByteString`.
 *
 * Note that `equals()` and `hashCode()` only use `value`, not `serialized`.
 */
@Serializable(with = ByteStringWrapperSerializer::class)
public class ByteStringWrapper<T>(
    public val value: T,
    public val serialized: ByteArray = byteArrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ByteStringWrapper<*>

        return value == other.value
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "ByteStringWrapper(value=$value, serialized=${serialized.contentToString()})"
    }

}


@OptIn(ExperimentalSerializationApi::class)
public class ByteStringWrapperSerializer<T>(private val dataSerializer: KSerializer<T>) :
    KSerializer<ByteStringWrapper<T>> {

    override val descriptor: SerialDescriptor = dataSerializer.descriptor

    override fun serialize(encoder: Encoder, value: ByteStringWrapper<T>) {
        val bytes = Cbor.encodeToByteArray(dataSerializer, value.value)
        encoder.encodeSerializableValue(ByteArraySerializer(), bytes)
    }

    override fun deserialize(decoder: Decoder): ByteStringWrapper<T> {
        val bytes = decoder.decodeSerializableValue(ByteArraySerializer())
        val value = Cbor.decodeFromByteArray(dataSerializer, bytes)
        return ByteStringWrapper(value, bytes)
    }

}
