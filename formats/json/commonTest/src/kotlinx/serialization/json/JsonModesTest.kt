/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonModesTest : JsonTestBase() {

    @Test
    fun testNan() = parametrizedTest(lenient) {
        assertStringFormAndRestored("{\"double\":NaN,\"float\":NaN}", Box(Double.NaN, Float.NaN), Box.serializer())
    }

    @Test
    fun testInfinity() = parametrizedTest(lenient) {
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
            lenient.decodeFromString(JsonOptionalTests.Data.serializer(), "{strangeField: 100500, a:0}", useStreaming),
            data
        )
        assertEquals(
            lenient.decodeFromString(JsonOptionalTests.Data.serializer(), "{a:0, strangeField: 100500}", useStreaming),
            data
        )
    }

    @Test
    fun nonStrictJsonCanSkipComplexValues() = parametrizedTest { useStreaming ->
        val data = JsonOptionalTests.Data()

        assertEquals(
            lenient.decodeFromString(
                JsonOptionalTests.Data.serializer(),
                "{a: 0, strangeField: {a: b, c: {d: e}, f: [g,h,j] }}",
                useStreaming
            ),
            data
        )
        assertEquals(
            lenient.decodeFromString(
                JsonOptionalTests.Data.serializer(),
                "{strangeField: {a: b, c: {d: e}, f: [g,h,j] }, a: 0}",
                useStreaming
            ),
            data
        )
    }

    @Test
    fun ignoreKeysCanIgnoreWeirdStringValues() {
        val data = JsonOptionalTests.Data()
        fun doTest(input: String) {
            assertEquals(data, lenient.decodeFromString(input))
        }
        doTest("{a: 0, strangeField: [\"imma string with } bracket\", \"sss\"]}")
        doTest("{a: 0, strangeField: [\"imma string with ] bracket\", \"sss\"]}")
        doTest("{a: 0, strangeField: \"imma string with } bracket\"}")
        doTest("{a: 0, strangeField: \"imma string with ] bracket\"}")
        doTest("{a: 0, strangeField: {key: \"imma string with ] bracket\"}}")
        doTest("{a: 0, strangeField: {key: \"imma string with } bracket\"}}")
        doTest("""{"a": 0, "strangeField": {"key": "imma string with } bracket"}}""")
        doTest("""{"a": 0, "strangeField": {"key": "imma string with ] bracket"}}""")
        doTest("""{"a": 0, "strangeField": ["imma string with ] bracket"]}""")
        doTest("""{"a": 0, "strangeField": ["imma string with } bracket"]}""")
    }

    @Serializable
    class Empty

    @Test
    fun lenientThrowOnMalformedString() {
        fun doTest(input: String) {
            assertFailsWith<SerializationException> { lenient.decodeFromString(Empty.serializer(), input) }
        }
        doTest("""{"a":[{"b":[{"c":{}d",""e"":"}]}""")
        doTest("""{"a":[}""")
        doTest("""{"a":""")
        lenient.decodeFromString(Empty.serializer(), """{"a":[]}""") // should not throw
    }

    @Test
    fun testSerializeQuotedJson() = parametrizedTest { useStreaming ->
        assertEquals(
            """{"a":10,"e":false,"c":"Hello"}""", default.encodeToString(
                JsonTransientTest.Data.serializer(),
                JsonTransientTest.Data(10, 100), useStreaming
            )
        )
    }

    @Test
    fun testStrictJsonCanNotSkipValues() = parametrizedTest { useStreaming ->
        assertFailsWith(SerializationException::class) {
            default.decodeFromString(JsonOptionalTests.Data.serializer(), "{strangeField: 100500, a:0}", useStreaming)
        }
    }

    @Serializable
    data class Box(val double: Double, val float: Float)
}
