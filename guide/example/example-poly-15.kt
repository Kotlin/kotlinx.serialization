// This file was automatically generated from serialization-polymorphism.md by Knit tool. Do not edit.
package example.examplePoly15

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

// Defines an abstract response class with a generic parameter T
@Serializable
abstract class Response<out T>

// Represents a successful response with a generic data type
@Serializable
@SerialName("OkResponse")
data class OkResponse<out T>(val data: T) : Response<T>()

// Defines the abstract class Project
@Serializable
abstract class Project {
    abstract val name: String
}

// Concrete subclass of Project
@Serializable
@SerialName("OwnedProject")
data class OwnedProject(override val name: String, val owner: String) : Project()

// Defines a serializers module for polymorphic classes
val responseModule = SerializersModule {
    polymorphic(Response::class) {
        // Registers the polymorphic serializer for OkResponse
        subclass(OkResponse.serializer(PolymorphicSerializer(Any::class)))
    }
    polymorphic(Any::class) {
        // Registers OwnedProject as a subclass of Any
        subclass(OwnedProject::class)
    }
    polymorphic(Project::class) {
        // Registers OwnedProject as a subclass of Project
        subclass(OwnedProject::class)
    }
}

// Creates a Json format with the registered serializers
val format = Json { serializersModule = responseModule }

fun main() {
    // Creates an instance of OkResponse with a Project subtype
    val data: Response<Project> = OkResponse(OwnedProject("kotlinx.serialization", "kotlin"))

    // Serializes the data to JSON
    val jsonString = format.encodeToString(data)
    println(jsonString)
    // {"type":"OkResponse","data":{"type":"OwnedProject","name":"kotlinx.serialization","owner":"kotlin"}}

    // Deserializes the JSON back to Response<Project>
    val deserializedData = format.decodeFromString<Response<Project>>(jsonString)
    println(deserializedData)
    // OkResponse(data=OwnedProject(name=kotlinx.serialization, owner=kotlin))
}
