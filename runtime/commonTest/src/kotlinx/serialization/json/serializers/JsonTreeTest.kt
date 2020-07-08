/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlin.test.*

class JsonTreeTest : JsonTestBase() {

    private fun parse(input: String): JsonElement = default.decodeFromString(JsonElementSerializer, input)

    @Test
    fun testParseWithoutExceptions() {
        val input = """{"a": "foo",              "b": 10, "c": ["foo", 100500, {"bar": "baz"}]}"""
        parse(input)
    }

    @Test
    fun testJsonLiteral() {
        val v = JsonPrimitive("foo")
        assertEquals(v, parse("\"foo\""))
    }

    @Test
    fun testJsonObject() {
        val input = """{"a": "foo", "b": 10, "c": true, "d": null}"""
        val elem = parse(input)

        assertTrue(elem is JsonObject)
        assertEquals(setOf("a", "b", "c", "d"), elem.keys)

        assertEquals(JsonPrimitive("foo"), elem["a"])
        assertEquals(10, elem["b"]?.jsonPrimitive?.int)
        assertEquals(true, elem["c"]?.jsonPrimitive?.boolean)
        assertSame(elem.getValue("d") as JsonNull, JsonNull)
    }

    @Test
    fun testJsonObjectWithArrays() {
        val input = """{"a": "foo",              "b": 10, "c": ["foo", 100500, {"bar": "baz"}]}"""
        val elem = parse(input)

        assertTrue(elem is JsonObject)
        assertEquals(setOf("a", "b", "c"), elem.keys)
        assertTrue(elem.getValue("c") is JsonArray)

        val array = elem.getValue("c").jsonArray
        assertEquals("foo", array.getOrNull(0)?.jsonPrimitive?.content)
        assertEquals(100500, array.getOrNull(1)?.jsonPrimitive?.int)

        assertTrue(array[2] is JsonObject)
        val third = array[2].jsonObject
        assertEquals("baz", third.getValue("bar").jsonPrimitive.content)
    }

    @Test
    fun testSaveToJson() {
        val input = """{"a":"foo","b":10,"c":true,"d":null,"e":["foo",100500,{"bar":"baz"}]}"""
        val elem = parse(input)
        val json = elem.toString()
        assertEquals(input, json)
    }

    @Test
    fun testEqualityTest() {
        val input = """{"a": "foo", "b": 10}"""
        val parsed = parse(input)
        val parsed2 = parse(input)
        val handCrafted = buildJsonObject { put("a", JsonPrimitive("foo")); put("b", JsonPrimitive(10)) }
        assertEquals(parsed, parsed2)
        assertEquals(parsed, handCrafted)
    }

    @Test
    fun testInEqualityTest() {
        val input = """{"a": "10", "b": 10}"""
        val parsed = parse(input) as JsonObject
        val handCrafted = buildJsonObject { put("a", JsonPrimitive("10")); put("b", JsonPrimitive(10)) }
        assertEquals(parsed, handCrafted)

        assertNotEquals(parsed["a"], parsed["b"])
        assertNotEquals(parsed["a"], handCrafted["b"])
        assertNotEquals(handCrafted["a"], parsed["b"])
        assertNotEquals(handCrafted["a"], handCrafted["b"])
    }

    @Test
    fun testExceptionalState() {
        val tree =
            JsonObject(mapOf("a" to JsonPrimitive(42), "b" to JsonArray(listOf(JsonNull)), "c" to JsonPrimitive(false)))
        assertFailsWith<NoSuchElementException> { tree.getValue("no key").jsonObject }
        assertFailsWith<IllegalArgumentException> { tree.getValue("a").jsonArray }
        assertEquals(null, tree["no key"]?.jsonObject)
        assertEquals(null, tree["a"] as? JsonArray)

        val n = tree.getValue("b").jsonArray[0].jsonPrimitive
        assertFailsWith<NumberFormatException> { n.int }
        assertEquals(null, n.intOrNull)

        assertFailsWith<IllegalStateException> { n.boolean }
        assertEquals(null, n.booleanOrNull)
    }

    @Test
    fun testThatJsonArraysCompareWithLists() {
        val jsonArray: List<JsonElement> = JsonArray(listOf(JsonPrimitive(3), JsonPrimitive(4)))
        val arrayList: List<JsonElement> = ArrayList(listOf(JsonPrimitive(3), JsonPrimitive(4)))
        val otherArrayList: List<JsonElement> = ArrayList(listOf(JsonPrimitive(3), JsonPrimitive(5)))

        assertEquals(jsonArray, arrayList)
        assertEquals(arrayList, jsonArray)
        assertEquals(jsonArray.hashCode(), arrayList.hashCode())
        assertNotEquals(jsonArray, otherArrayList)
    }

    @Test
    fun testThatJsonObjectsCompareWithMaps() {
        val jsonObject: Map<String, JsonElement> = JsonObject(
            mapOf(
                "three" to JsonPrimitive(3),
                "four" to JsonPrimitive(4)
            )
        )
        val hashMap: Map<String, JsonElement> = HashMap(
            mapOf(
                "three" to JsonPrimitive(3),
                "four" to JsonPrimitive(4)
            )
        )
        val otherJsonObject: Map<String, JsonElement> = JsonObject(
            mapOf(
                "three" to JsonPrimitive(3),
                "five" to JsonPrimitive(5)
            )
        )
        val otherHashMap: Map<String, JsonElement> = HashMap(
            mapOf(
                "three" to JsonPrimitive(3),
                "four" to JsonPrimitive(5)
            )
        )

        assertEquals(jsonObject, hashMap)
        assertEquals(hashMap, jsonObject)
        assertEquals(jsonObject.hashCode(), hashMap.hashCode())
        assertNotEquals(jsonObject, otherHashMap)
        assertNotEquals(jsonObject, otherJsonObject)
    }
}
