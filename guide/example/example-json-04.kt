// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson04

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
// Maps both "name" and "title" JSON fields to the `name` property
data class Project(@JsonNames("title") val name: String)

fun main() {
    val project = Json.decodeFromString<Project>("""{"name":"kotlinx.serialization"}""")
    println(project)
    // Project(name=kotlinx.serialization)

    val oldProject = Json.decodeFromString<Project>("""{"title":"kotlinx.coroutines"}""")
    // Both `name` and `title` Json fields correspond to `name` property
    println(oldProject)
    // Project(name=kotlinx.coroutines)
}
