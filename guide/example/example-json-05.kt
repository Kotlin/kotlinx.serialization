// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson05

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class) // JsonNames is an experimental annotation for now
@Serializable
data class Project(@JsonNames("title") val name: String)

fun main() {
  val project = Json.decodeFromString<Project>("""{"name":"kotlinx.serialization"}""")
  println(project)
  val oldProject = Json.decodeFromString<Project>("""{"title":"kotlinx.coroutines"}""")
  println(oldProject)
}
