// This file was automatically generated from serialization-polymorphism.md by Knit tool. Do not edit.
package example.examplePoly03

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed class Project {
    abstract val name: String
}

@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

// Serializes data of compile-time type Project
fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    // A `type` key is added to the resulting JSON object as a discriminator.
    println(Json.encodeToString(data))
    // {"type":"OwnedProject","name":"kotlinx.coroutines","owner":"kotlin"}
}
