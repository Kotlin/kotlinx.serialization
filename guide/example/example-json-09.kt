// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson09

import kotlinx.serialization.*
import kotlinx.serialization.json.*

val format = Json { allowStructuredMapKeys = true }

@Serializable
data class Project(val name: String)

fun main() {
    val map = mapOf(
        Project("kotlinx.serialization") to "Serialization",
        Project("kotlinx.coroutines") to "Coroutines"
    )
    // Serializes the map with structured keys as a JSON array:
    // `[key1, value1, key2, value2,...]`.
    println(format.encodeToString(map))
    // [{"name":"kotlinx.serialization"},"Serialization",{"name":"kotlinx.coroutines"},"Coroutines"]
}
