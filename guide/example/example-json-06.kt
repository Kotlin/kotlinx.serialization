// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson06

import kotlinx.serialization.*
import kotlinx.serialization.json.*

val format = Json { encodeDefaults = true }

@Serializable
class Project(
    val name: String,
    val language: String = "Kotlin",
    val website: String? = null
)

fun main() {
    val data = Project("kotlinx.serialization")
    println(format.encodeToString(data))
}
