// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleClasses07

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Project(val name: String, @Required val language: String = "Kotlin")

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization"}
    """)
    println(data)
}
