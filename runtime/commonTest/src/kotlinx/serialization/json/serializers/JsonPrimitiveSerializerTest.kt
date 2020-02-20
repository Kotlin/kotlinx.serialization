/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonPrimitiveSerializerTest : JsonTestBase() {

    @Test
    fun testJsonLiteralDouble() = parametrizedTest { useStreaming ->
        if (isJs()) return@parametrizedTest // JS toString numbers

        val wrapper = JsonLiteralWrapper(JsonLiteral(1.0))
        val string = strict.stringify(JsonLiteralWrapper.serializer(), wrapper, useStreaming)
        assertEquals("{\"literal\":1.0}", string)
        assertEquals(JsonLiteralWrapper(JsonLiteral(1.0)), strict.parse(JsonLiteralWrapper.serializer(), string, useStreaming))
    }

    @Test
    fun testJsonPrimitiveDouble() = parametrizedTest { useStreaming ->
        if (isJs()) return@parametrizedTest // JS toString numbers


        val wrapper = JsonPrimitiveWrapper(JsonLiteral(1.0))
        val string = strict.stringify(JsonPrimitiveWrapper.serializer(), wrapper, useStreaming)
        assertEquals("{\"primitive\":1.0}", string)
        assertEquals(JsonPrimitiveWrapper(JsonLiteral(1.0)), strict.parse(JsonPrimitiveWrapper.serializer(), string, useStreaming))
    }

    @Test
    fun testJsonLiteralInt() = parametrizedTest { useStreaming ->
        val wrapper = JsonLiteralWrapper(JsonLiteral(1))
        val string = strict.stringify(JsonLiteralWrapper.serializer(), wrapper, useStreaming)
        assertEquals("{\"literal\":1}", string)
        assertEquals(JsonLiteralWrapper(JsonLiteral(1)), strict.parse(JsonLiteralWrapper.serializer(), string, useStreaming))
    }

    @Test
    fun testJsonPrimitiveInt() = parametrizedTest { useStreaming ->
        val wrapper = JsonPrimitiveWrapper(JsonLiteral(1))
        val string = strict.stringify(JsonPrimitiveWrapper.serializer(), wrapper, useStreaming)
        assertEquals("{\"primitive\":1}", string)
        assertEquals(JsonPrimitiveWrapper(JsonLiteral(1)), strict.parse(JsonPrimitiveWrapper.serializer(), string, useStreaming))
    }

    @Test
    fun testJsonLiteralString() = parametrizedTest { useStreaming ->
        val wrapper = JsonLiteralWrapper(JsonLiteral("foo"))
        val string = strict.stringify(JsonLiteralWrapper.serializer(), wrapper, useStreaming)
        assertEquals("{\"literal\":\"foo\"}", string)
        assertEquals(JsonLiteralWrapper(JsonLiteral("foo")), strict.parse(JsonLiteralWrapper.serializer(), string, useStreaming))
    }

    @Test
    fun testJsonPrimitiveString() = parametrizedTest { useStreaming ->
        val wrapper = JsonPrimitiveWrapper(JsonLiteral("foo"))
        val string = strict.stringify(JsonPrimitiveWrapper.serializer(), wrapper, useStreaming)
        assertEquals("{\"primitive\":\"foo\"}", string)
        assertEquals(JsonPrimitiveWrapper(JsonLiteral("foo")), strict.parse(JsonPrimitiveWrapper.serializer(), string, useStreaming))
    }

    @Test
    fun testJsonLiteralStringNumber() = parametrizedTest { useStreaming ->
        val wrapper = JsonLiteralWrapper(JsonLiteral("239"))
        val string = strict.stringify(JsonLiteralWrapper.serializer(), wrapper, useStreaming)
        assertEquals("{\"literal\":\"239\"}", string)
        assertEquals(JsonLiteralWrapper(JsonLiteral("239")), strict.parse(JsonLiteralWrapper.serializer(), string, useStreaming))
    }

    @Test
    fun testJsonPrimitiveStringNumber() = parametrizedTest { useStreaming ->
        val wrapper = JsonPrimitiveWrapper(JsonLiteral("239"))
        val string = strict.stringify(JsonPrimitiveWrapper.serializer(), wrapper, useStreaming)
        assertEquals("{\"primitive\":\"239\"}", string)
        assertEquals(JsonPrimitiveWrapper(JsonLiteral("239")), strict.parse(JsonPrimitiveWrapper.serializer(), string, useStreaming))
    }

    @Test
    fun testTopLevelPrimitive() = parametrizedTest { useStreaming ->
        val string = strict.stringify(JsonPrimitiveSerializer, JsonLiteral(42), useStreaming)
        assertEquals("42", string)
        assertEquals(JsonLiteral(42), strict.parse(JsonPrimitiveSerializer, string))
    }

    @Test
    fun testTopLevelPrimitiveAsLiteral() = parametrizedTest { useStreaming ->
        val string = strict.stringify(JsonLiteralSerializer, JsonLiteral("some string literal"), useStreaming)
        assertEquals("\"some string literal\"", string)
        assertEquals(JsonLiteral("some string literal"), strict.parse(JsonLiteralSerializer, string, useStreaming))
    }

    @Test
    fun testTopLevelPrimitiveAsElement() = parametrizedTest { useStreaming ->
        if (isJs()) return@parametrizedTest // JS toString numbers
        val string = strict.stringify(JsonElementSerializer, JsonLiteral(1.3), useStreaming)
        assertEquals("1.3", string)
        assertEquals(JsonLiteral(1.3), strict.parse(JsonElementSerializer, string, useStreaming))
    }

    @Test
    fun testJsonLiteralStringToString() {
        val literal = JsonLiteral("some string literal")
        val string = strict.stringify(JsonLiteralSerializer, literal)
        assertEquals(string, literal.toString())
    }

    @Test
    fun testJsonLiteralIntToString() {
        val literal = JsonLiteral(0)
        val string = strict.stringify(JsonLiteralSerializer, literal)
        assertEquals(string, literal.toString())
    }

    @Test
    fun testJsonLiterals()  {
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
        val literal = JsonLiteral(number)
        val string = strict.stringify(JsonLiteralSerializer, literal)
        assertEquals(string, literal.toString())
        if (isJvm()) { // We can rely on stable double/float format only on JVM
            assertEquals(string, jvmExpectedString)
        }
    }
}
