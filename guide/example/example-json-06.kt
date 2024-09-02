// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson06

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Configures a Json instance to omit null values during serialization
val format = Json { explicitNulls = false }

@Serializable
data class Project(
    val name: String,
    val language: String,
    val version: String? = "1.2.2",
    val website: String?,
    val description: String? = null
)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin", null, null, null)
    val json = format.encodeToString(data)

    // The version, website, and description fields are omitted from the output JSON
    println(json)
    // {"name":"kotlinx.serialization","language":"Kotlin"}

    // Missing nullable fields without defaults are treated as null
    // Fields with defaults are filled with their default values
    println(format.decodeFromString<Project>(json))
    // Project(name=kotlinx.serialization, language=Kotlin, version=1.2.2, website=null, description=null)
}
