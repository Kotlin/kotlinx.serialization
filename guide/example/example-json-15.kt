// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson15

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Project(val projectName: String, val projectOwner: String)

// Configures a Json instance to apply SnakeCase naming strategy
val format = Json { namingStrategy = JsonNamingStrategy.SnakeCase }

fun main() {
    val project = format.decodeFromString<Project>("""{"project_name":"kotlinx.coroutines", "project_owner":"Kotlin"}""")
    // Serializes and deserializes as if all serial names are transformed from camel case to snake case
    println(format.encodeToString(project.copy(projectName = "kotlinx.serialization")))
    // {"project_name":"kotlinx.serialization","project_owner":"Kotlin"}
}
