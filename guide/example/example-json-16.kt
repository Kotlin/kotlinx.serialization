// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson16

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
    // If response is not an array, then it is a single object that should be wrapped into the array
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element !is JsonArray) JsonArray(listOf(element)) else element
}

fun main() {
    println(Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","users":{"name":"kotlin"}}
    """))
    println(Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","users":[{"name":"kotlin"},{"name":"jetbrains"}]}
    """))
}
