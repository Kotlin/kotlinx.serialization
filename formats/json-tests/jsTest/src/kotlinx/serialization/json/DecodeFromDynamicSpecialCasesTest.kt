/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
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
    fun testJsonNull() {
        testJsonElement<JsonElement>(js("null"), JsonNull)
        testJsonElement<JsonElement>(js("undefined"), JsonNull)
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
    data class Wrapper(val e: JsonElement, val p: JsonPrimitive, val o: JsonObject, val a: JsonArray, val n: JsonNull)

    @Serializable
    data class UIntHolder(val value: UInt, val values: List<UInt>, val mapped: Map<String, UInt>)

    @Test
    fun testJsonElementWrapper() {
        val js = js("""{"e":42,"p":"239", "o":{"k":"v"}, "a":[1, 2, 3], "n": null}""")
        val parsed = Json.decodeFromDynamic<Wrapper>(js)
        val expected = Wrapper(JsonPrimitive(42), JsonPrimitive("239"), buildJsonObject { put("k", "v") }, JsonArray((1..3).map(::JsonPrimitive)), JsonNull)
        assertEquals(expected, parsed)
    }

    @Test
    fun testUIntBoundaryWithExplicitSerializer() {
        assertEquals(2147483647u, Json.decodeFromDynamic(UInt.serializer(), js("2147483647")))
        assertEquals(2147483648u, Json.decodeFromDynamic(UInt.serializer(), js("2147483648")))
    }

    @Test
    fun testUIntReifiedMatchesStringifiedJson() {
        val samples = listOf(
            0u to js("0"),
            1u to js("1"),
            2147483657u to js("2147483657"),
            UInt.MAX_VALUE to js("4294967295")
        )
        for ((expected, dyn) in samples) {
            val parsed = Json.decodeFromDynamic<UInt>(dyn)
            assertEquals(expected, parsed)
            assertEquals(Json.decodeFromString<UInt>(JSON.stringify(dyn)), parsed)
        }
    }

    @Test
    fun testUpperHalfUIntInObjectListAndMap() {
        val dyn = js("{value:2147483648, values:[2147483648, 4294967295], mapped:{k:2147483648}}")
        val parsed = Json.decodeFromDynamic<UIntHolder>(dyn)
        val expected = UIntHolder(2147483648u, listOf(2147483648u, UInt.MAX_VALUE), mapOf("k" to 2147483648u))
        assertEquals(expected, parsed)
        assertEquals(Json.decodeFromString<UIntHolder>(JSON.stringify(dyn)), parsed)
    }

    @Test
    fun testSignedIntExtremes() {
        assertEquals(Int.MIN_VALUE, Json.decodeFromDynamic<Int>(js("-2147483648")))
        assertEquals(-1, Json.decodeFromDynamic<Int>(js("-1")))
        assertEquals(0, Json.decodeFromDynamic<Int>(js("0")))
        assertEquals(Int.MAX_VALUE, Json.decodeFromDynamic<Int>(js("2147483647")))
    }

    @Test
    fun testNonNumericIntDoesNotCoerceToZero() {
        assertFailsWith<ClassCastException> {
            Json.decodeFromDynamic<Int>(js("'not-a-number'"))
        }
    }
}
