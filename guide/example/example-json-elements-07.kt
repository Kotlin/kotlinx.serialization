// This file was automatically generated from serialization-json-elements.md by Knit tool. Do not edit.
package example.exampleJsonElements07

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.math.BigDecimal

fun main() {
    val piObjectJson = """
          {
              "pi_literal": 3.141592653589793238462643383279
          }
      """.trimIndent()

    // Decodes the JSON string into a JsonObject
    val piObject: JsonObject = Json.decodeFromString(piObjectJson)

    // Extracts the string content from the JsonPrimitive
    val piJsonLiteral = piObject["pi_literal"]!!.jsonPrimitive.content

    // Converts the string to a BigDecimal
    val pi = BigDecimal(piJsonLiteral)
    // Prints the decoded value of pi, preserving all 30 decimal places
    println(pi)
    // 3.141592653589793238462643383279
}
