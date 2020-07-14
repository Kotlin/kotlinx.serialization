// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin02

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Data(val value: Double)                     

fun main() {
    val data = Data(Double.NaN)
    println(Json.encodeToString(data))
}
