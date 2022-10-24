package kotlinx.serialization.protobuf

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

class PolymorphicProtobufStreamTest {
    private val protoBuf = ProtoBuf {
        serializersModule = SerializersModule {
            polymorphic(Any::class) {
                subclass(String::class, String.serializer())
                subclass(Int::class, Int.serializer())
            }
        }
    }

    @Serializable
    data class PolyString(@Polymorphic val value: Any)

    @Serializable
    data class PolyInt(@Polymorphic val value: Any)

    @Test
    fun shouldSerializePolymorphicString() {
        val polyString = PolyString("10")
        val outputStream = ByteArrayOutputStream()
        protoBuf.encodeToStream(polyString, outputStream)
        val inputSteam = ByteArrayInputStream(outputStream.toByteArray())
        val result = protoBuf.decodeFromStream<PolyString>(inputSteam)
        assertEquals("10", result.value)
    }

    @Test
    fun shouldSerializeDelimitedOnePolymorphicStringMessage() {
        val polyString = PolyString("10")
        val outputStream = ByteArrayOutputStream()
        protoBuf.encodeDelimitedToStream(polyString, outputStream)
        val inputSteam = ByteArrayInputStream(outputStream.toByteArray())
        val result = protoBuf.decodeDelimitedMessages<PolyString>(inputSteam)
        assertEquals("10", result.first().value)
    }

    @Test
    fun shouldSerializePolymorphicInt() {
        val polyInt = PolyInt(10)
        val outputStream = ByteArrayOutputStream()
        protoBuf.encodeToStream(polyInt, outputStream)
        val inputSteam = ByteArrayInputStream(outputStream.toByteArray())
        val result = protoBuf.decodeFromStream<PolyInt>(inputSteam)
        assertEquals(10, result.value)
    }

    @Test
    fun shouldSerializeDelimitedOnePolymorphicIntMessage() {
        val polyInt = PolyInt(10)
        val outputStream = ByteArrayOutputStream()
        protoBuf.encodeDelimitedToStream(polyInt, outputStream)
        val inputSteam = ByteArrayInputStream(outputStream.toByteArray())
        val result = protoBuf.decodeDelimitedMessages<PolyInt>(inputSteam)
        assertEquals(10, result.first().value)
    }
}