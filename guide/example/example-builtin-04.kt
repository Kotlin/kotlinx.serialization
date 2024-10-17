// This file was automatically generated from serialization-serialize-builtin-types.md by Knit tool. Do not edit.
package example.exampleBuiltin04

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

// The @Serializable annotation is not needed for enum classes
enum class Status { SUPPORTED }
        
@Serializable
class Project(val name: String, val status: Status) 

fun main() {
    val data = Project("kotlinx.serialization", Status.SUPPORTED)
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","status":"SUPPORTED"}
}
