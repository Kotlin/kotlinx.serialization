package kotlinx.serialization.json

import kotlinx.serialization.*

public interface JsonInput : Decoder, CompositeDecoder {

    public val json: Json

    public fun readTree(): JsonElement
}
