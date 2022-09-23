// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin12

import kotlinx.serialization.*
import kotlinx.serialization.json.*

import kotlin.time.*

fun main() {
    val duration = 1000.toDuration(DurationUnit.SECONDS)
    println(Json.encodeToString(duration))
}
