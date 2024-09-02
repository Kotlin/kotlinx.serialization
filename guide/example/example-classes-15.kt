// This file was automatically generated from serialization-customization-options.md by Knit tool. Do not edit.
package example.exampleClasses15

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Project(
    val name: String,
    // The 'language' property will always be included in the serialized output, even if it has the default value "Kotlin"
    @EncodeDefault val language: String = "Kotlin"
)

@Serializable
data class User(
    val name: String,
    // The 'projects' property will never be included in the serialized output, even if it has a value
    // Since the default value is an empty list, 'projects' will be omitted unless it contains elements
    @EncodeDefault(EncodeDefault.Mode.NEVER) val projects: List<Project> = emptyList()
)

fun main() {
    val userA = User("Alice", listOf(Project("kotlinx.serialization")))
    val userB = User("Bob")
    // 'projects' is serialized because it contains a value, and 'language' is always serialized
    println(Json.encodeToString(userA))
    // {"name":"Alice","projects":[{"name":"kotlinx.serialization","language":"Kotlin"}]}

    // 'projects' is omitted because it's an empty list and EncodeDefault.Mode is set to NEVER, so it's not serialized
    println(Json.encodeToString(userB))
    // {"name":"Bob"}
}
