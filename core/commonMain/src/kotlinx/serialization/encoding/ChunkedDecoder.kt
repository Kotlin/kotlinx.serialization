package kotlinx.serialization.encoding

import kotlinx.serialization.ExperimentalSerializationApi

/**
 * This interface indicates that decoder supports consuming large strings by chunks via consumeChunk method.
 * Currently, only streaming json decoder implements this interface.
 * Please note that this interface is only applicable to streaming decoders. That means that it is not possible to use
 * some JsonTreeDecoder features like polymorphism with this interface.
 */
@ExperimentalSerializationApi
public interface ChunkedDecoder {
    /**
     * Method allows decoding a string value by fixed-size chunks.
     * Usable for handling very large strings that may not fit in memory.
     * Chunk size is guaranteed to not exceed 16384 chars (but it may be smaller than that).
     * Feeds string chunks to the provided consumer.
     *
     * @param consumeChunk - lambda function to handle string chunks
     *
     * Example usage:
     * ```
     * @Serializable(with = LargeStringSerializer::class)
     * data class LargeStringData(val largeString: String)
     *
     * @Serializable
     * data class ClassWithLargeStringDataField(val largeStringField: LargeStringData)
     *
     * object LargeStringSerializer : KSerializer<LargeStringData> {
     *     override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LargeStringContent", PrimitiveKind.STRING)
     *
     *     override fun deserialize(decoder: Decoder): LargeStringData {
     *         require(decoder is ChunkedDecoder) { "Only chunked decoder supported" }
     *
     *         val tmpFile = createTempFile()
     *         val writer = FileWriter(tmpFile.toFile()).use {
     *             decoder.decodeStringChunked { chunk ->
     *                 writer.append(chunk)
     *             }
     *         }
     *         return LargeStringData("file://${tmpFile.absolutePathString()}")
     *     }
     * }
     * ```
     *
     * In this sample, we need to be able to handle a huge string coming from json. Instead of storing it in memory,
     * we offload it into a file and return the file name instead
     */
    @ExperimentalSerializationApi
    public fun decodeStringChunked(consumeChunk: (chunk: String) -> Unit)
}