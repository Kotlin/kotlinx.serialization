// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson08

enum class Color { BLACK, WHITE }

@Serializable
data class Brush(val foreground: Color = Color.BLACK, val background: Color?)

val json = Json { 
  coerceInputValues = true
  explicitNulls = false
}

fun main() {
    val brush = json.decodeFromString<Brush>("""{"foreground":"pink", "background":"purple"}""")
  println(brush)
}
