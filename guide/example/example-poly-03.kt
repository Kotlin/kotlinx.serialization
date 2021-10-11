// This file was automatically generated from polymorphism.md by Knit tool. Do not edit.
package example.examplePoly03

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
abstract class Project {
    abstract val name: String
}

class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(Json.encodeToString(data))
}  
