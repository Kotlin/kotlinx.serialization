// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson06

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

val format = Json { coerceInputValues = true }

@Serializable
data class Project(val name: String, val language: String = "Kotlin")

fun main() {
    val data = format.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":null}
    """)

    // The invalid `null` value for `language` is coerced to its default value
    println(data)
    // Project(name=kotlinx.serialization, language=Kotlin)
}
