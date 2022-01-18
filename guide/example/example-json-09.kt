// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson09

import kotlinx.serialization.*
import kotlinx.serialization.json.*

val format = Json { allowSpecialFloatingPointValues = true }

@Serializable
class Data(
    val value: Double
)

fun main() {
    val data = Data(Double.NaN)
    println(format.encodeToString(data))
}
