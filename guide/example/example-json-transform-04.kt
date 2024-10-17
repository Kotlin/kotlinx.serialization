// This file was automatically generated from serialization-transform-json.md by Knit tool. Do not edit.
package example.exampleJsonTransform04

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

@Serializable
abstract class Project {
    abstract val name: String
}

@Serializable
data class BasicProject(override val name: String): Project()


@Serializable
data class OwnedProject(override val name: String, val owner: String) : Project()

// Custom serializer that selects deserializer based on the presence of "owner"
object ProjectSerializer : JsonContentPolymorphicSerializer<Project>(Project::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        // Distinguishes the BasicProject and OwnedProject subclasses by the presence of "owner" key
        "owner" in element.jsonObject -> OwnedProject.serializer()
        else -> BasicProject.serializer()
    }
}

fun main() {
    val data = listOf(
        OwnedProject("kotlinx.serialization", "kotlin"),
        BasicProject("example")
    )
    val string = Json.encodeToString(ListSerializer(ProjectSerializer), data)
    // No class discriminator is added in the JSON output
    println(string)
    // [{"name":"kotlinx.serialization","owner":"kotlin"},{"name":"example"}]
    println(Json.decodeFromString(ListSerializer(ProjectSerializer), string))
    // [OwnedProject(name=kotlinx.serialization, owner=kotlin), BasicProject(name=example)]
}
