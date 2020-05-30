package kotlinx.serialization.cbor

import kotlinx.serialization.CompositeDecoder
import kotlinx.serialization.Decoder
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.AbstractDecoder

public interface CborDecoder: Decoder {
    /**
     * Decodes a CBOR ByteString.
     * @return [ByteString]
     *
     * @throws SerializationException if used by any format other than CBOR.
     */
    public fun decodeByteString(): ByteString
}

public interface CborCompositeDecoder: CompositeDecoder {
    /**
     * Decodes a ByteString value from the underlying input.
     *
     * The resulting value is associated with the [descriptor] element at the given [index].
     *
     * @throws SerializationException if used by any format other than CBOR.
     */
    public fun decodeByteStringElement(descriptor: SerialDescriptor, index: Int): ByteString
}

public abstract class CborAbstractDecoder: AbstractDecoder(), CborDecoder, CborCompositeDecoder {
    override fun decodeByteString(): ByteString = decodeValue() as ByteString

    final override fun decodeByteStringElement(descriptor: SerialDescriptor, index: Int): ByteString = decodeByteString()
}
