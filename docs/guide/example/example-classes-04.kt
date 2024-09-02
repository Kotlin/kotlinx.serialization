// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleClasses04

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization"}
    """)
    println(data)
}
