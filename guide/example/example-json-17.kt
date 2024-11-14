// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson17

import kotlinx.serialization.*
import kotlinx.serialization.json.*

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
