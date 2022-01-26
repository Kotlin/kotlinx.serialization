// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson01

import kotlinx.serialization.*
import kotlinx.serialization.json.*

val format = Json { prettyPrint = true }

@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    println(format.encodeToString(data))
}
