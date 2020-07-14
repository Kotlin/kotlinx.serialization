// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin05

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// @Serializable annotation is not need for a enum classes
enum class Status { SUPPORTED }
        
@Serializable
class Project(val name: String, val status: Status) 

fun main() {
    val data = Project("kotlinx.serialization", Status.SUPPORTED)
    println(Json.encodeToString(data))
}
