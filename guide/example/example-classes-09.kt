// This file was automatically generated from serialization-customization-options.md by Knit tool. Do not edit.
package example.exampleClasses09

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Project private constructor(val owner: String, val name: String) {
    // Creates a Project object using a path string
    constructor(path: String) : this(
        owner = path.substringBefore('/'),
        name = path.substringAfter('/')
    )

    val path: String
        get() = "$owner/$name"
}
fun main() {
    println(Json.encodeToString(Project("kotlin/kotlinx.serialization")))
    // {"owner":"kotlin","name":"kotlinx.serialization"}
}
