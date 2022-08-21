package kotlinx.serialization.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.ChunkedDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFails

@Serializable(with = LargeStringSerializer::class)
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

object LargeStringSerializer : KSerializer<LargeBinaryData> {
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


class JsonChunkedDecoderTest:JsonTestBase() {

    @Test
    fun decodeBase64String() {
        val sourceObject = ClassWithBinaryDataField(LargeBinaryData(Random.nextBytes(16 * 1024))) // After encoding will be more than BATCH_SIZE (16k)
        val serializedObject = Json.encodeToString(sourceObject)

        JsonTestingMode.values().forEach { mode ->
            if (mode == JsonTestingMode.TREE) {
                assertFails("Only chunked decoder supported") {
                    Json.decodeFromString<ClassWithBinaryDataField>(serializedObject, mode)
                }
            } else {
                val deserializedObject = Json.decodeFromString<ClassWithBinaryDataField>(serializedObject, mode)
                assertEquals(sourceObject.binaryField, deserializedObject.binaryField)
            }
        }
    }
}
