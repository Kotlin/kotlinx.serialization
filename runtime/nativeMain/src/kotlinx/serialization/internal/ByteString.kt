package kotlinx.serialization.internal

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializer
import kotlinx.serialization.StructureKind

public actual class ByteString actual constructor(
    actual val bytes: ByteArray
) {
    public actual val size: Int = bytes.size

    @Serializer(forClass = ByteString::class)
    public actual companion object : KSerializer<ByteString> {
        public actual override val descriptor: SerialDescriptor = SerialDescriptor(
            serialName = "kotlinx.serialization.ByteString",
            kind = StructureKind.BYTE_STRING
        )
        public actual override fun serialize(encoder: Encoder, value: ByteString): Unit = encoder.encodeByteString(value)
        public actual override fun deserialize(decoder: Decoder): ByteString = decoder.decodeByteString()
    }
}
