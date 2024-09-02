// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson13

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Configures a Json instance to omit the class discriminator from the output
val format = Json { classDiscriminatorMode = ClassDiscriminatorMode.NONE }

@Serializable
sealed class Project {
    abstract val name: String
}

@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    // Serializes without a discriminator
    println(format.encodeToString(data))
    // {"name":"kotlinx.coroutines","owner":"kotlin"}
}
