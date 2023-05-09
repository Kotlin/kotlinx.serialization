package kotlinx.serialization.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.test.*
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonExponentTest : JsonTestBase() {
    @Serializable
    data class SomeData(val count: Long)
    @Serializable
    data class SomeDataDouble(val count: Double)

    @Test
    fun testExponentDecodingPositive() = parametrizedTest {
        val decoded =  Json.decodeFromString<SomeData>("""{ "count": 23e11 }""", it)
        assertEquals(2300000000000, decoded.count)
    }

    @Test
    fun testExponentDecodingNegative() = parametrizedTest {
        val decoded = Json.decodeFromString<SomeData>("""{ "count": -10E1 }""", it)
        assertEquals(-100, decoded.count)
    }

    @Test
    fun testExponentDecodingPositiveDouble() = parametrizedTest {
        val decoded = Json.decodeFromString<SomeDataDouble>("""{ "count": 1.5E1 }""", it)
        assertEquals(15.0, decoded.count)
    }

    @Test
    fun testExponentDecodingNegativeDouble() = parametrizedTest {
        val decoded = Json.decodeFromString<SomeDataDouble>("""{ "count": -1e-1 }""", it)
        assertEquals(-0.1, decoded.count)
    }

    @Test
    fun testExponentDecodingErrorTruncatedDecimal() = parametrizedTest {
        assertFailsWithSerial("JsonDecodingException")
        { Json.decodeFromString<SomeData>("""{ "count": -1E-1 }""", it) }
    }

    @Test
    fun testExponentDecodingErrorExponent() = parametrizedTest {
        assertFailsWithSerial("JsonDecodingException")
        { Json.decodeFromString<SomeData>("""{ "count": 1e-1e-1 }""", it) }
    }

    @Test
    fun testExponentDecodingErrorExponentDouble() = parametrizedTest {
        assertFailsWithSerial("JsonDecodingException")
        { Json.decodeFromString<SomeDataDouble>("""{ "count": 1e-1e-1 }""", it) }
    }

    @Test
    fun testExponentOverflowDouble() = parametrizedTest {
        assertFailsWithSerial("JsonDecodingException")
        { Json.decodeFromString<SomeDataDouble>("""{ "count": 10000e10000 }""", it) }
    }

    @Test
    fun testExponentUnderflowDouble() = parametrizedTest {
        assertFailsWithSerial("JsonDecodingException")
        { Json.decodeFromString<SomeDataDouble>("""{ "count": -100e2222 }""", it) }
    }

    @Test
    fun testExponentOverflow() = parametrizedTest {
        assertFailsWithSerial("JsonDecodingException")
        { Json.decodeFromString<SomeData>("""{ "count": 10000e10000 }""", it) }
    }

    @Test
    fun testExponentUnderflow() = parametrizedTest {
        assertFailsWithSerial("JsonDecodingException")
        { Json.decodeFromString<SomeData>("""{ "count": -10000e10000 }""", it) }
    }
}