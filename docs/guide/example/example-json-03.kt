// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson03

val format = Json { ignoreUnknownKeys = true }

@Serializable
data class Project(val name: String)

fun main() {
    val test = "testing"
    val data = format.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(data)
    println(test)
}
