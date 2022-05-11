// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleClasses06

import kotlinx.serialization.*
import kotlinx.serialization.json.*

fun computeLanguage(): String {
    println("Computing")
    return "Kotlin"
}

@Serializable
data class Project(val name: String, val language: String = computeLanguage())

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(data)
}
