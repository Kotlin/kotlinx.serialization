/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.model

import kotlinx.serialization.json.*
import java.io.*

fun main() {
    val text = Twitter::class.java.getResource("/twitter.json")!!.readBytes().decodeToString()
    val elem = Json.parseToJsonElement(text)
    val output = elem.toString5()
    File("twitter.json5").writeText(output)
}

// Produces unquoted keys and values always within a ".
private fun JsonElement.toString5(): String {
    return when(this) {
        is JsonNull -> "null"
        is JsonPrimitive -> toString()
        is JsonArray -> joinToString(prefix = "[", postfix = "]", separator = ",") { it.toString5() }
        is JsonObject -> entries.joinToString(
            separator = ",",
            prefix = "{",
            postfix = "}",
            transform = { (k, v) ->
                buildString {
                    append(k)
                    append(':')
                    append(v.toString5())
                }
            })
    }
}
