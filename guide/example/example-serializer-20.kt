// This file was automatically generated from serializers.md by Knit tool. Do not edit.
package example.exampleSerializer20

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

// NOT @Serializable, will use external serializer
class Project(
    // val in a primary constructor -- serialized
    val name: String
) {
    var stars: Int = 0 // property with getter & setter -- serialized
 
    val path: String // getter only -- not serialized
        get() = "kotlin/$name"                                         

    private var locked: Boolean = false // private, not accessible -- not serialized 
}              

@Serializer(forClass = Project::class)
object ProjectSerializer

fun main() {
    val data = Project("kotlinx.serialization").apply { stars = 9000 }
    println(Json.encodeToString(ProjectSerializer, data))
}
