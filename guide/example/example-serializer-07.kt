// This file was automatically generated from create-custom-serializers.md by Knit tool. Do not edit.
package example.exampleSerializer07

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*

// Marks the Box<T> class with @Serializable and specifies a custom serializer
@Serializable(with = BoxSerializer::class)
data class Box<T>(val contents: T)

// Creates a custom serializer as a class for Box<T>
class BoxSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<Box<T>> {
    // Uses the descriptor from the provided KSerializer to define the structure of Box<T>
    override val descriptor: SerialDescriptor = dataSerializer.descriptor
    // Delegates serialization and deserialization
    override fun serialize(encoder: Encoder, value: Box<T>) = dataSerializer.serialize(encoder, value.contents)
    override fun deserialize(decoder: Decoder) = Box(dataSerializer.deserialize(decoder))
}

@Serializable
data class Project(val name: String)

fun main() {
    val box = Box(Project("kotlinx.serialization"))
    val string = Json.encodeToString(box)
    println(string)
    // {"name":"kotlinx.serialization"}
    println(Json.decodeFromString<Box<Project>>(string))
    // Box(contents=Project(name=kotlinx.serialization))
}
