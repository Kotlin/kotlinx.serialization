// This file was automatically generated from serialization-polymorphism.md by Knit tool. Do not edit.
package example.examplePoly04

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed class Project {
    abstract val name: String
}
            
@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    // The static type is OwnedProject
    val data = OwnedProject("kotlinx.coroutines", "kotlin")
    
    // The `type` discriminator is not included because the static type is OwnedProject.
    println(Json.encodeToString(data))
    // {"name":"kotlinx.coroutines","owner":"kotlin"}
}
