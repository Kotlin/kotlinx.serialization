package kotlinx.serialization.base64

import kotlinx.serialization.*
import kotlinx.serialization.base64.impl.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * Serializer that encodes and decodes [UByteArray] using [Base64](https://en.wikipedia.org/wiki/Base64) encodings.
 * This is usually makes sense with text formats like JSON.
 */
@ExperimentalUnsignedTypes
public object UByteArrayAsBase64StringSerializer : KSerializer<UByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            "kotlinx.serialization.UByteArrayAsBase64StringSerializer",
            PrimitiveKind.STRING
    )

    override fun deserialize(decoder: Decoder): UByteArray {
        return decode(decoder.decodeString()).toUByteArray()
    }

    override fun serialize(encoder: Encoder, value: UByteArray) {
        encoder.encodeString(encode(value.toByteArray()))
    }
}
