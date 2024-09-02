// This file was automatically generated from serialization-customization-options.md by Knit tool. Do not edit.
package example.exampleClasses08

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
// The language property is abbreviated to lang using @SerialName
class Project(val name: String, @SerialName("lang") val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    // In the JSON output, the abbreviated name lang is used instead of the full property name
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","lang":"Kotlin"}
}
