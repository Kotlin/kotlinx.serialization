// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleClasses16

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Project(val name: String, @SerialName("lang") val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    println(Json.encodeToString(data))
}
