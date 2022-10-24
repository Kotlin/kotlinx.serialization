package kotlinx.serialization.protobuf

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.internal.ProtobufDecodingException
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProtobufStreamTest {

    private val protoBuf = ProtoBuf

    @Serializable
    data class Foo(val value: String)

    @Test
    fun shouldSerializeSimpleString() {
        val foo = Foo("stream")
        val outputStream = ByteArrayOutputStream()
        protoBuf.encodeToStream(foo, outputStream)
        val inputSteam = ByteArrayInputStream(outputStream.toByteArray())
        val result = protoBuf.decodeFromStream<Foo>(inputSteam)
        assertEquals("stream", result.value)
    }

    @Test
    fun shouldSerializeDelimitedOneStringMessage() {
        val foo = Foo("stream")
        val outputStream = ByteArrayOutputStream()
        protoBuf.encodeDelimitedToStream(foo, outputStream)
        val inputSteam = ByteArrayInputStream(outputStream.toByteArray())
        val result = protoBuf.decodeDelimitedMessages<Foo>(inputSteam)
        assertEquals("stream", result.first().value)
    }

    @Test
    fun shouldSerializeMultipleStringMessages() {
        val outputStream = ByteArrayOutputStream()
        val mutList = mutableListOf<Foo>()
        for (i in 1..6) {
            val foo = Foo("stream + $i")
            mutList.add(foo)
            protoBuf.encodeDelimitedToStream(foo, outputStream)
        }
        val inputSteam = ByteArrayInputStream(outputStream.toByteArray())
        val result = protoBuf.decodeDelimitedMessages<Foo>(inputSteam)
        mutList.forEach { assertContains(result, it) }
    }

    @Serializable
    data class Var(val value: Int)

    @Test
    fun shouldSerializeSimpleInt() {
        val random = Var(10)
        val outputStream = ByteArrayOutputStream()
        protoBuf.encodeToStream(random, outputStream)
        val inputSteam = ByteArrayInputStream(outputStream.toByteArray())
        val result = protoBuf.decodeFromStream<Var>(inputSteam)
        assertEquals(10, result.value)
    }

    @Test
    fun shouldSerializeDelimitedOneIntMessage() {
        val random = Var(10)
        val outputStream = ByteArrayOutputStream()
        protoBuf.encodeDelimitedToStream(random, outputStream)
        val inputSteam = ByteArrayInputStream(outputStream.toByteArray())
        val result = protoBuf.decodeDelimitedMessages<Var>(inputSteam)
        assertEquals(10, result.first().value)
    }

    @Test
    fun shouldSerializeMultipleIntMessages() {
        val outputStream = ByteArrayOutputStream()
        val mutList = mutableListOf<Var>()
        for (i in 1..6) {
            val random = Var(i)
            mutList.add(random)
            protoBuf.encodeDelimitedToStream(random, outputStream)
        }
        val inputSteam = ByteArrayInputStream(outputStream.toByteArray())
        val result = protoBuf.decodeDelimitedMessages<Var>(inputSteam)
        mutList.forEach { assertContains(result, it) }
    }

    /*
     * Note: should this fail? This mostly works by accident since we do not check for more
     * bytes in stream.
     *
     * Should this behavior fixed or just documented?
     */
    @Test
    fun shouldFailToSerializeTwoSameObjectsInStreamNoDelimited() {
        val foo = Foo("stream")
        val outputStream = ByteArrayOutputStream()
        protoBuf.encodeToStream(foo, outputStream)
        protoBuf.encodeToStream(foo, outputStream)
        val inputSteam = ByteArrayInputStream(outputStream.toByteArray())
        /*
         * Even if this does not fail, it renders the next object in stream malformed state
         */
        val result = protoBuf.decodeFromStream<Foo>(inputSteam)
        assertEquals("stream", result.value)
        // consecutive calls to retrieve the next object fails (as expected)
        assertFailsWith<SerializationException> { protoBuf.decodeFromStream<Foo>(inputSteam) }
    }

    @Test
    fun shouldFailForTwoDifferentObjectsEncodedInStream() {
        val foo = Foo("stream")
        val random = Var(10)
        val outputStream = ByteArrayOutputStream()
        protoBuf.encodeToStream(foo, outputStream)
        protoBuf.encodeToStream(random, outputStream)
        assertFailsWith<ProtobufDecodingException> {
            val inputSteam = ByteArrayInputStream(outputStream.toByteArray())
            protoBuf.decodeFromStream<Var>(inputSteam)
        }
    }
}






















