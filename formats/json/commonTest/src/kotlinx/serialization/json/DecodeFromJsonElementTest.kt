package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class DecodeFromJsonElementTest {
    @Serializable
    data class A(val a: Int)

    @Serializable
    data class B(val a: A?)

    @Test
    fun testDecodeTopLevelNullable() {
        val a = A(42)
        val jsonElement = Json.encodeToJsonElement(a)
        assertEquals(a, Json.decodeFromJsonElement<A?>(jsonElement))
    }

    @Test
    fun topLevelNull() {
        assertNull(Json.decodeFromJsonElement<A?>(JsonNull))
    }

    @Test
    fun testInnerNullable() {
        val b = B(A(42))
        val json = Json.encodeToJsonElement(b)
        assertEquals(b, Json.decodeFromJsonElement(json))
    }

    @Test
    fun testInnerNullableNull() {
        val b = B(null)
        val json = Json.encodeToJsonElement(b)
        assertEquals(b, Json.decodeFromJsonElement(json))
    }

    @Test
    fun testPrimitive() {
        assertEquals(42, Json.decodeFromJsonElement(JsonPrimitive(42)))
        assertEquals(42, Json.decodeFromJsonElement<Int?>(JsonPrimitive(42)))
        assertEquals(null, Json.decodeFromJsonElement<Int?>(JsonNull))
    }

    @Test
    fun testNullableList() {
        assertEquals(listOf(42), Json.decodeFromJsonElement<List<Int>?>(JsonArray(listOf(JsonPrimitive(42)))))
        assertEquals(listOf(42), Json.decodeFromJsonElement<List<Int?>?>(JsonArray(listOf(JsonPrimitive(42)))))
        assertEquals(listOf(42), Json.decodeFromJsonElement<List<Int?>>(JsonArray(listOf(JsonPrimitive(42)))))
        // Nulls
        assertEquals(null, Json.decodeFromJsonElement<List<Int>?>(JsonNull))
        assertEquals(null, Json.decodeFromJsonElement<List<Int?>?>(JsonNull))
    }
}
