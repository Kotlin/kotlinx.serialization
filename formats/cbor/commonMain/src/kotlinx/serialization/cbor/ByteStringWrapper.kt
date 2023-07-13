package kotlinx.serialization.cbor

/**
 * Use this class if you'll need to serialize a complex type as a byte string before encoding it,
 * i.e. as it is the case with the protected header in COSE structures.
 *
 * Clients also need to write a custom serializer, i.e. in the form of
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
 *     @Serializable(with = ByteStringWrapperSerializer::class)
 *     @ByteString
 *     @SerialLabel(1)
 *     @SerialName("protectedHeader")
 *     val protectedHeader: ByteStringWrapper<CoseHeader>,
 * )
 *
 * object ByteStringWrapperSerializer : KSerializer<ByteStringWrapper<CoseHeader>> {
 *     override val descriptor: SerialDescriptor =
 *         PrimitiveSerialDescriptor("ByteStringWrapperSerializer", PrimitiveKind.STRING)
 *     override fun serialize(encoder: Encoder, value: ByteStringWrapper<CoseHeader>) {
 *         val bytes = Cbor.encodeToByteArray(value.value)
 *         encoder.encodeSerializableValue(ByteArraySerializer(), bytes)
 *     }
 *     override fun deserialize(decoder: Decoder): ByteStringWrapper<CoseHeader> {
 *         val bytes = decoder.decodeSerializableValue(ByteArraySerializer())
 *         return ByteStringWrapper(Cbor.decodeFromByteArray(bytes), bytes)
 *     }
 * }
 * ```
 *
 * then serializing a `CoseSigned` object would result in `a10143a10126`, in diagnostic notation:
 *
 * ```
 * A1           # map(1)
 *    01        # unsigned(1)
 *    43        # bytes(3)
 *       A10126 # "\xA1\u0001&"
 * ```
 *
 * so the `protectedHeader` got serialized first and then encoded as a `@ByteString`
 */
public data class ByteStringWrapper<T>(
    val value: T,
    val serialized: ByteArray = byteArrayOf()
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
}