// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson16

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Project(val projectName: String, val projectOwner: String)

@OptIn(ExperimentalSerializationApi::class) // namingStrategy is an experimental setting for now
val format = Json { namingStrategy = JsonNamingStrategy.SnakeCase }

fun main() {
    val project = format.decodeFromString<Project>("""{"project_name":"kotlinx.coroutines", "project_owner":"Kotlin"}""")
    println(format.encodeToString(project.copy(projectName = "kotlinx.serialization")))
}
