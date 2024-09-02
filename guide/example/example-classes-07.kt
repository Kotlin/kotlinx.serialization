// This file was automatically generated from serialization-customization-options.md by Knit tool. Do not edit.
package example.exampleClasses07

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
// The `Box<T>` class can be used with built-in types like `Int`, or with user-defined types like `Project`.
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
    // {"a":{"contents":42},"b":{"contents":{"name":"kotlinx.serialization","language":"Kotlin"}}}
}
