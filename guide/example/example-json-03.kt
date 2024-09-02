// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson03

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Configures a Json instance to ignore unknown keys
val format = Json { ignoreUnknownKeys = true }

@Serializable
data class Project(val name: String)

fun main() {
    val data = format.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    // Decodes the object even though the `Project` class doesn't have the `language` property
    println(data)
    // Project(name=kotlinx.serialization)
}
