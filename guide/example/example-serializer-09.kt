// This file was automatically generated from serializers.md by Knit tool. Do not edit.
package example.exampleSerializer09

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

object ColorAsStringSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        val string = value.rgb.toString(16).padStart(6, '0')
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): Color {
        val string = decoder.decodeString()
        return Color(string.toInt(16))
    }
}

@Serializable(with = ColorAsStringSerializer::class)
data class Color(val rgb: Int)

@Serializable 
data class Settings(val background: Color, val foreground: Color)

fun main() {
    val data = Settings(Color(0xffffff), Color(0))
    val string = Json.encodeToString(data)
    println(string)
    require(Json.decodeFromString<Settings>(string) == data)
}  
