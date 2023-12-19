// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson13

import kotlinx.serialization.*
import kotlinx.serialization.json.*

val format = Json { decodeEnumsCaseInsensitive = true }

enum class Cases { VALUE_A, @JsonNames("Alternative") VALUE_B }

@Serializable
data class CasesList(val cases: List<Cases>)

fun main() {
  println(format.decodeFromString<CasesList>("""{"cases":["value_A", "alternative"]}""")) 
}
