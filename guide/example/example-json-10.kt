// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson10

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Configures a Json instance to use a custom class discriminator
val format = Json { classDiscriminator = "#class" }

@Serializable
sealed class Project {
    abstract val name: String
}

// Specifies a custom serial name for the OwnedProject class
@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

// Specifies a custom serial name for the SimpleProject class
@Serializable
@SerialName("simple")
class SimpleProject(override val name: String) : Project()

fun main() {
  val simpleProject: Project = SimpleProject("kotlinx.serialization")
  val ownedProject: Project = OwnedProject("kotlinx.coroutines", "kotlin")

  // Serializes SimpleProject with #class: "simple"
  println(format.encodeToString(simpleProject))
  // {"#class":"simple","name":"kotlinx.serialization"}

  // Serializes OwnedProject with #class: "owned"
  println(format.encodeToString(ownedProject))
  // {"#class":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
