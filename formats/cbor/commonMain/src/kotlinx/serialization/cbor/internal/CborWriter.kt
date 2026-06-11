package kotlinx.serialization.cbor.internal

/**
 * Common interface for CBOR writers that can emit CBOR data to different destinations.
 */
internal sealed interface CborWriter {
    // Collection operations are represented by Encoder.beginStructure/endStructure.

    // Value writing operations
    fun encodeNull()
    fun encodeBoolean(value: Boolean)
    fun encodeRawNumber(value: Long)
    fun encodePositive(value: ULong)
    fun encodeNegative(value: ULong)
    fun encodeString(value: String)
    fun encodeByteString(byteArray: ByteArray)
    fun encodeDouble(value: Double)
    fun encodeFloat(value: Float)
    fun encodeUndefined()

    // Tag writing
    @OptIn(ExperimentalUnsignedTypes::class)
    fun encodeTags(tags: ULongArray)
    @OptIn(ExperimentalUnsignedTypes::class)
    fun encodeElementTags(tags: ULongArray)
}
