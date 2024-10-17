// This file was automatically generated from serialization-json-elements.md by Knit tool. Do not edit.
package example.exampleJsonElements02

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

fun main() {
    val element = Json.parseToJsonElement("""
        {
            "name": "kotlinx.serialization",
            "forks": [{"votes": 42}, {"votes": 9000}, {}]
        }
    """)
    // Sums `votes` in all objects in the `forks` array, ignoring the objects without `votes`
    val sum = element
        // Accesses the "forks" key from the root JsonObject
        .jsonObject["forks"]!!

        // Checks that "forks" is a JsonArray and sums the "votes" from each JsonObject
        .jsonArray.sumOf { it.jsonObject["votes"]?.jsonPrimitive?.int ?: 0 }
    println(sum)
    // 9042
}
