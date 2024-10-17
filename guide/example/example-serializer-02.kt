// This file was automatically generated from create-custom-serializers.md by Knit tool. Do not edit.
package example.exampleSerializer02

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*

// Binds the Color class with the custom ColorAsStringSerializer using the with property
@Serializable(with = ColorAsStringSerializer::class)
data class Color(val rgb: Int)

// Creates the custom serializer for the Color class
object ColorAsStringSerializer : KSerializer<Color> {
    // Defines the schema for the serialized data as a single string
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    // Defines how the Color object is converted to a string during serialization
    override fun serialize(encoder: Encoder, value: Color) {
        // Converts the RGB value to a hex string
        val hexValue = value.rgb.toString(16).padStart(6, '0')
        // Encodes the hex string using the encodeString() function
        encoder.encodeString(hexValue)
    }

    // Defines how the string is converted back to a Color object during deserialization
    override fun deserialize(decoder: Decoder): Color {
        // Decodes the string using the decodeString() function
        val hexValue = decoder.decodeString()
        // Converts the hex value back into a Color object
        return Color(hexValue.toInt(16))
    }
}

fun main() {
    val color = Color(0x00FF00)
    // Serializes a color to JSON
    val jsonString = Json.encodeToString(color)
    println(jsonString)
    // "00ff00"

    // Deserializes the color back
    val deserializedColor = Json.decodeFromString<Color>(jsonString)
    println(deserializedColor.rgb)
    // 65280
}
