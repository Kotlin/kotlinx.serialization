// This file was automatically generated from serializers.md by Knit tool. Do not edit.
package example.exampleSerializer19

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

// NOT @Serializable
class Project(val name: String, val language: String)
                           
@Serializer(forClass = Project::class)
object ProjectSerializer

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    println(Json.encodeToString(ProjectSerializer, data))    
}
