// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson01

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Creates a custom Json format
val format = Json { prettyPrint = true }

@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")

    // Prints the pretty-printed JSON string
    println(format.encodeToString(data))
}
