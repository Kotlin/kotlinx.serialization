[//]: # (title: Create custom serializers)

A plugin-generated serializer is convenient, but it may not produce the JSON we want for such a class as `Color`.
Let's study the alternatives.

## Primitive serializer

We want to serialize the `Color` class as a hex string with the green color represented as `"00ff00"`.
To achieve this, we write an object that implements the `KSerializer` interface for the `Color` class.

```kotlin
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
```