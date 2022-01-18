// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson10

import kotlinx.serialization.*
import kotlinx.serialization.json.*

val format = Json { classDiscriminator = "#class" }

@Serializable
sealed class Project {
    abstract val name: String
}

@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(format.encodeToString(data))
}
