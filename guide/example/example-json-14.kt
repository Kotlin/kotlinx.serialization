// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson14

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Configures a Json instance to decode enum values in a case-insensitive way
val format = Json { decodeEnumsCaseInsensitive = true }

enum class Cases { VALUE_A, @JsonNames("Alternative") VALUE_B }

@Serializable
data class CasesList(val cases: List<Cases>)

fun main() {
    // Decodes enum values regardless of their case, affecting both serial names and alternative names
    println(format.decodeFromString<CasesList>("""{"cases":["value_A", "alternative"]}""")) 
    // CasesList(cases=[VALUE_A, VALUE_B])
}
