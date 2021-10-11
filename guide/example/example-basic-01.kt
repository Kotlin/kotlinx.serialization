// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleBasic01

import kotlinx.serialization.*
import kotlinx.serialization.json.*

class Project(val name: String, val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    println(Json.encodeToString(data))
}
