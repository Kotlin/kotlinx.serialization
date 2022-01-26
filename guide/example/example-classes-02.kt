// This file was automatically generated from basic-serialization.md by Knit tool. Do not edit.
package example.exampleClasses02

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Project private constructor(val owner: String, val name: String) {
    constructor(path: String) : this(
        owner = path.substringBefore('/'),
        name = path.substringAfter('/')
    )

    val path: String
        get() = "$owner/$name"
}

fun main() {
    println(Json.encodeToString(Project("kotlin/kotlinx.serialization")))
}
