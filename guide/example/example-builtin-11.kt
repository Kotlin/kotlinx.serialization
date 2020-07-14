// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin11

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Project(val name: String)

fun main() {
    val map = mapOf(
        1 to Project("kotlinx.serialization"),
        2 to Project("kotlinx.coroutines")    
    )
    println(Json.encodeToString(map))
}  
