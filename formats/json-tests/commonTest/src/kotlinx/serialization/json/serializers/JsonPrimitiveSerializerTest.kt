/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonPrimitiveSerializerTest : JsonTestBase() {

    @Test
    fun testJsonPrimitiveDouble() = parametrizedTest { jsonTestingMode ->
        if (isJs()) return@parametrizedTest // JS toString numbers


        val wrapper = JsonPrimitiveWrapper(JsonPrimitive(1.0))
        val string = default.encodeToString(JsonPrimitiveWrapper.serializer(), wrapper, jsonTestingMode)
        assertEquals("{\"primitive\":1.0}", string)
        assertEquals(JsonPrimitiveWrapper(JsonPrimitive(1.0)), default.decodeFromString(JsonPrimitiveWrapper.serializer(), string, jsonTestingMode))
    }

    @Test
    fun testJsonPrimitiveInt() = parametrizedTest { jsonTestingMode ->
        val wrapper = JsonPrimitiveWrapper(JsonPrimitive(1))
        val string = default.encodeToString(JsonPrimitiveWrapper.serializer(), wrapper, jsonTestingMode)
        assertEquals("{\"primitive\":1}", string)
        assertEquals(JsonPrimitiveWrapper(JsonPrimitive(1)), default.decodeFromString(JsonPrimitiveWrapper.serializer(), string, jsonTestingMode))
    }


    @Test
    fun testJsonPrimitiveString() = parametrizedTest { jsonTestingMode ->
        val wrapper = JsonPrimitiveWrapper(JsonPrimitive("foo"))
        val string = default.encodeToString(JsonPrimitiveWrapper.serializer(), wrapper, jsonTestingMode)
        assertEquals("{\"primitive\":\"foo\"}", string)
        assertEquals(JsonPrimitiveWrapper(JsonPrimitive("foo")), default.decodeFromString(JsonPrimitiveWrapper.serializer(), string, jsonTestingMode))
    }

    @Test
    fun testJsonPrimitiveStringNumber() = parametrizedTest { jsonTestingMode ->
        val wrapper = JsonPrimitiveWrapper(JsonPrimitive("239"))
        val string = default.encodeToString(JsonPrimitiveWrapper.serializer(), wrapper, jsonTestingMode)
        assertEquals("{\"primitive\":\"239\"}", string)
        assertEquals(JsonPrimitiveWrapper(JsonPrimitive("239")), default.decodeFromString(JsonPrimitiveWrapper.serializer(), string, jsonTestingMode))
    }

    @Test
    fun testJsonUnquotedLiteralNumbers() = parametrizedTest { jsonTestingMode ->
        listOf(
            "99999999999999999999999999999999999999999999999999999999999999999999999999",
            "99999999999999999999999999999999999999.999999999999999999999999999999999999",
            "-99999999999999999999999999999999999999999999999999999999999999999999999999",
            "-99999999999999999999999999999999999999.999999999999999999999999999999999999",
            "2.99792458e8",
            "-2.99792458e8",
        ).forEach { literalNum ->
            val literalNumJson = JsonUnquotedLiteral(literalNum)
            val wrapper = JsonPrimitiveWrapper(literalNumJson)
            val string = default.encodeToString(JsonPrimitiveWrapper.serializer(), wrapper, jsonTestingMode)
            assertEquals("{\"primitive\":$literalNum}", string, "mode:$jsonTestingMode")
            assertEquals(
                JsonPrimitiveWrapper(literalNumJson),
                default.decodeFromString(JsonPrimitiveWrapper.serializer(), string, jsonTestingMode),
                "mode:$jsonTestingMode",
            )
        }
    }

    @Test
    fun testTopLevelPrimitive() = parametrizedTest { jsonTestingMode ->
        val string = default.encodeToString(JsonPrimitive.serializer(), JsonPrimitive(42), jsonTestingMode)
        assertEquals("42", string)
        assertEquals(JsonPrimitive(42), default.decodeFromString(JsonPrimitive.serializer(), string))
    }

    @Test
    fun testTopLevelPrimitiveAsElement() = parametrizedTest { jsonTestingMode ->
        if (isJs()) return@parametrizedTest // JS toString numbers
        val string = default.encodeToString(JsonElement.serializer(), JsonPrimitive(1.3), jsonTestingMode)
        assertEquals("1.3", string)
        assertEquals(JsonPrimitive(1.3), default.decodeFromString(JsonElement.serializer(), string, jsonTestingMode))
    }

    @Test
    fun testJsonLiteralStringToString() {
        val literal = JsonPrimitive("some string literal")
        val string = default.encodeToString(JsonPrimitive.serializer(), literal)
        assertEquals(string, literal.toString())
    }

    @Test
    fun testJsonLiteralIntToString() {
        val literal = JsonPrimitive(0)
        val string = default.encodeToString(JsonPrimitive.serializer(), literal)
        assertEquals(string, literal.toString())
    }

    @Test
    fun testJsonLiterals() {
        testLiteral(0L, "0")
        testLiteral(0, "0")
        testLiteral(0.0, "0.0")
        testLiteral(0.0f, "0.0")
        testLiteral(Long.MAX_VALUE, "9223372036854775807")
        testLiteral(Long.MIN_VALUE, "-9223372036854775808")
        testLiteral(Float.MAX_VALUE, "3.4028235E38")
        testLiteral(Float.MIN_VALUE, "1.4E-45")
        testLiteral(Double.MAX_VALUE, "1.7976931348623157E308")
        testLiteral(Double.MIN_VALUE, "4.9E-324")
        testLiteral(Int.MAX_VALUE, "2147483647")
        testLiteral(Int.MIN_VALUE, "-2147483648")
    }

    private fun testLiteral(number: Number, jvmExpectedString: String) {
        val literal = JsonPrimitive(number)
        val string = default.encodeToString(JsonPrimitive.serializer(), literal)
        assertEquals(string, literal.toString())
        if (isJvm()) { // We can rely on stable double/float format only on JVM
            assertEquals(string, jvmExpectedString)
        }
    }

    /**
     * Helper function for [testJsonPrimitiveUnsignedNumbers]
     *
     * Asserts that an [unsigned number][actual] can be used to create a [JsonPrimitive][actualPrimitive],
     * which can be decoded correctly.
     *
     * @param expected the expected string value of [actual]
     * @param actual the unsigned number
     * @param T should be an unsigned number
     */
    private inline fun <reified T> assertUnsignedNumberEncoding(
        expected: String,
        actual: T,
        actualPrimitive: JsonPrimitive,
    ) {
        assertEquals(
            expected,
            actualPrimitive.toString(),
            "expect ${T::class.simpleName} $actual can be used to create a JsonPrimitive"
        )

        parametrizedTest { mode ->
            assertEquals(
                expected,
                default.encodeToString(JsonElement.serializer(), actualPrimitive, mode),
                "expect ${T::class.simpleName} primitive can be decoded",
            )
        }
    }

    @Test
    fun testJsonPrimitiveUnsignedNumbers() {

        val expectedActualUBytes: Map<String, UByte> = mapOf(
            "0" to 0u,
            "1" to 1u,
            "255" to UByte.MAX_VALUE,
        )

        expectedActualUBytes.forEach { (expected, actual) ->
            assertUnsignedNumberEncoding(expected, actual, JsonPrimitive(actual))
        }

        val expectedActualUShorts: Map<String, UShort> = mapOf(
            "0" to 0u,
            "1" to 1u,
            "255" to UByte.MAX_VALUE.toUShort(),
            "65535" to UShort.MAX_VALUE,
        )

        expectedActualUShorts.forEach { (expected, actual) ->
            assertUnsignedNumberEncoding(expected, actual, JsonPrimitive(actual))
        }

        val expectedActualUInts: Map<String, UInt> = mapOf(
            "0" to 0u,
            "1" to 1u,
            "255" to UByte.MAX_VALUE.toUInt(),
            "65535" to UShort.MAX_VALUE.toUInt(),
            "4294967295" to UInt.MAX_VALUE,
        )

        expectedActualUInts.forEach { (expected, actual) ->
            assertUnsignedNumberEncoding(expected, actual, JsonPrimitive(actual))
        }

        val expectedActualULongs: Map<String, ULong> = mapOf(
            "0" to 0u,
            "1" to 1u,
            "255" to UByte.MAX_VALUE.toULong(),
            "65535" to UShort.MAX_VALUE.toULong(),
            "4294967295" to UInt.MAX_VALUE.toULong(),
            "18446744073709551615" to ULong.MAX_VALUE,
        )

        expectedActualULongs.forEach { (expected, actual) ->
            assertUnsignedNumberEncoding(expected, actual, JsonPrimitive(actual))
        }
    }
}
