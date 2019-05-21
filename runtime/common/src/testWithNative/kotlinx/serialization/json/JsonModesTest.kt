/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.*

class JsonModesTest : JsonTestBase() {

    @Test
    fun testNan() = parametrizedTest(nonStrict) {
        assertStringFormAndRestored("{\"double\":NaN,\"float\":NaN}", Box(Double.NaN, Float.NaN), Box.serializer())
    }

    @Test
    fun testInfinity() = parametrizedTest(nonStrict) {
        assertStringFormAndRestored(
            "{\"double\":Infinity,\"float\":-Infinity}",
            Box(Double.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY),
            Box.serializer()
        )
    }

    @Test
    fun nonStrictJsonCanSkipValues() = parametrizedTest { useStreaming ->
        val data = JsonOptionalTests.Data()
        assertEquals(
            nonStrict.parse(JsonOptionalTests.Data.serializer(), "{strangeField: 100500, a:0}", useStreaming),
            data
        )
        assertEquals(
            nonStrict.parse(JsonOptionalTests.Data.serializer(), "{a:0, strangeField: 100500}", useStreaming),
            data
        )
    }

    @Test
    fun nonStrictJsonCanSkipComplexValues() = parametrizedTest { useStreaming ->
        val data = JsonOptionalTests.Data()

        assertEquals(
            nonStrict.parse(
                JsonOptionalTests.Data.serializer(),
                "{a: 0, strangeField: {a: b, c: {d: e}, f: [g,h,j] }}",
                useStreaming
            ),
            data)
        assertEquals(
            nonStrict.parse(
                JsonOptionalTests.Data.serializer(),
                "{strangeField: {a: b, c: {d: e}, f: [g,h,j] }, a: 0}",
                useStreaming
            ),
            data)
    }

    @Test
    fun testSerializeQuotedJson() = parametrizedTest { useStreaming ->
        assertEquals(
            """{"a":10,"e":false,"c":"Hello"}""", strict.stringify(
                JsonTransientTest.Data.serializer(),
                JsonTransientTest.Data(10, 100), useStreaming))
    }

    @Test
    fun testParseEscapedSymbols() = parametrizedTest { useStreaming ->
        assertEquals(
            Url("https://t.co/M1uhwigsMT"),
            strict.parse(Url.serializer(), """{"url":"https:\/\/t.co\/M1uhwigsMT"}""", useStreaming)
        )
        assertEquals(Url("\"test\""), strict.parse(Url.serializer(), """{"url": "\"test\""}""", useStreaming))
        assertEquals(Url("\u00c9"), strict.parse(Url.serializer(), """{"url": "\u00c9"}""", useStreaming))
        assertEquals(Url("""\\"""), strict.parse(Url.serializer(), """{"url": "\\\\"}""", useStreaming))
    }

    @Test
    fun testStrictJsonCanNotSkipValues() = parametrizedTest { useStreaming ->
        assertFailsWith(SerializationException::class) {
            strict.parse(JsonOptionalTests.Data.serializer(), "{strangeField: 100500, a:0}", useStreaming)
        }
    }

    @Serializable
    data class Box(val double: Double, val float: Float)

    @Serializable
    data class Url(val url: String)
}
