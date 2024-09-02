[//]: # (title: Create custom serializers)

A plugin-generated serializer is convenient, but it may not produce the JSON we want for classes such as `Color`.
In this section we will cover the alternatives.

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

### Base64

To encode and decode Base64 formats, we will need to manually write a serializer. Here, we will use a default
implementation of Kotlin's Base64 encoder. Note that some serializers use different RFCs for Base64 encoding by default.
For example, Jackson uses a variant of [Base64 Mime](https://datatracker.ietf.org/doc/html/rfc2045). The same result in
kotlinx.serialization can be achieved with Base64.Mime encoder.
[Kotlin's documentation for Base64](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io.encoding/-base64/) lists
other available encoders.

```kotlin
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.descriptors.*
import kotlin.io.encoding.*

@OptIn(ExperimentalEncodingApi::class)
object ByteArrayAsBase64Serializer : KSerializer<ByteArray> {
    private val base64 = Base64.Default

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(
            "ByteArrayAsBase64Serializer",
            PrimitiveKind.STRING
        )

    override fun serialize(encoder: Encoder, value: ByteArray) {
        val base64Encoded = base64.encode(value)
        encoder.encodeString(base64Encoded)
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        val base64Decoded = decoder.decodeString()
        return base64.decode(base64Decoded)
    }
}
```

For more details on how to create your own custom serializer, you can
see [custom serializers](serializers.md#custom-serializers).

Then we can use it like this:

```kotlin
@Serializable
data class Value(
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val base64Input: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Value
        return base64Input.contentEquals(other.base64Input)
    }

    override fun hashCode(): Int {
        return base64Input.contentHashCode()
    }
}

fun main() {
    val string = "foo string"
    val value = Value(string.toByteArray())
    val encoded = Json.encodeToString(value)
    println(encoded)
    val decoded = Json.decodeFromString<Value>(encoded)
    println(decoded.base64Input.decodeToString())
}
```

> You can get the full code [here](../../guide/example/example-json-16.kt)

```text
{"base64Input":"Zm9vIHN0cmluZw=="}
foo string
```

Notice the serializer we wrote is not dependent on `Json` format, therefore, it can be used in any format.

For projects that use this serializer in many places, to avoid specifying the serializer every time, it is possible
to [specify a serializer globally using typealias](serializers.md#specifying-serializer-globally-using-typealias).
For example:
````kotlin
typealias Base64ByteArray = @Serializable(ByteArrayAsBase64Serializer::class) ByteArray
````

<!--- TEST -->