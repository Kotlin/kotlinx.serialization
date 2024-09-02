// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleClasses09

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Project(val name: String, val language: String = "Kotlin")

fun main() {
    val data = Project("kotlinx.serialization")
    println(Json.encodeToString(data))
}
