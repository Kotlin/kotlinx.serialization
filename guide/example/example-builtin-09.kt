// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin09

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Project(val name: String)

fun main() {
    val set = setOf(
        Project("kotlinx.serialization"),
        Project("kotlinx.coroutines")    
    )
    println(Json.encodeToString(set))
}  
