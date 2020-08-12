// This file was automatically generated from serializers.md by Knit tool. Do not edit.
package example.exampleSerializer01

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Color(val rgb: Int)

fun main() {
    val green = Color(0x00ff00)
    println(Json.encodeToString(green))
}  
