// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleClasses11

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Project(val name: String, val renamedTo: String? = null)

fun main() {
    val data = Project("kotlinx.serialization")
    println(Json.encodeToString(data))
}
