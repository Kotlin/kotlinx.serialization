// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson05

import kotlinx.serialization.*
import kotlinx.serialization.json.*

val format = Json { coerceInputValues = true }

@Serializable
data class Project(val name: String, val language: String = "Kotlin")

fun main() {
    val data = format.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":null}
    """)
    println(data)
}
