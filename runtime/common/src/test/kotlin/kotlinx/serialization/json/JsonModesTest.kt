/*
 * Copyright 2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class JsonModesTest : JsonTestBase() {

    @Test
    fun testNan() = parametrizedTest { useStreaming ->
        val box = Box(Double.NaN, Float.NaN)
        val json = nonStrict.stringify(box, useStreaming = useStreaming)
        assertEquals("{\"double\":NaN,\"float\":NaN}", json)
        val deserialized = nonStrict.parse<Box>(json, useStreaming)
        assertEquals(box, deserialized)
    }

    @Test
    fun testInfinity() = parametrizedTest { useStreaming ->
        val box = Box(Double.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        val json  = nonStrict.stringify(box, useStreaming)
        assertEquals("{\"double\":Infinity,\"float\":-Infinity}", json)
        val deserialized = nonStrict.parse<Box>(json, useStreaming)
        assertEquals(box, deserialized)
    }

    @Test
    fun nonStrictJsonCanSkipValues() = parametrizedTest { useStreaming ->
        val data = JsonOptionalTests.Data()
        assertEquals(nonStrict.parse("{strangeField: 100500, a:0}", useStreaming), data)
        assertEquals(nonStrict.parse("{a:0, strangeField: 100500}", useStreaming), data)
    }

    @Test
    fun nonStrictJsonCanSkipComplexValues() = parametrizedTest { useStreaming ->
        val data = JsonOptionalTests.Data()

        assertEquals(
            nonStrict.parse("{a: 0, strangeField: {a: b, c: {d: e}, f: [g,h,j] }}", useStreaming),
            data)
        assertEquals(
            nonStrict.parse("{strangeField: {a: b, c: {d: e}, f: [g,h,j] }, a: 0}", useStreaming),
            data)
    }

    @Test
    fun testSerializeQuotedJson() = parametrizedTest { useStreaming ->
        assertEquals(
            """{"a":10,"e":false,"c":"Hello"}""", strict.stringify(
                JsonTransientTest.Data(10, 100), useStreaming))
    }

    @Test
    fun testParseEscapedSymbols() = parametrizedTest { useStreaming ->
        assertEquals(Url("https://t.co/M1uhwigsMT"), strict.parse("""{"url":"https:\/\/t.co\/M1uhwigsMT"}""", useStreaming))
        assertEquals(Url("\"test\""), strict.parse("""{"url": "\"test\""}""", useStreaming))
        assertEquals(Url("\u00c9"), strict.parse("""{"url": "\u00c9"}""", useStreaming))
        assertEquals(Url("""\\"""), strict.parse("""{"url": "\\\\"}""", useStreaming))
    }

    @Test
    fun testStrictJsonCanNotSkipValues() = parametrizedTest { useStreaming ->
        assertFailsWith(SerializationException::class) {
            strict.parse<JsonOptionalTests.Data>("{strangeField: 100500, a:0}", useStreaming)
        }
    }

    @Serializable
    data class Box(val double: Double, val float: Float)

    @Serializable
    data class Url(val url: String)
}
