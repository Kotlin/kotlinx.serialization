package kotlinx.serialization.protobuf

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProtobufAbsenceTest {
    @Serializable
    data class SimpleValue(val i: Int)
    @Serializable
    data class DefaultValue(val i: Int = 42)
    @Serializable
    data class NullableValue(val i: Int?)
    @Serializable
    data class DefaultAndNullValue(val i: Int? = 42)

    @Serializable
    data class SimpleList(val l: List<Int>)
    @Serializable
    data class DefaultList(val l: List<Int> = listOf(42))
    @Serializable
    data class NullableList(val l: List<Int>?)
    @Serializable
    data class DefaultNullableList(val l: List<Int>? = listOf(42))

    @Serializable
    data class SimpleMap(val m: Map<Int, Int>)
    @Serializable
    data class DefaultMap(val m: Map<Int, Int> = mapOf(42 to 43))
    @Serializable
    data class NullableMap(val m: Map<Int, Int>?)
    @Serializable
    data class DefaultNullableMap(val m: Map<Int, Int>? = mapOf(42 to 43))

    @Test
    fun testSimpleValue() {
        assertFailsWith(SerializationException::class) { ProtoBuf.decodeFromByteArray<SimpleValue>(ByteArray(0)) }
    }

    @Test
    fun testDefaultValue() {
        val bytes = ProtoBuf.encodeToByteArray(DefaultValue(42))
        assertEquals(0, bytes.size)

        val decoded = ProtoBuf.decodeFromByteArray<DefaultValue>(bytes)
        assertEquals(42, decoded.i)
    }

    @Test
    fun testNullableValue() {
        val bytes = ProtoBuf.encodeToByteArray(NullableValue(null))
        assertEquals(0, bytes.size)

        val decoded = ProtoBuf.decodeFromByteArray<NullableValue>(bytes)
        assertEquals(null, decoded.i)
    }

    @Test
    fun testDefaultAndNullValue() {
        assertFailsWith(SerializationException::class) { ProtoBuf.encodeToByteArray(DefaultAndNullValue(null)) }

        val bytes = ProtoBuf.encodeToByteArray(DefaultAndNullValue(42))
        assertEquals(0, bytes.size)

        val decoded = ProtoBuf.decodeFromByteArray<DefaultAndNullValue>(bytes)
        assertEquals(42, decoded.i)
    }


    @Test
    fun testSimpleList() {
        val bytes = ProtoBuf.encodeToByteArray(SimpleList(emptyList()))
        assertEquals(0, bytes.size)

        val decoded = ProtoBuf.decodeFromByteArray<SimpleList>(bytes)
        assertEquals(emptyList(), decoded.l)
    }

    @Test
    fun testDefaultList() {
        val bytes = ProtoBuf.encodeToByteArray(DefaultList(listOf(42)))
        assertEquals(0, bytes.size)

        val decoded = ProtoBuf.decodeFromByteArray<DefaultList>(bytes)
        assertEquals(listOf(42), decoded.l)
    }

    @Test
    fun testNullableList() {
        assertFailsWith(SerializationException::class) { ProtoBuf.encodeToByteArray(NullableList(null)) }

        val bytes = ProtoBuf.encodeToByteArray(NullableList(emptyList()))
        assertEquals(0, bytes.size)

        val decoded = ProtoBuf.decodeFromByteArray<NullableList>(bytes)
        assertEquals(emptyList(), decoded.l)
    }


    @Test
    fun testDefaultNullableList() {
        assertFailsWith(SerializationException::class) { ProtoBuf.encodeToByteArray(DefaultNullableList(null)) }

        val bytes = ProtoBuf.encodeToByteArray(DefaultNullableList(listOf(42)))
        assertEquals(0, bytes.size)

        val decoded = ProtoBuf.decodeFromByteArray<DefaultNullableList>(bytes)
        assertEquals(listOf(42), decoded.l)
    }

    @Test
    fun testSimpleMap() {
        val bytes = ProtoBuf.encodeToByteArray(SimpleMap(emptyMap()))
        assertEquals(0, bytes.size)
        val decoded = ProtoBuf.decodeFromByteArray<SimpleMap>(bytes)
        assertEquals(emptyMap(), decoded.m)
    }

    @Test
    fun testDefaultMap() {
        val bytes = ProtoBuf.encodeToByteArray(DefaultMap(mapOf(42 to 43)))
        assertEquals(0, bytes.size)
        val decoded = ProtoBuf.decodeFromByteArray<DefaultMap>(bytes)
        assertEquals(mapOf(42 to 43), decoded.m)
    }

    @Test
    fun testNullableMap() {
        assertFailsWith(SerializationException::class) { ProtoBuf.encodeToByteArray(NullableMap(null)) }

        val bytes = ProtoBuf.encodeToByteArray(NullableMap(emptyMap()))
        assertEquals(0, bytes.size)

        val decoded = ProtoBuf.decodeFromByteArray<NullableMap>(bytes)
        assertEquals(emptyMap(), decoded.m)
    }

    @Test
    fun testDefaultNullableMap() {
        assertFailsWith(SerializationException::class) { ProtoBuf.encodeToByteArray(DefaultNullableMap(null)) }

        val bytes = ProtoBuf.encodeToByteArray(DefaultNullableMap(mapOf(42 to 43)))
        assertEquals(0, bytes.size)

        val decoded = ProtoBuf.decodeFromByteArray<DefaultNullableMap>(bytes)
        assertEquals(mapOf(42 to 43), decoded.m)
    }
}
