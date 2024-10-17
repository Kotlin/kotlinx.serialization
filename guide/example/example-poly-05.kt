// This file was automatically generated from serialization-polymorphism.md by Knit tool. Do not edit.
package example.examplePoly05

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
    // Sets the static type as OwnedProject
    val data = OwnedProject("kotlinx.coroutines", "kotlin")

    // Specifies the base type Project, which includes the `type` discriminator in the output.
    println(Json.encodeToString<Project>(data))
    // {"type":"OwnedProject","name":"kotlinx.coroutines","owner":"kotlin"}
}
