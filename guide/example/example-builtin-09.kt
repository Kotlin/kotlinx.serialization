// This file was automatically generated from serialization-serialize-builtin-types.md by Knit tool. Do not edit.
package example.exampleBuiltin09

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

@Serializable
class Project(val name: String)

fun main() {
    val map = mapOf(
        1 to Project("kotlinx.serialization"),
        2 to Project("kotlinx.coroutines")    
    )
    println(Json.encodeToString(map))
    // {"1":{"name":"kotlinx.serialization"},"2":{"name":"kotlinx.coroutines"}}
}
