// This file was automatically generated from serialization-polymorphism.md by Knit tool. Do not edit.
package example.examplePoly01

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Defines an open class Project with a name property
@Serializable
open class Project(val name: String)

// Defines a derived class OwnedProject with an additional owner property
class OwnedProject(name: String, val owner: String) : Project(name)

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    // Serializes data based on the static type Project, ignoring the OwnedProject properties
    println(Json.encodeToString(data))
    // {"name":"kotlinx.coroutines"}
}
