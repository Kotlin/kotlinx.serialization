// This file was automatically generated from serialization-transform-json.md by Knit tool. Do not edit.
package example.exampleJsonTransform03

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

@Serializable
class Project(val name: String, val language: String)

// Custom serializer that omits the "language" property if it is equal to "Kotlin"
object ProjectSerializer : JsonTransformingSerializer<Project>(Project.serializer()) {
    override fun transformSerialize(element: JsonElement): JsonElement =
        // Omits the "language" property if its value is "Kotlin"
        JsonObject(element.jsonObject.filterNot {
            (k, v) -> k == "language" && v.jsonPrimitive.content == "Kotlin"
        })
}

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    // Uses the plugin-generated serializer
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","language":"Kotlin"}
    println(Json.encodeToString(ProjectSerializer, data)) // using custom serializer
    // {"name":"kotlinx.serialization"}
}
