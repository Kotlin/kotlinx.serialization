// This file was automatically generated from serialization-transform-json.md by Knit tool. Do not edit.
package example.exampleJsonTransform02

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

@Serializable
data class Project(
  val name: String,
  @Serializable(with = UserListSerializer::class)
  val users: List<User>
)

@Serializable
data class User(val name: String)

// Unwraps single-element lists into a single object during serialization
object UserListSerializer : JsonTransformingSerializer<List<User>>(ListSerializer(User.serializer())) {

    override fun transformSerialize(element: JsonElement): JsonElement {
        // Ensures that the input is a list
        require(element is JsonArray)
        // Unwraps single-element lists into a single JSON object
        return element.singleOrNull() ?: element
    }
}
  
fun main() {
    val data = Project("kotlinx.serialization", listOf(User("kotlin")))
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","users":{"name":"kotlin"}}
}
