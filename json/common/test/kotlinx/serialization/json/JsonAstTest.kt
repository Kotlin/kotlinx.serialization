/*
 * Copyright 2018 JetBrains s.r.o.
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

import kotlin.test.*

class JsonAstTest {
    @Test
    fun parseWithoutExceptions() {
        val input = """{"a": "foo",              "b": 10, "c": ["foo", 100500, {"bar": "baz"}]}"""
        JsonTreeParser(input).readFully()
    }

    @Test
    fun jsonValue() {
        val v = JsonLiteral("foo")
        assertEquals(v, JsonTreeParser("foo").readFully())
    }

    @Test
    fun jsonObject() {
        val input = """{"a": "foo", "b": 10, "c": true, "d": null}"""
        val elem = JsonTreeParser(input).readFully()

        assertTrue(elem is JsonObject)
        elem as JsonObject
        assertEquals(setOf("a", "b", "c", "d"), elem.keys)

        assertEquals(JsonString("foo"), elem["a"])
        assertEquals(10, elem.getAsValue("b")?.asInt)
        assertEquals(true, elem.getAsValue("c")?.asBoolean)
        assertTrue(elem.getValue("d") === JsonNull)
    }

    @Test
    fun jsonObjectWithArrays() {
        val input = """{"a": "foo",              "b": 10, "c": ["foo", 100500, {"bar": "baz"}]}"""
        val elem = JsonTreeParser(input).readFully()

        assertTrue(elem is JsonObject)
        elem as JsonObject
        assertEquals(setOf("a", "b", "c"), elem.keys)
        assertTrue(elem.getValue("c") is JsonArray)

        val array = elem.getAsArray("c")!!
        assertEquals("foo", array.getAsValue(0)?.str)
        assertEquals(100500, array.getAsValue(1)?.asInt)

        assertTrue(array[2] is JsonObject)
        val third = array.getAsObject(2)!!
        assertEquals("baz", third.getAsValue("bar")?.str)
    }

    @Test
    fun saveToJson() {
        val input = """{"a": "foo", "b": 10, "c": true, "d": null, "e": ["foo", 100500, {"bar": "baz"}]}"""
        val elem = JsonTreeParser(input).readFully()
        val json = elem.toString()
        assertEquals(input, json)
    }
}
