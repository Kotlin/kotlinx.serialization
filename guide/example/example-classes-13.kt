// This file was automatically generated from serialization-customization-options.md by Knit tool. Do not edit.
package example.exampleClasses13

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports the necessary libraries
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

@Serializable
// Excludes the `language` property from serialization
data class Project(val name: String, @Transient val language: String = "Kotlin")

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(data)
    // JsonDecodingException
}
