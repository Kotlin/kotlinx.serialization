// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson15

@Serializable
data class Project(val projectName: String, val projectOwner: String)

val format = Json { namingStrategy = JsonNamingStrategy.SnakeCase }

fun main() {
    val project = format.decodeFromString<Project>("""{"project_name":"kotlinx.coroutines", "project_owner":"Kotlin"}""")
    println(format.encodeToString(project.copy(projectName = "kotlinx.serialization")))
}
