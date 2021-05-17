// This file was automatically generated from polymorphism.md by Knit tool. Do not edit.
package example.examplePoly05

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed class Project {
    abstract val name: String
}
            
@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data = OwnedProject("kotlinx.coroutines", "kotlin") // data: OwnedProject here
    println(Json.encodeToString(data)) // Serializing data of compile-time type OwnedProject
}  
