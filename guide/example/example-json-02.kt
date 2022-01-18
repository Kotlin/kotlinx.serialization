// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson02

import kotlinx.serialization.*
import kotlinx.serialization.json.*

val format = Json { isLenient = true }

enum class Status { SUPPORTED }

@Serializable
data class Project(val name: String, val status: Status, val votes: Int)

fun main() {
    val data = format.decodeFromString<Project>("""
        {
            name   : kotlinx.serialization,
            status : SUPPORTED,
            votes  : "9000"
        }
    """)
    println(data)
}
