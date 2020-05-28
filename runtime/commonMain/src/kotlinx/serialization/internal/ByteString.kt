package kotlinx.serialization.internal

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializer

public expect class ByteString(bytes: ByteArray) {
    public val bytes: ByteArray
    public val size: Int

    @Serializer(forClass = ByteString::class)
    public companion object : KSerializer<ByteString> {
        public override val descriptor: SerialDescriptor
        public override fun serialize(encoder: Encoder, value: ByteString)
        public override fun deserialize(decoder: Decoder): ByteString
    }
}
