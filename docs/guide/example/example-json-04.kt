// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson04

@Serializable
data class Project(@JsonNames("title") val name: String)

fun main() {
  val project = Json.decodeFromString<Project>("""{"name":"kotlinx.serialization"}""")
  println(project)
  val oldProject = Json.decodeFromString<Project>("""{"title":"kotlinx.coroutines"}""")
  println(oldProject)
}
