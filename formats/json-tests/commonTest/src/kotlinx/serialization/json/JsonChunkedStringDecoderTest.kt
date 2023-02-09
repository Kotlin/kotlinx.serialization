package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.test.assertFailsWithMessage
import kotlin.test.*


@Serializable(with = LargeStringSerializer::class)
data class LargeStringData(val largeString: String)

@Serializable
data class ClassWithLargeStringDataField(val largeStringField: LargeStringData)


object LargeStringSerializer : KSerializer<LargeStringData> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LargeStringContent", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LargeStringData {
        require(decoder is ChunkedDecoder) { "Only chunked decoder supported" }

        val outStringBuilder = StringBuilder()

        decoder.decodeStringChunked { chunk ->
            outStringBuilder.append(chunk)
        }
        return LargeStringData(outStringBuilder.toString())
    }

    override fun serialize(encoder: Encoder, value: LargeStringData) {
        encoder.encodeString(value.largeString)
    }
}

open class JsonChunkedStringDecoderTest : JsonTestBase() {

    @Test
    fun decodePlainLenientString() {
        val longString = "abcd".repeat(8192) // Make string more than 16k
        val sourceObject = ClassWithLargeStringDataField(LargeStringData(longString))
        val serializedObject = "{\"largeStringField\": $longString }"
        val jsonWithLenientMode = Json { isLenient = true }
        testDecodeInAllModes(jsonWithLenientMode, serializedObject, sourceObject)
    }

    @Test
    fun decodePlainString() {
        val longStringWithEscape = "${"abcd".repeat(4096)}\"${"abcd".repeat(4096)}" // Make string more than 16k
        val sourceObject = ClassWithLargeStringDataField(LargeStringData(longStringWithEscape))
        val serializedObject = Json.encodeToString(sourceObject)
        testDecodeInAllModes(Json, serializedObject, sourceObject)
    }

    private fun testDecodeInAllModes(
        seralizer: Json, serializedObject: String, sourceObject: ClassWithLargeStringDataField
    ) {
        /* Filter out Java Streams mode in common tests. Java streams tested separately in java tests */
        JsonTestingMode.values().filterNot { it == JsonTestingMode.JAVA_STREAMS }.forEach { mode ->
            if (mode == JsonTestingMode.TREE) {
                assertFailsWithMessage<IllegalArgumentException>(
                    "Only chunked decoder supported", "Shouldn't decode JSON in TREE mode"
                ) {
                    seralizer.decodeFromString<ClassWithLargeStringDataField>(serializedObject, mode)
                }
            } else {
                val deserializedObject =
                    seralizer.decodeFromString<ClassWithLargeStringDataField>(serializedObject, mode)
                assertEquals(sourceObject.largeStringField, deserializedObject.largeStringField)
            }
        }
    }
}
