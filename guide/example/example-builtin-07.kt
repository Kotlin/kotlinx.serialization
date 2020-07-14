// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin07

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Project(val name: String)

fun main() {
    val pair = 1 to Project("kotlinx.serialization")
    println(Json.encodeToString(pair))
}  
