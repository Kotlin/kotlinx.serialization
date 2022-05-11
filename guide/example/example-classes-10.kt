// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleClasses10

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Project(
    val name: String,
    @EncodeDefault val language: String = "Kotlin"
)


@Serializable
data class User(
    val name: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val projects: List<Project> = emptyList()
)

fun main() {
    val userA = User("Alice", listOf(Project("kotlinx.serialization")))
    val userB = User("Bob")
    println(Json.encodeToString(userA))
    println(Json.encodeToString(userB))
}
