// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson03

import kotlinx.serialization.*
import kotlinx.serialization.json.*

val format = Json { ignoreUnknownKeys = true }

@Serializable
data class Project(val name: String)

fun main() {
    val data = format.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(data)
}
