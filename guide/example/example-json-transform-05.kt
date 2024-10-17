// This file was automatically generated from serialization-transform-json.md by Knit tool. Do not edit.
package example.exampleJsonTransform05

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

// Defines a sealed class for API responses
@Serializable(with = ResponseSerializer::class)
sealed class Response<out T> {
    data class Ok<out T>(val data: T) : Response<T>()
    data class Error(val message: String) : Response<Nothing>()
}

// Implements custom serialization logic for Response class
class ResponseSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<Response<T>> {
    override val descriptor: SerialDescriptor = buildSerialDescriptor("Response", PolymorphicKind.SEALED) {
        element("Ok", dataSerializer.descriptor)
        element("Error", buildClassSerialDescriptor("Error") {
          element<String>("message")
        })
    }

    // Deserializes Response from JSON
    override fun deserialize(decoder: Decoder): Response<T> {
        // Decoder -> JsonDecoder
        // Ensures the decoder is a JsonDecoder
        require(decoder is JsonDecoder)
        // JsonDecoder -> JsonElement
        val element = decoder.decodeJsonElement()
        // JsonElement -> value
        if (element is JsonObject && "error" in element)
            return Response.Error(element["error"]!!.jsonPrimitive.content)
        return Response.Ok(decoder.json.decodeFromJsonElement(dataSerializer, element))
    }

    // Serializes Response to JSON
    override fun serialize(encoder: Encoder, value: Response<T>) {
        // Encoder -> JsonEncoder
        // Ensures the encoder is a JsonEncoder
        require(encoder is JsonEncoder)
        // value -> JsonElement
        val element = when (value) {
            is Response.Ok -> encoder.json.encodeToJsonElement(dataSerializer, value.data)
            is Response.Error -> buildJsonObject { put("error", value.message) }
        }
        // JsonElement -> JsonEncoder
        encoder.encodeJsonElement(element)
    }
}

@Serializable
data class Project(val name: String)

fun main() {
    val responses = listOf(
        Response.Ok(Project("kotlinx.serialization")),
        Response.Error("Not found")
    )
    val string = Json.encodeToString(responses)
    println(string)
    // [{"name":"kotlinx.serialization"},{"error":"Not found"}]
    println(Json.decodeFromString<List<Response<Project>>>(string))
    // [Ok(data=Project(name=kotlinx.serialization)), Error(message=Not found)]
}
