// This file was automatically generated from polymorphism.md by Knit tool. Do not edit.
package example.examplePoly02

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
open class Project(val name: String)

class OwnedProject(name: String, val owner: String) : Project(name)

fun main() {
    val data = OwnedProject("kotlinx.coroutines", "kotlin")
    println(Json.encodeToString(data))
}  
