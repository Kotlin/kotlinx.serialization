// This file was automatically generated from serialization-polymorphism.md by Knit tool. Do not edit.
package example.examplePoly10

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

// Creates a SerializersModule to register the implementing classes of the interface
val module = SerializersModule {
    polymorphic(Project::class) {
        subclass(OwnedProject::class)
    }
}

val format = Json { serializersModule = module }

// Declares an interface used for polymorphic serialization
interface Project {
    val name: String
}

// OwnedProject implements the Project interface 
@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project

fun main() {
    // Declares `data` with the type of `Project`, which is assigned an instance of `OwnedProject`
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(format.encodeToString(data))
    // {"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
