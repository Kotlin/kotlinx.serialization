package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.internal.*
import kotlin.jvm.*

public interface JsonOutput: Encoder, CompositeEncoder {

    public val json: Json

    public fun writeTree(tree: JsonElement)
}