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

class JsonTest {

    @Test
    fun testNan() {
        val box = Box(Double.NaN, Float.NaN)
        val json  = Json.nonstrict.stringify(box)
        assertEquals("{\"double\":NaN,\"float\":NaN}", json)
        val deserialized = Json.parse<Box>(json)
        assertEquals(box, deserialized)
    }

    @Test
    fun testInfinity() {
        val box = Box(Double.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        val json  = Json.nonstrict.stringify(box)
        assertEquals("{\"double\":Infinity,\"float\":-Infinity}", json)
        val deserialized = Json.parse<Box>(json)
        assertEquals(box, deserialized)
    }

    @Test
    fun nonStrictJsonCanSkipValues() {
        assertEquals(Json.nonstrict.parse("{strangeField: 100500, a:0}"),
            OptionalTests.Data()
        )
        assertEquals(Json.nonstrict.parse("{a:0, strangeField: 100500}"),
            OptionalTests.Data()
        )
    }

    @Test
    fun nonStrictJsonCanSkipComplexValues() {
        assertEquals(Json.nonstrict.parse("{a: 0, strangeField: {a: b, c: {d: e}, f: [g,h,j] }}"),
            OptionalTests.Data()
        )
        assertEquals(Json.nonstrict.parse("{strangeField: {a: b, c: {d: e}, f: [g,h,j] }, a: 0}"),
            OptionalTests.Data()
        )
    }

    @Test
    fun testSerializeQuotedJson() {
        assertEquals("""{"a":10,"e":false,"c":"Hello"}""", Json.stringify(
            TransientTests.Data(
                10,
                100
            )
        ))
    }

    @Test
    fun strictJsonCanNotSkipValues() {
        assertFailsWith(SerializationException::class) {
            Json.parse<OptionalTests.Data>("{strangeField: 100500, a:0}")
        }
    }

    @Serializable
    data class Box(val double: Double, val float: Float)

    @Serializable
    data class Url(val url: String)

    @Test
    fun testParseEscapedSymbols() {
        assertEquals(Url("https://t.co/M1uhwigsMT"), Json.parse("""{"url":"https:\/\/t.co\/M1uhwigsMT"}"""))
        assertEquals(Url("\"test\""), Json.parse("""{"url": "\"test\""}"""))
        assertEquals(Url("\u00c9"), Json.parse("""{"url": "\u00c9"}"""))
        assertEquals(Url("""\\"""), Json.parse("""{"url": "\\\\"}"""))
    }
}
