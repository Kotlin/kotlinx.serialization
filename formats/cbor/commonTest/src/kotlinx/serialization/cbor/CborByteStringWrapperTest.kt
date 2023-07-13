package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.test.*

class CborByteStringWrapperTest {

    private val reference = CoseSigned(protectedHeader = ByteStringWrapper(CoseHeader(alg = -7)))

    /**
     * BF             # map(*)
     *    01          # unsigned(1)
     *    44          # bytes(4)
     *       BF0126FF # "\xBF\u0001&\xFF"
     *    FF          # primitive(*)
     */
    private val referenceHex = "bf0144bf0126ffff"

    @Test
    fun writeReadVerifyCoseSigned() {
        assertEquals(referenceHex, Cbor.encodeToHexString(CoseSigned.serializer(), reference))
        assertEquals(reference, Cbor.decodeFromHexString(referenceHex))
    }


    @Serializable
    data class CoseHeader(
        @SerialLabel(1)
        @SerialName("alg")
        val alg: Int? = null
    )

    @Serializable
    data class CoseSigned(
        @Serializable(with = ByteStringWrapperSerializer::class)
        @ByteString
        @SerialLabel(1)
        @SerialName("protectedHeader")
        val protectedHeader: ByteStringWrapper<CoseHeader>,
    )

    object ByteStringWrapperSerializer : KSerializer<ByteStringWrapper<CoseHeader>> {

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("ByteStringWrapperSerializer", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: ByteStringWrapper<CoseHeader>) {
            val bytes = Cbor.encodeToByteArray(value.value)
            encoder.encodeSerializableValue(ByteArraySerializer(), bytes)
        }

        override fun deserialize(decoder: Decoder): ByteStringWrapper<CoseHeader> {
            val bytes = decoder.decodeSerializableValue(ByteArraySerializer())
            return ByteStringWrapper(Cbor.decodeFromByteArray(bytes), bytes)
        }

    }

}
