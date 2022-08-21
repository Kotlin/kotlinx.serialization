package kotlinx.serialization.encoding

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind

public interface ChunkedDecoder {
    /**
     * Decodes a string value by chunks (16k by default), outputs string them to consumer.
     * Corresponding kind is [PrimitiveKind.STRING].
     */
    @ExperimentalSerializationApi
    public fun decodeStringChunked(consumeChunk:(chunk:String) -> Unit)
}