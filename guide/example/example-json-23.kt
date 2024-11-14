// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson23

import kotlinx.serialization.*
import kotlinx.serialization.json.*

import java.math.BigDecimal

val format = Json { prettyPrint = true }

fun main() {
    val pi = BigDecimal("3.141592653589793238462643383279")

    // use JsonUnquotedLiteral to encode raw JSON content
    @OptIn(ExperimentalSerializationApi::class)
    val piJsonLiteral = JsonUnquotedLiteral(pi.toString())

    val piJsonDouble = JsonPrimitive(pi.toDouble())
    val piJsonString = JsonPrimitive(pi.toString())
  
    val piObject = buildJsonObject {
        put("pi_literal", piJsonLiteral)
        put("pi_double", piJsonDouble)
        put("pi_string", piJsonString)
    }

    println(format.encodeToString(piObject))
}
