package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.test.assertFailsWithMessage
import org.junit.Test
import java.io.*
import java.util.*
import kotlin.random.Random
import kotlin.test.*


@Serializable(with = LargeBase64StringSerializer::class)
data class LargeBinaryData(val binaryData: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LargeBinaryData

        if (!binaryData.contentEquals(other.binaryData)) return false

        return true
    }

    override fun hashCode(): Int {
        return binaryData.contentHashCode()
    }
}

@Serializable
data class ClassWithBinaryDataField(val binaryField: LargeBinaryData)

object LargeBase64StringSerializer : KSerializer<LargeBinaryData> {
    private val b64Decoder: Base64.Decoder = Base64.getDecoder()
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LargeStringContent", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LargeBinaryData {
        require(decoder is ChunkedDecoder) { "Only chunked decoder supported" }

        var reminder = ""
        val decodedBytes = ByteArrayOutputStream().use { bos ->
            decoder.decodeStringChunked {
                val actualChunk = reminder + it
                val reminderLength = actualChunk.length % 4
                val alignedLength = actualChunk.length - reminderLength
                val alignedChunk = actualChunk.take(alignedLength)
                reminder = actualChunk.takeLast(reminderLength)
                bos.write(b64Decoder.decode(alignedChunk))
            }
            bos.toByteArray()
        }

        return LargeBinaryData(decodedBytes)
    }

    override fun serialize(encoder: Encoder, value: LargeBinaryData) {
        encoder.encodeString(Base64.getEncoder().encodeToString(value.binaryData))
    }
}

class JsonChunkedBase64DecoderTest : JsonTestBase() {

    @Test
    fun decodeBase64String() {
        val sourceObject =
            ClassWithBinaryDataField(LargeBinaryData(Random.nextBytes(16 * 1024))) // After encoding to Base64 will be larger than 16k (JsonLexer#BATCH_SIZE)
        val serializedObject = Json.encodeToString(sourceObject)

        JsonTestingMode.values().filter { it != JsonTestingMode.OKIO_STREAMS }.forEach { mode ->
            if (mode == JsonTestingMode.TREE) {
                assertFailsWithMessage<IllegalArgumentException>(
                    "Only chunked decoder supported", "Shouldn't decode JSON in TREE mode"
                ) {
                    Json.decodeFromString<ClassWithBinaryDataField>(serializedObject, mode)
                }
            } else {
                val deserializedObject = Json.decodeFromString<ClassWithBinaryDataField>(serializedObject, mode)
                assertEquals(sourceObject.binaryField, deserializedObject.binaryField)
            }
        }
    }
}
