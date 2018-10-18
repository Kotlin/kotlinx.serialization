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
        assertEquals(v, JsonTreeParser("\"foo\"").readFully())
    }

    @Test
    fun jsonObject() {
        val input = """{"a": "foo", "b": 10, "c": true, "d": null}"""
        val elem = JsonTreeParser(input).readFully()

        assertTrue(elem is JsonObject)
        elem as JsonObject
        assertEquals(setOf("a", "b", "c", "d"), elem.keys)

        assertEquals(JsonLiteral("foo"), elem["a"])
        assertEquals(10, elem.getPrimitiveOrNull("b")?.int)
        assertEquals(true, elem.getPrimitiveOrNull("c")?.boolean)
        assertTrue(elem.getAs<JsonNull>("d") === JsonNull)
    }

    @Test
    fun jsonObjectWithArrays() {
        val input = """{"a": "foo",              "b": 10, "c": ["foo", 100500, {"bar": "baz"}]}"""
        val elem = JsonTreeParser(input).readFully()

        assertTrue(elem is JsonObject)
        elem as JsonObject
        assertEquals(setOf("a", "b", "c"), elem.keys)
        assertTrue(elem.getValue("c") is JsonArray)

        val array = elem.getArray("c")
        assertEquals("foo", array.getPrimitiveOrNull(0)?.content)
        assertEquals(100500, array.getPrimitiveOrNull(1)?.int)

        assertTrue(array[2] is JsonObject)
        val third = array.getObject(2)
        assertEquals("baz", third.getPrimitive("bar").content)
    }

    @Test
    fun saveToJson() {
        val input = """{"a": "foo", "b": 10, "c": true, "d": null, "e": ["foo", 100500, {"bar": "baz"}]}"""
        val elem = JsonTreeParser(input).readFully()
        val json = elem.toString()
        assertEquals(input, json)
    }

    @Test
    fun exceptionCorrectness() {
        val tree =
            JsonObject(mapOf("a" to JsonLiteral(42), "b" to JsonArray(listOf(JsonNull)), "c" to JsonLiteral(false)))
        assertFailsWith<NoSuchElementException> { tree.getObject("no key") }
        assertFailsWith<JsonElementTypeMismatchException> { tree.getArray("a") }
        assertEquals(null, tree.getObjectOrNull("no key"))
        assertEquals(null, tree.getArrayOrNull("a"))

        val n = tree.getArray("b").getPrimitive(0)
        assertFailsWith<NumberFormatException> { n.int }
        assertEquals(null, n.intOrNull)

        assertFailsWith<IllegalStateException> { n.boolean }
        assertEquals(null, n.booleanOrNull)
    }
}
