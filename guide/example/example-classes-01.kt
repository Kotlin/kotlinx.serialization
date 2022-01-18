// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleClasses01

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Project(
    // name is a property with backing field -- serialized
    var name: String
) {
    var stars: Int = 0 // property with a backing field -- serialized

    val path: String // getter only, no backing field -- not serialized
        get() = "kotlin/$name"

    var id by ::name // delegated property -- not serialized
}

fun main() {
    val data = Project("kotlinx.serialization").apply { stars = 9000 }
    println(Json.encodeToString(data))
}
