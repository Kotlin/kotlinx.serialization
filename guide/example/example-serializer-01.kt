// This file was automatically generated from create-custom-serializers.md by Knit tool. Do not edit.
package example.exampleSerializer01

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*

@Serializable
data class Color(val rgb: Int)

fun main() {
    val colorSerializer: KSerializer<Color> = Color.serializer()
    println(colorSerializer.descriptor)
    // Color(rgb: kotlin.Int)
}
