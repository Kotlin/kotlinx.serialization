/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonPrimitiveSerializerTest : JsonTestBase() {

    @Test
    fun testJsonPrimitiveDouble() = parametrizedTest { useStreaming ->
        if (isJs()) return@parametrizedTest // JS toString numbers


        val wrapper = JsonPrimitiveWrapper(JsonPrimitive(1.0))
        val string = default.encodeToString(JsonPrimitiveWrapper.serializer(), wrapper, useStreaming)
        assertEquals("{\"primitive\":1.0}", string)
        assertEquals(JsonPrimitiveWrapper(JsonPrimitive(1.0)), default.decodeFromString(JsonPrimitiveWrapper.serializer(), string, useStreaming))
    }

    @Test
    fun testJsonPrimitiveInt() = parametrizedTest { useStreaming ->
        val wrapper = JsonPrimitiveWrapper(JsonPrimitive(1))
        val string = default.encodeToString(JsonPrimitiveWrapper.serializer(), wrapper, useStreaming)
        assertEquals("{\"primitive\":1}", string)
        assertEquals(JsonPrimitiveWrapper(JsonPrimitive(1)), default.decodeFromString(JsonPrimitiveWrapper.serializer(), string, useStreaming))
    }


    @Test
    fun testJsonPrimitiveString() = parametrizedTest { useStreaming ->
        val wrapper = JsonPrimitiveWrapper(JsonPrimitive("foo"))
        val string = default.encodeToString(JsonPrimitiveWrapper.serializer(), wrapper, useStreaming)
        assertEquals("{\"primitive\":\"foo\"}", string)
        assertEquals(JsonPrimitiveWrapper(JsonPrimitive("foo")), default.decodeFromString(JsonPrimitiveWrapper.serializer(), string, useStreaming))
    }

    @Test
    fun testJsonPrimitiveStringNumber() = parametrizedTest { useStreaming ->
        val wrapper = JsonPrimitiveWrapper(JsonPrimitive("239"))
        val string = default.encodeToString(JsonPrimitiveWrapper.serializer(), wrapper, useStreaming)
        assertEquals("{\"primitive\":\"239\"}", string)
        assertEquals(JsonPrimitiveWrapper(JsonPrimitive("239")), default.decodeFromString(JsonPrimitiveWrapper.serializer(), string, useStreaming))
    }

    @Test
    fun testTopLevelPrimitive() = parametrizedTest { useStreaming ->
        val string = default.encodeToString(JsonPrimitiveSerializer, JsonPrimitive(42), useStreaming)
        assertEquals("42", string)
        assertEquals(JsonPrimitive(42), default.decodeFromString(JsonPrimitiveSerializer, string))
    }

    @Test
    fun testTopLevelPrimitiveAsElement() = parametrizedTest { useStreaming ->
        if (isJs()) return@parametrizedTest // JS toString numbers
        val string = default.encodeToString(JsonElementSerializer, JsonPrimitive(1.3), useStreaming)
        assertEquals("1.3", string)
        assertEquals(JsonPrimitive(1.3), default.decodeFromString(JsonElementSerializer, string, useStreaming))
    }

    @Test
    fun testJsonLiteralStringToString() {
        val literal = JsonPrimitive("some string literal")
        val string = default.encodeToString(JsonPrimitiveSerializer, literal)
        assertEquals(string, literal.toString())
    }

    @Test
    fun testJsonLiteralIntToString() {
        val literal = JsonPrimitive(0)
        val string = default.encodeToString(JsonPrimitiveSerializer, literal)
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
        val literal = JsonPrimitive(number)
        val string = default.encodeToString(JsonPrimitiveSerializer, literal)
        assertEquals(string, literal.toString())
        if (isJvm()) { // We can rely on stable double/float format only on JVM
            assertEquals(string, jvmExpectedString)
        }
    }
}
