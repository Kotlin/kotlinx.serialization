// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson21

import kotlinx.serialization.*
import kotlinx.serialization.json.*

import java.math.BigDecimal

fun main() {
    val piObjectJson = """
          {
              "pi_literal": 3.141592653589793238462643383279
          }
      """.trimIndent()
    
    val piObject: JsonObject = Json.decodeFromString(piObjectJson)
    
    val piJsonLiteral = piObject["pi_literal"]!!.jsonPrimitive.content
    
    val pi = BigDecimal(piJsonLiteral)
    
    println(pi)
}
