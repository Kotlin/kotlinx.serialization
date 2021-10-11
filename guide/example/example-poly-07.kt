// This file was automatically generated from polymorphism.md by Knit tool. Do not edit.
package example.examplePoly07

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed class Project {
    abstract val name: String   
    var status = "open"
}
            
@Serializable   
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val json = Json { encodeDefaults = true } // "status" will be skipped otherwise
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(json.encodeToString(data))
}  
