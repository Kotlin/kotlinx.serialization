// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin06

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable // required because of @SerialName
enum class Status { @SerialName("maintained") SUPPORTED }
        
@Serializable
class Project(val name: String, val status: Status) 

fun main() {
    val data = Project("kotlinx.serialization", Status.SUPPORTED)
    println(Json.encodeToString(data))
}
