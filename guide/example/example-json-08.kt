// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson08

import kotlinx.serialization.*
import kotlinx.serialization.json.*

enum class Color { BLACK, WHITE }

@Serializable
data class Brush(val foreground: Color = Color.BLACK, val background: Color?)

val json = Json { 
  coerceInputValues = true
  explicitNulls = false
}

fun main() {

    // Decodes `foreground` to its default value and `background` to `null`
    val brush = json.decodeFromString<Brush>("""{"foreground":"pink", "background":"purple"}""")
    println(brush)
    // Brush(foreground=BLACK, background=null)
}
