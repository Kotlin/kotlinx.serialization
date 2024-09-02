// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleClasses13

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Project(val name: String, val owner: User)

@Serializable
class User(val name: String)

fun main() {
    val owner = User("kotlin")
    val data = Project("kotlinx.serialization", owner)
    println(Json.encodeToString(data))
}
