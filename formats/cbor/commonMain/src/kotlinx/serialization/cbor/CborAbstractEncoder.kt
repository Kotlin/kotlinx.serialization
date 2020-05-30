package kotlinx.serialization.cbor

import kotlinx.serialization.CompositeEncoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.AbstractEncoder

public interface CborEncoder: Encoder {
    /**
     * Encodes an array of bytes as a CBOR ByteString.
     *
     * Can be ignored by all other formats.
     *
     * * @throws SerializationException if used by any format other than CBOR.
     */
    public fun encodeByteString(value: ByteString)
}

public interface CborCompositeEncoder: CompositeEncoder {
    /**
     * Encodes the bytes [value] associated with an element at the given [index] in [serial descriptor][descriptor].
     * The element at the given [index] should be a [ByteArray].
     *
     * @throws SerializationException if used by any format other than CBOR.
     */
    public fun encodeByteStringElement(descriptor: SerialDescriptor, index: Int, value: ByteString)
}

public abstract class CborAbstractEncoder: AbstractEncoder(), CborEncoder, CborCompositeEncoder {
    override fun encodeByteString(value: ByteString): Unit = encodeValue(value)

    final override fun encodeByteStringElement(
        descriptor: SerialDescriptor,
        index: Int,
        value: ByteString
    ) {
        if (encodeElement(descriptor, index)) encodeByteString(value)
    }
}
