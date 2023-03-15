// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson04

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
@JsonIgnoreProperties("language", "description")
abstract class BaseProject

data class ConcreteProject(val name: String) : BaseProject

@Serializable
@JsonIgnoreProperties("language", "description")
data class Project(val name: String)

fun main() {
    val data = format.decodeFromString<ConcreteProject>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(data)

  val data = format.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
  println(data)
}
