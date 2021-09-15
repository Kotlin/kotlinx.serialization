package kotlinx.serialization.base64

import kotlinx.serialization.*
import kotlinx.serialization.base64.impl.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * Serializer that encodes and decodes [ByteArray] using [Base64](https://en.wikipedia.org/wiki/Base64) encodings.
 * This is usually makes sense with text formats like JSON.
 */
public object ByteArrayAsBase64StringSerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            "kotlinx.serialization.ByteArrayAsBase64StringSerializer",
            PrimitiveKind.STRING
    )

    override fun deserialize(decoder: Decoder): ByteArray {
        return decode(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(encode(value))
    }
}
