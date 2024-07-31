// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson29

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed class Project {
    abstract val name: String
}

@KeepGeneratedSerializer
@Serializable(with = BasicProjectSerializer::class)
@SerialName("basic")
data class BasicProject(override val name: String): Project()

object BasicProjectSerializer : JsonTransformingSerializer<BasicProject>(BasicProject.generatedSerializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val jsonObject = element.jsonObject
        return if ("basic-name" in jsonObject) {
            val nameElement = jsonObject["basic-name"] ?: throw IllegalStateException()
            JsonObject(mapOf("name" to nameElement))
        } else {
            jsonObject
        }
    }
}


fun main() {
    val project = Json.decodeFromString<Project>("""{"type":"basic","basic-name":"example"}""")
    println(project)
}
