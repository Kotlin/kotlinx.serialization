// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson09

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Configures a Json instance to allow special floating-point values
val format = Json { allowSpecialFloatingPointValues = true }

@Serializable
class Data(
    val value: Double
)

fun main() {
    val data = Data(Double.NaN)
    // This example produces the following non-standard JSON output, yet it is a widely used encoding for
    // special values in JVM world:
    println(format.encodeToString(data))
    // {"value":NaN}
}
