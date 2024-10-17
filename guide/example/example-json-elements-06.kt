// This file was automatically generated from serialization-json-elements.md by Knit tool. Do not edit.
package example.exampleJsonElements06

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.math.BigDecimal

val format = Json { prettyPrint = true }

fun main() {
    val pi = BigDecimal("3.141592653589793238462643383279")

    // Encodes the raw JSON content using JsonUnquotedLiteral
    @OptIn(ExperimentalSerializationApi::class)
    val piJsonLiteral = JsonUnquotedLiteral(pi.toString())

    // Converts to Double and String
    val piJsonDouble = JsonPrimitive(pi.toDouble())
    val piJsonString = JsonPrimitive(pi.toString())

    val piObject = buildJsonObject {
        put("pi_literal", piJsonLiteral)
        put("pi_double", piJsonDouble)
        put("pi_string", piJsonString)
    }

    // `pi_literal` now accurately matches the value defined.
    println(format.encodeToString(piObject))
    // "pi_literal": 3.141592653589793238462643383279,
    // "pi_double": 3.141592653589793,
    // "pi_string": "3.141592653589793238462643383279"
}
