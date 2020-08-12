// This file was automatically generated from serializers.md by Knit tool. Do not edit.
package example.exampleSerializer10

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

@Serializable
@SerialName("Color")
private class ColorSurrogate(val r: Int, val g: Int, val b: Int) {
    init {     
        require(r in 0..255 && g in 0..255 && b in 0..255)
    }
}

object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = ColorSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Color) {
        val surrogate = ColorSurrogate((value.rgb shr 16) and 0xff, (value.rgb shr 8) and 0xff, value.rgb and 0xff)
        encoder.encodeSerializableValue(ColorSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Color {
        val surrogate = decoder.decodeSerializableValue(ColorSurrogate.serializer())
        return Color((surrogate.r shl 16) or (surrogate.g shl 8) or surrogate.b)
    }
}

@Serializable(with = ColorSerializer::class)
class Color(val rgb: Int)
fun main() {
    val green = Color(0x00ff00)
    println(Json.encodeToString(green))
}
