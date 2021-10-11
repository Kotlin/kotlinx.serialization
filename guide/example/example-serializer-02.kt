// This file was automatically generated from serializers.md by Knit tool. Do not edit.
package example.exampleSerializer02

import kotlinx.serialization.*

@Serializable
@SerialName("Color")
class Color(val rgb: Int)

fun main() {
    val colorSerializer: KSerializer<Color> = Color.serializer()
    println(colorSerializer.descriptor)
} 
