// This file was automatically generated from serialization-customization-options.md by Knit tool. Do not edit.
package example.exampleClasses01

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Project(
    // name is a property with backing field -- serialized
    var name: String
) {
    // stars is property with a backing field -- serialized
    var stars: Int = 0

    // path is getter only, no backing field -- not serialized
    val path: String
        get() = "kotlin/$name"

    // id is a delegated property -- not serialized
    var id by ::name
}

fun main() {
    val data = Project("kotlinx.serialization").apply { stars = 9000 }
    // Only the name and the stars properties are present in the JSON output.
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","stars":9000}
}
