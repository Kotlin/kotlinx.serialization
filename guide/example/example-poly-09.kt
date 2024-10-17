// This file was automatically generated from serialization-polymorphism.md by Knit tool. Do not edit.
package example.examplePoly09

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

// Defines a SerializersModule with polymorphic serialization
val module = SerializersModule {
    polymorphic(Project::class) {
        // Registers OwnedProject as a subclass of Project
        subclass(OwnedProject::class)
    }
}

// Creates a custom JSON format with the module
val format = Json { serializersModule = module }

// Defines an abstract serializable class Project with an abstract property `name`
@Serializable
abstract class Project {
    abstract val name: String
}

// Defines a subclass OwnedProject with an additional property `owner` and a SerialName
@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    // Serializes data using the custom format
    println(format.encodeToString(data))
    // {"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
