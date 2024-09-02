// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson05

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Configures a Json instance to encode default values
val format = Json { encodeDefaults = true }

@Serializable
class Project(
    val name: String,
    val language: String = "Kotlin",
    val website: String? = null
)

fun main() {
    val data = Project("kotlinx.serialization")

    // Encodes all the property values including the default ones
    println(format.encodeToString(data))
    // {"name":"kotlinx.serialization","language":"Kotlin","website":null}
}
