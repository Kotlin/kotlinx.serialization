// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson21

import kotlinx.serialization.*
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
        // Cast to JSON-specific interface
        val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        // Read the whole content as JSON
        val json = jsonInput.decodeJsonElement().jsonObject
        // Extract and remove name property
        val name = json.getValue("name").jsonPrimitive.content
        val details = json.toMutableMap()
        details.remove("name")
        return UnknownProject(name, JsonObject(details))
    }

    override fun serialize(encoder: Encoder, value: UnknownProject) {
        error("Serialization is not supported")
    }
}

fun main() {
    println(Json.decodeFromString(UnknownProjectSerializer, """{"type":"unknown","name":"example","maintainer":"Unknown","license":"Apache 2.0"}"""))
}
