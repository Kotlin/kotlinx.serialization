// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin04

import kotlinx.serialization.*
import kotlinx.serialization.json.*

import kotlinx.serialization.builtins.*

@Serializable
class Data(
    @Serializable(with=LongAsStringSerializer::class)
    val signature: Long
)

fun main() {
    val data = Data(0x1CAFE2FEED0BABE0)
    println(Json.encodeToString(data))
}
