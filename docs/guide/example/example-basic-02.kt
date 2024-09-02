// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleBasic02

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Project(val name: String, val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    println(Json.encodeToString(data))
}
