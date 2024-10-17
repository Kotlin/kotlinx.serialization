// This file was automatically generated from serialization-transform-json.md by Knit tool. Do not edit.
package example.exampleJsonTransform06

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

data class UnknownProject(val name: String, val details: JsonObject)

object UnknownProjectSerializer : KSerializer<UnknownProject> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UnknownProject") {
        element<String>("name")
        element<JsonElement>("details")
    }

    override fun deserialize(decoder: Decoder): UnknownProject {
        // Ensures the decoder is JSON-specific
        val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")

        // Reads the entire content as JSON
        val json = jsonInput.decodeJsonElement().jsonObject

        // Extracts and removes the "name" property
        val name = json.getValue("name").jsonPrimitive.content

        // Flattens the remaining properties into the 'details' field
        val details = json.toMutableMap()
        details.remove("name")
        return UnknownProject(name, JsonObject(details))
    }

    override fun serialize(encoder: Encoder, value: UnknownProject) {
        error("Serialization is not supported")
    }
}

fun main() {
    // Deserializes JSON with unknown fields into 'UnknownProject'
    println(Json.decodeFromString(UnknownProjectSerializer, """{"type":"unknown","name":"example","maintainer":"Unknown","license":"Apache 2.0"}"""))
    // UnknownProject(name=example, details={"type":"unknown","maintainer":"Unknown","license":"Apache 2.0"})

}
