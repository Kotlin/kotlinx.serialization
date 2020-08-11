// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin01

import kotlinx.serialization.*
import kotlinx.serialization.json.*

import kotlin.math.*

@Serializable
class Data(
    val answer: Int,
    val pi: Double
)                     

fun main() {
    val data = Data(42, PI)
    println(Json.encodeToString(data))
}
