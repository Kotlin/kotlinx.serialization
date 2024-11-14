// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson15

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class) // decodeEnumsCaseInsensitive is an experimental setting for now
val format = Json { decodeEnumsCaseInsensitive = true }

@OptIn(ExperimentalSerializationApi::class) // JsonNames is an experimental annotation for now
enum class Cases { VALUE_A, @JsonNames("Alternative") VALUE_B }

@Serializable
data class CasesList(val cases: List<Cases>)

fun main() {
  println(format.decodeFromString<CasesList>("""{"cases":["value_A", "alternative"]}""")) 
}
