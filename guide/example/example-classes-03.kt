// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleClasses03

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Project(val name: String) {
    init {
        require(name.isNotEmpty()) { "name cannot be empty" }
    }
}

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":""}
    """)
    println(data)
}
