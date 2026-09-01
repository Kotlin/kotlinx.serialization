// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson33

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class) // useExtraKeys is an experimental setting for now
val format = Json { useExtraKeys = true }

@OptIn(ExperimentalSerializationApi::class) // JsonExtraKeys is an experimental annotation for now
@Serializable
data class Project(
    val name: String,
    @JsonExtraKeys val details: JsonObject = JsonObject(emptyMap())
)

fun main() {
    val project = format.decodeFromString<Project>("""{"type":"unknown","name":"example","maintainer":"Unknown","license":"Apache 2.0"}""")
    println(project)
    println(format.encodeToString(project))
}
