// This file was automatically generated from serialization-customization-options.md by Knit tool. Do not edit.
package example.exampleClasses02

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
// The 'renamedTo' property is nullable and defaults to null, and it's not encoded
class Project(val name: String, val renamedTo: String? = null)

fun main() {
    val data = Project("kotlinx.serialization")
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization"}
}
