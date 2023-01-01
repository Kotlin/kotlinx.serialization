package kotlinx.serialization.encoding

import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
public interface ChunkedDecoder {
    /**
     * Method allow decoding a string value by fixed-size chunks.
     * Usable for handling very large strings.
     * Chunk size defined in the JsonLexer#BATCH_SIZE constant.
     * Feeds string chunks to the provided consumer.
     *
     * @param consumeChunk - lambda function to handle strong chunks
     */
    @ExperimentalSerializationApi
    public fun decodeStringChunked(consumeChunk: (chunk: String) -> Unit)
}