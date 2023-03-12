package kotlinx.serialization.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.*

@Serializable
data class SomeData(val count: Int)
@Serializable
data class SomeDataDouble(val count: Double)

class JsonElementDecodingTest : JsonTestBase() {

    @Serializable
    data class A(val a: Int = 42)

    @Test
    fun testTopLevelClass() = assertSerializedForm(A(), """{}""".trimMargin())

    @Test
    fun testTopLevelNullableClass() {
        assertSerializedForm<A?>(A(), """{}""")
        assertSerializedForm<A?>(null, "null")
    }

    @Test
    fun testTopLevelPrimitive() = assertSerializedForm(42, """42""")

    @Test
    fun testTopLevelNullablePrimitive() {
        assertSerializedForm<Int?>(42, """42""")
        assertSerializedForm<Int?>(null, """null""")
    }

    @Test
    fun testTopLevelList() = assertSerializedForm(listOf(42), """[42]""")

    @Test
    fun testTopLevelNullableList() {
        assertSerializedForm<List<Int>?>(listOf(42), """[42]""")
        assertSerializedForm<List<Int>?>(null, """null""")
    }

    private inline fun <reified T> assertSerializedForm(value: T, expectedString: String) {
        val element = Json.encodeToJsonElement(value)
        assertEquals(expectedString, element.toString())
        assertEquals(value, Json.decodeFromJsonElement(element))
    }

    @Test
    fun testDeepRecursion() {
        // Reported as https://github.com/Kotlin/kotlinx.serialization/issues/1594
        var json = """{ "a": %}"""
        for (i in 0..12) {
            json = json.replace("%", json)
        }
        json = json.replace("%", "0")
        Json.parseToJsonElement(json)
    }

    @Test
    fun testExponentDecoding() {

        val decoded = Json.decodeFromString<SomeData>("""{ "count": 2e3 }""")
        val negativeDecoded = Json.decodeFromString<SomeData>("""{ "count": -10E1 }""")
        val decimalTrunked = Json.decodeFromString<SomeData>("""{ "count": -1E-1 }""") //This  is 0.1, gets truncated to 0
        val doubleDecoded = Json.decodeFromString<SomeDataDouble>("""{ "count": 1.5E1 }""")
        val negativeDoubleDecoded = Json.decodeFromString<SomeDataDouble>("""{ "count": -1e-1 }""")
        val errorExponent = { Json.decodeFromString<SomeData>("""{ "count": 1e-1e-1 }""") }
        val errorExponentDouble = { Json.decodeFromString<SomeDataDouble>("""{ "count": 1e-1e-1 }""") }

        assertEquals(2000, decoded.count)
        assertEquals(-100, negativeDecoded.count)
        assertEquals(0, decimalTrunked.count)
        assertEquals(15.0, doubleDecoded.count)
        assertEquals(-0.1, negativeDoubleDecoded.count)
        assertFails { errorExponent() }
        assertFails { errorExponentDouble() }
    }
}
