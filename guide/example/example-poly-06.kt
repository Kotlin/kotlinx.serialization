// This file was automatically generated from serialization-polymorphism.md by Knit tool. Do not edit.
package example.examplePoly06

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed class Project {
    abstract val name: String
}

// Assigns the custom serial name "owned" to OwnedProject for JSON output
@Serializable         
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    // Serializes the object with the custom `type` key "owned" instead of the class name
    println(Json.encodeToString(data))
    // {"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
