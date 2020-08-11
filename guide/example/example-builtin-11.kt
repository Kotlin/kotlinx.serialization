// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin11

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
object SerializationVersion {
    val libraryVersion: String = "1.0.0"
}

fun main() {
    println(Json.encodeToString(SerializationVersion))
    println(Json.encodeToString(Unit))
}  
