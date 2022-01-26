// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleClasses15

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Box<T>(val contents: T)

@Serializable
data class Project(val name: String, val language: String)

@Serializable
class Data(
    val a: Box<Int>,
    val b: Box<Project>
)

fun main() {
    val data = Data(Box(42), Box(Project("kotlinx.serialization", "Kotlin")))
    println(Json.encodeToString(data))
}
