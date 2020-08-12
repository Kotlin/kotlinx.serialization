// This file was automatically generated from serializers.md by Knit tool. Do not edit.
package example.exampleSerializer17

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

import java.util.Date
import java.text.SimpleDateFormat

@Serializable          
class ProgrammingLanguage(
    val name: String,
    @Contextual 
    val stableReleaseDate: Date
)

fun main() {
    val data = ProgrammingLanguage("Kotlin", SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
    println(Json.encodeToString(data))
}
