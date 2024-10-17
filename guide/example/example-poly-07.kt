// This file was automatically generated from serialization-polymorphism.md by Knit tool. Do not edit.
package example.examplePoly07

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed class Project {
    abstract val name: String
    // Defines a property with a backing field in the base class
    var status = "open"
}
            
@Serializable   
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    // Includes default values like "status"
    val json = Json { encodeDefaults = true }
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    // Serializes superclass properties before subclass properties
    println(json.encodeToString(data))
    // {"type":"owned","status":"open","name":"kotlinx.coroutines","owner":"kotlin"}
}
