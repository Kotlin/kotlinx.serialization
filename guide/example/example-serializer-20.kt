// This file was automatically generated from serializers.md by Knit tool. Do not edit.
package example.exampleSerializer20

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

// NOT @Serializable, will use external serializer
class Project(
    // val in a primary constructor -- serialized
    val name: String,
    var stars: Int
) {
}              

@Serializer(forClass = Project::class)
object ProjectSerializer

fun main() {
    val data = Project("kotlinx.serialization", 9000)
    println(Json.encodeToString(ProjectSerializer, data))
}
