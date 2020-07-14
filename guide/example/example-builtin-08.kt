// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin08

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Project(val name: String)

fun main() {
    val list = listOf(
        Project("kotlinx.serialization"),
        Project("kotlinx.coroutines")    
    )
    println(Json.encodeToString(list))
}  
