package kotlinx.serialization.cbor

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializer
import kotlinx.serialization.StructureKind

public class ByteString(
    public val bytes: ByteArray
) {
    public val size: Int
        get() = bytes.size

    @Serializer(forClass = ByteString::class)
    public companion object : CborKSerializer<ByteString> {
        override val descriptor: SerialDescriptor = SerialDescriptor(
            serialName = "kotlinx.serialization.cbor.ByteString",
            kind = StructureKind.VALUE_TYPE
        )

        override fun serialize(encoder: CborEncoder, value: ByteString): Unit = encoder.encodeByteString(value)
        override fun deserialize(decoder: CborDecoder): ByteString = decoder.decodeByteString()

        override fun serialize(encoder: Encoder, value: ByteString) {
            check(encoder is CborEncoder) { "CBOR encoding must use a CborEncoder" }
            serialize(encoder, value)
        }

        override fun deserialize(decoder: Decoder): ByteString {
            check(decoder is CborDecoder) { "CBOR decoding must use a CborDecoder" }
            return deserialize(decoder)
        }
    }
}
