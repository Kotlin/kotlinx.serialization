// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin02

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Data(val signature: Long)

fun main() {
    val data = Data(0x1CAFE2FEED0BABE0)
    println(Json.encodeToString(data))
}
