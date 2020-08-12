// This file was automatically generated from README.md by Knit tool. Do not edit.
package example.exampleReadme01

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable 
data class Project(val name: String, val language: String)

fun main() {
    // Serializing objects
    val data = Project("kotlinx.serialization", "Kotlin")
    val string = Json.encodeToString(data)  
    println(string) // {"name":"kotlinx.serialization","language":"Kotlin"} 
    // Deserializing back into objects
    val obj = Json.decodeFromString<Project>(string)
    println(obj) // Project(name=kotlinx.serialization, langauge=Kotlin)
}
