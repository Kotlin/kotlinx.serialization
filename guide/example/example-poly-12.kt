// This file was automatically generated from serialization-polymorphism.md by Knit tool. Do not edit.
package example.examplePoly12

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

val module = SerializersModule {
    // Registers OwnedProject as a subclass of Any for polymorphic serialization
    polymorphic(Any::class) {
        subclass(OwnedProject::class)
    }
}

val format = Json { serializersModule = module }

@Serializable
abstract class Project {
    abstract val name: String
}

@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    // Declares data as Any, requiring explicit handling of polymorphism
    val data: Any = OwnedProject("kotlinx.coroutines", "kotlin")
    // Uses PolymorphicSerializer to serialize data of type Any
    println(format.encodeToString(PolymorphicSerializer(Any::class), data))
    // {"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
