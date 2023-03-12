package kotlinx.serialization.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.test.assertFailsWithSerial
import kotlinx.serialization.test.assertFailsWithSerialMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonExponentTest : JsonTestBase() {
    @Serializable
    data class SomeData(val count: Long)
    @Serializable
    data class SomeDataDouble(val count: Double)

    @Test
    fun testExponentDecodingPositiveInteger() {
        val decoded = Json.decodeFromString<SomeData>("""{ "count": 23e11 }""")
        assertEquals(2300000000000, decoded.count)
    }

    @Test
    fun testExponentDecodingNegativeInteger() {
        val decoded = Json.decodeFromString<SomeData>("""{ "count": -10E1 }""")
        assertEquals(-100, decoded.count)
    }

    @Test
    fun testExponentDecodingTruncatedDecimal() {
        val decoded = Json.decodeFromString<SomeData>("""{ "count": -1E-1 }""")
        assertEquals(0, decoded.count)
    }

    @Test
    fun testExponentDecodingPositiveDouble() {
        val decoded = Json.decodeFromString<SomeDataDouble>("""{ "count": 1.5E1 }""")
        assertEquals(15.0, decoded.count)
    }

    @Test
    fun testExponentDecodingNegativeDouble() {
        val decoded = Json.decodeFromString<SomeDataDouble>("""{ "count": -1e-1 }""")
        assertEquals(-0.1, decoded.count)
    }

    @Test
    fun testExponentDecodingErrorExponent() {
        assertFailsWithSerialMessage("JsonDecodingException", "Unexpected symbol 'e' in numeric literal")
        { Json.decodeFromString<SomeData>("""{ "count": 1e-1e-1 }""") }
    }

    @Test
    fun testExponentDecodingErrorExponentDouble() {
        assertFailsWithSerialMessage("JsonDecodingException","Failed to parse type 'double' for input '1e-1e-1'")
        { Json.decodeFromString<SomeDataDouble>("""{ "count": 1e-1e-1 }""") }
    }

    @Test
    fun testExponentOverflowDouble() {
        assertFailsWithSerialMessage("JsonDecodingException","Unexpected special floating-point value Infinity")
        { Json.decodeFromString<SomeDataDouble>("""{ "count": 10000e10000 }""") }
    }

    @Test
    fun testExponentUnderflowDouble() {
        assertFailsWithSerialMessage("JsonDecodingException", "Unexpected special floating-point value -Infinity")
        { Json.decodeFromString<SomeDataDouble>("""{ "count": -100e2222 }""") }
    }

    @Test
    fun testExponentOverflowLong() {
        assertFailsWithSerialMessage("JsonDecodingException","Numeric value overflow")
        { Json.decodeFromString<SomeData>("""{ "count": 10000e10000 }""") }
    }

    @Test
    fun testExponentUnderflowLong() {
        assertFailsWithSerialMessage("JsonDecodingException","Numeric value overflow")
        { Json.decodeFromString<SomeData>("""{ "count": -10000e10000 }""") }
    }

}