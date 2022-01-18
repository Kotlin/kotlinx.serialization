// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson08

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
    println(format.encodeToString(map))
}
