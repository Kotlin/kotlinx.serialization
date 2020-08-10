// This file was automatically generated from polymorphism.md by Knit tool. Do not edit.
package example.examplePoly17

import kotlinx.serialization.*
import kotlinx.serialization.json.*

import kotlinx.serialization.modules.*

@Serializable
abstract class Project {
    abstract val name: String
}

@Serializable
@SerialName("OwnedProject")
data class OwnedProject(override val name: String, val owner: String) : Project()

val module = SerializersModule {
    polymorphic(Project::class) {
        subclass(OwnedProject::class)
    }
}

val format = Json { serializersModule = module }

fun main() {
    println(format.decodeFromString<Project>("""
        {"type":"unknown","name":"example"}
    """))
}
