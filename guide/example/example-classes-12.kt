// This file was automatically generated from serialization-customization-options.md by Knit tool. Do not edit.
package example.exampleClasses12

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports the necessary libraries
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
// Marks the `language` property as required
data class Project(val name: String, @Required val language: String = "Kotlin")

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization"}
    """)
    println(data)
    // MissingFieldException
}
