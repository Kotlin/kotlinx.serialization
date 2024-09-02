// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleBasic03

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(data)
}
