// This file was automatically generated from serialization-customization-options.md by Knit tool. Do not edit.
package example.exampleClasses04

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

fun computeLanguage(): String {
    println("Computing")
    return "Kotlin"
}

@Serializable
// Initializer is skipped if language is in input
data class Project(val name: String, val language: String = computeLanguage())

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Java"}
    """)
    println(data)
    // Project(name=kotlinx.serialization, language=Java)
}
