package kotlinx.serialization.encoding

import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
public interface ChunkedDecoder {
    /**
     * Method allows decoding a string value by fixed-size chunks.
     * Usable for handling very large strings.
     * Chunk size defined in the JsonLexer#BATCH_SIZE constant.
     * Feeds string chunks to the provided consumer.
     *
     * @param consumeChunk - lambda function to handle strong chunks
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
     *         val writer = FileWriter("/tmp/string.blob")
     *
     *         decoder.decodeStringChunked { chunk ->
     *             writer.append(chunk)
     *         }
     *         writer.close()
     *
     *         return LargeStringData("file:///tmp/string.blob")
     *     }
     * }
     * ```
     *
     * In this sample, we need to be able to handle huge string comes from json. Instead of storing it in memory,
     * we're offload it into file and return file name instead
     */
    @ExperimentalSerializationApi
    public fun decodeStringChunked(consumeChunk: (chunk: String) -> Unit)
}