// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin04

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// The @Serializable annotation is not needed for enum classes
enum class Status { SUPPORTED }
        
@Serializable
class Project(val name: String, val status: Status) 

fun main() {
    val data = Project("kotlinx.serialization", Status.SUPPORTED)
    println(Json.encodeToString(data))
}
