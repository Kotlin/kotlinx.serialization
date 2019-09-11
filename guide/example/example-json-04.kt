// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson04

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Project(@JsonNames(["title"]) val name: String)

fun main() {
  val project = Json.decodeFromString<Project>("""{"name":"kotlinx.serialization"}""")
  println(project)
  val oldProject = Json.decodeFromString<Project>("""{"title":"kotlinx.coroutines"}""")
  println(oldProject)
}
