// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson07

import kotlinx.serialization.*
import kotlinx.serialization.json.*

val format = Json { explicitNulls = false }

@Serializable
data class Project(
    val name: String,
    val language: String,
    val version: String? = "1.2.2",
    val website: String?,
    val description: String? = null
)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin", null, null, null)
    val json = format.encodeToString(data)
    println(json)
    println(format.decodeFromString<Project>(json))
}
