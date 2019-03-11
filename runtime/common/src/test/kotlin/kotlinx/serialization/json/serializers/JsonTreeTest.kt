/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlin.test.*

class JsonTreeTest {

    private fun parse(input: String): JsonElement = Json().parse(JsonElementSerializer, input)

    @Test
    fun testParseWithoutExceptions() { 
        val input = """{"a": "foo",              "b": 10, "c": ["foo", 100500, {"bar": "baz"}]}"""
        parse(input)
    }

    @Test
    fun testJsonLiteral() {
        val v = JsonLiteral("foo")
        assertEquals(v, parse("\"foo\""))
    }

    @Test
    fun testJsonObject() {
        val input = """{"a": "foo", "b": 10, "c": true, "d": null}"""
        val elem = parse(input)

        assertTrue(elem is JsonObject)
        assertEquals(setOf("a", "b", "c", "d"), elem.keys)

        assertEquals(JsonLiteral("foo"), elem["a"])
        assertEquals(10, elem.getPrimitiveOrNull("b")?.int)
        assertEquals(true, elem.getPrimitiveOrNull("c")?.boolean)
        assertSame(elem.getAs("d"), JsonNull)
    }

    @Test
    fun testJsonObjectWithArrays() {
        val input = """{"a": "foo",              "b": 10, "c": ["foo", 100500, {"bar": "baz"}]}"""
        val elem = parse(input)

        assertTrue(elem is JsonObject)
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
        val handCrafted = json { "a" to JsonPrimitive("foo"); "b" to JsonPrimitive(10) }
        assertEquals(parsed, parsed2)
        assertEquals(parsed, handCrafted)
    }

    @Test
    fun testInEqualityTest() {
        val input = """{"a": "10", "b": 10}"""
        val parsed = parse(input) as JsonObject
        val handCrafted = json { "a" to JsonPrimitive("10"); "b" to JsonPrimitive(10) }
        assertEquals(parsed, handCrafted)

        assertNotEquals(parsed["a"], parsed["b"])
        assertNotEquals(parsed["a"], handCrafted["b"])
        assertNotEquals(handCrafted["a"], parsed["b"])
        assertNotEquals(handCrafted["a"], handCrafted["b"])
    }

    @Test
    fun testExceptionalState() {
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

    @Test
    fun testThatJsonArraysCompareWithLists() {
        val jsonArray: List<JsonElement> = JsonArray(listOf(JsonLiteral(3), JsonLiteral(4)))
        val arrayList: List<JsonElement> = ArrayList(listOf(JsonLiteral(3), JsonLiteral(4)))
        val otherArrayList: List<JsonElement> = ArrayList(listOf(JsonLiteral(3), JsonLiteral(5)))

        assertEquals(jsonArray, arrayList)
        assertEquals(arrayList, jsonArray)
        assertEquals(jsonArray.hashCode(), arrayList.hashCode())
        assertNotEquals(jsonArray, otherArrayList)
    }

    @Test
    fun testThatJsonObjectsCompareWithMaps() {
        val jsonObject: Map<String, JsonElement> = JsonObject(mapOf(
                "three" to JsonLiteral(3),
                "four" to JsonLiteral(4)
        ))
        val hashMap: Map<String, JsonElement> = HashMap(mapOf(
                "three" to JsonLiteral(3),
                "four" to JsonLiteral(4)
        ))
        val otherJsonObject: Map<String, JsonElement> = JsonObject(mapOf(
            "three" to JsonLiteral(3),
            "five" to JsonLiteral(5)
        ))
        val otherHashMap: Map<String, JsonElement> = HashMap(mapOf(
                "three" to JsonLiteral(3),
                "four" to JsonLiteral(5)
        ))

        assertEquals(jsonObject, hashMap)
        assertEquals(hashMap, jsonObject)
        assertEquals(jsonObject.hashCode(), hashMap.hashCode())
        assertNotEquals(jsonObject, otherHashMap)
        assertNotEquals(jsonObject, otherJsonObject)
    }

    @Test
    fun testThatLiteralsAreDetected() {
        val input = """{
                "number1": 10,
                "number2": -3,
                "number3": 0.7,
                "number4": 9999999999,
                "bool1": true,
                "bool2": false,
                "string": "abc",
                "null": null
            }""".trimMargin()

        with(parse(input) as JsonObject) {
            get("number1").let {
                assertTrue(it is JsonLiteral.JsonNumberLiteral)
                assertEquals(it.body, 10)
            }
            get("number2").let {
                assertTrue(it is JsonLiteral.JsonNumberLiteral)
                assertEquals(it.body, -3)
            }
            get("number3").let {
                assertTrue(it is JsonLiteral.JsonNumberLiteral)
                assertEquals(it.body, 0.7)
            }
            get("number4").let {
                assertTrue(it is JsonLiteral.JsonNumberLiteral)
                assertEquals(it.body, 9999999999L)
            }
            get("bool1").let {
                assertTrue(it is JsonLiteral.JsonBooleanLiteral)
                assertEquals(it.body, true)
            }
            get("bool2").let {
                assertTrue(it is JsonLiteral.JsonBooleanLiteral)
                assertEquals(it.body, false)
            }
            get("string").let {
                assertTrue(it is JsonLiteral.JsonStringLiteral)
                assertEquals(it.body, "abc")
            }
            assertTrue(get("null") is JsonNull)
        }
    }

    @Test @Ignore // Not yet supported, cf [JsonReader.takeNumber]
    fun testNumberLiteralsWithExponent() {
        val input = "[1e3, 1000e-3, 1.75E2, 1e0]"

        assertEquals(listOf<Number>(1000, 1.0, 175.0, 1).map { JsonLiteral.JsonNumberLiteral(it) } as List<JsonElement>,
                     parse(input) as JsonArray)
    }

    @Test
    fun testThatJsonLiteralsAreDistinguishable() {
        val input = """{
                "numberString": "10", "number": 10,
                "boolString": "true", "boolInt": 1, "bool": true,
                "nullString": "null", "null": null
            }""".trimMargin()

        with(parse(input) as JsonObject) {
            fun getLiteral(key: String) : JsonLiteral<*> =
                get(key) as JsonLiteral<*>

            assertNotEquals(get("numberString"), get("number"))
            assertNotEquals(get("boolString"), get("bool"))
            assertNotEquals(get("boolInt"), get("bool"))
            assertNotEquals(get("boolInt"), get("boolString"))
            assertNotEquals(get("nullString"), get("null"))

            assertNotEquals(getLiteral("numberString").body, getLiteral("number").body)
            assertNotEquals(getLiteral("boolString").body, getLiteral("bool").body)
            assertNotEquals(getLiteral("boolInt").body, getLiteral("bool").body)
            assertNotEquals(getLiteral("boolInt").body, getLiteral("boolString").body)
        }
    }
}
