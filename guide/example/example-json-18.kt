// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson18

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Project(val name: String, val language: String)

object ProjectSerializer : JsonTransformingSerializer<Project>(Project.serializer()) {
    override fun transformSerialize(element: JsonElement): JsonElement =
        // Filter out top-level key value pair with the key "language" and the value "Kotlin"
        JsonObject(element.jsonObject.filterNot {
            (k, v) -> k == "language" && v.jsonPrimitive.content == "Kotlin"
        })
}

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    println(Json.encodeToString(data)) // using plugin-generated serializer
    println(Json.encodeToString(ProjectSerializer, data)) // using custom serializer
}
