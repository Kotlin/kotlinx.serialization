// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson14

import kotlinx.serialization.*
import kotlinx.serialization.json.*

import kotlinx.serialization.builtins.*

@Serializable 
data class Project(
    val name: String,
    @Serializable(with = UserListSerializer::class)      
    val users: List<User>
)

@Serializable
data class User(val name: String)

object UserListSerializer : JsonTransformingSerializer<List<User>>(ListSerializer(User.serializer())) {

    override fun transformSerialize(element: JsonElement): JsonElement {
        require(element is JsonArray) // we are using this serializer with lists only
        return element.singleOrNull() ?: element
    }
}

fun main() {     
    val data = Project("kotlinx.serialization", listOf(User("kotlin")))
    println(Json.encodeToString(data))
}
