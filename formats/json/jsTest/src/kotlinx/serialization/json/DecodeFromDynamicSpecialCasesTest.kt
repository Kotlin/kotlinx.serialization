/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class DecodeFromDynamicSpecialCasesTest {

    @Test
    fun testTopLevelInt() {
        val dyn = js("42")
        val parsed = Json.decodeFromDynamic<Int>(dyn)
        assertEquals(42, parsed)
    }

    @Test
    fun testTopLevelString() {
        val dyn = js(""""42"""")
        val parsed = Json.decodeFromDynamic<String>(dyn)
        assertEquals("42", parsed)
    }

    @Test
    fun testTopLevelList() {
        val dyn = js("[1, 2, 3]")
        val parsed = Json.decodeFromDynamic<List<Int>>(dyn)
        assertEquals(listOf(1, 2, 3), parsed)
    }

    @Test
    fun testStringMap() = testMapWithPrimitiveKey("1", "2")

    @Test
    fun testByteMap() = testMapWithPrimitiveKey(1.toByte(), 2.toByte())

    @Test
    fun testCharMap() = testMapWithPrimitiveKey('1', '2')

    @Test
    fun testShortMap() = testMapWithPrimitiveKey(1.toShort(), 2.toShort())

    @Test
    fun testIntMap() = testMapWithPrimitiveKey(1, 2)

    @Test
    fun testLongMap()  = testMapWithPrimitiveKey(1L, 2L)

    @Test
    fun testDoubleMap()  = testMapWithPrimitiveKey(1.0, 2.0)

    @Test
    fun testFloatMap() = testMapWithPrimitiveKey(1.0f, 2.0f)

    private inline fun <reified T> testMapWithPrimitiveKey(k1: T, k2: T) {
        val map = mapOf(k1 to 3, k2 to 4)
        val dyn = js("{1:3, 2:4}")
        val parsed = Json.decodeFromDynamic<Map<T, Int>>(dyn)
        assertEquals(map, parsed)
    }

    @Test
    fun testJsonPrimitive() {
        testJsonElement<JsonPrimitive>(js("42"), JsonPrimitive(42))
        testJsonElement<JsonElement>(js("42"), JsonPrimitive(42))
    }

    @Test
    fun testJsonPrimitiveDouble() {
        testJsonElement<JsonElement>(js("42.0"), JsonPrimitive(42.0))
        testJsonElement<JsonPrimitive>(js("42.0"), JsonPrimitive(42.0))
    }

    @Test
    fun testJsonStringPrimitive() {
        testJsonElement<JsonElement>(js(""""42""""), JsonPrimitive("42"))
        testJsonElement<JsonPrimitive>(js(""""42""""), JsonPrimitive("42"))
    }

    @Test
    fun testJsonArray() {
        testJsonElement<JsonElement>(js("[1,2,3]"), JsonArray((1..3).map(::JsonPrimitive)))
        testJsonElement<JsonArray>(js("[1,2,3]"), JsonArray((1..3).map(::JsonPrimitive)))
    }

    @Test
    fun testJsonObject() {
        testJsonElement<JsonElement>(
            js("""{1:2,"3":4}"""),
            JsonObject(mapOf("1" to JsonPrimitive(2), "3" to JsonPrimitive(4)))
        )
        testJsonElement<JsonObject>(
            js("""{1:2,"3":4}"""),
            JsonObject(mapOf("1" to JsonPrimitive(2), "3" to JsonPrimitive(4)))
        )
    }

    private inline fun <reified T : JsonElement> testJsonElement(js: dynamic, expected: JsonElement) {
        val parsed = Json.decodeFromDynamic<T>(js)
        assertEquals(expected, parsed)
    }

    @Serializable
    data class Wrapper(val e: JsonElement, val p: JsonPrimitive, val o: JsonObject, val a: JsonArray)

    @Test
    fun testJsonElementWrapper() {
        val js = js("""{"e":42,"p":"239", "o":{"k":"v"}, "a":[1, 2, 3]}""")
        val parsed = Json.decodeFromDynamic<Wrapper>(js)
        val expected = Wrapper(JsonPrimitive(42), JsonPrimitive("239"), buildJsonObject { put("k", "v") }, JsonArray((1..3).map(::JsonPrimitive)))
        assertEquals(expected, parsed)
    }
}
